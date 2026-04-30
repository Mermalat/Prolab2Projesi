package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import classifier.*;
import evaluation.Evaluator;
import model.UserRecord;
import server.ApiServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TrainHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) buf.append(line);
            String json = buf.toString();

            String algo = extractString(json, "algorithm");
            int k = extractInt(json, "k", 5);
            int maxDepth = extractInt(json, "maxDepth", 10);
            String criterion = extractString(json, "criterion");
            if (criterion == null || criterion.isEmpty()) criterion = "gini";
            double trainRatio = extractDouble(json, "trainRatio", 0.8);

            if (ApiServer.currentData == null || ApiServer.currentData.isEmpty()) {
                sendError(exchange, 400, "No data loaded. Upload a dataset first.");
                return;
            }

            List<UserRecord> copy = new ArrayList<>(ApiServer.currentData);
            Collections.shuffle(copy, new Random(42));
            int trainSize = (int) (copy.size() * trainRatio);
            List<UserRecord> trainList = new ArrayList<>(copy.subList(0, trainSize));
            List<UserRecord> testList = new ArrayList<>(copy.subList(trainSize, copy.size()));

            System.out.println("[Train] Algorithm=" + algo + ", k=" + k +
                    ", maxDepth=" + maxDepth + ", criterion=" + criterion +
                    ", ratio=" + trainRatio + ", trainSize=" + trainList.size() +
                    ", testSize=" + testList.size());

            List<IClassifier> classifiers = new ArrayList<>();
            if ("KNN".equals(algo) || "BOTH".equals(algo)) {
                classifiers.add(new KNNClassifier(k));
            }
            if ("DT".equals(algo) || "BOTH".equals(algo)) {
                DecisionTreeClassifier dt = new DecisionTreeClassifier(maxDepth);
                dt.setCriterion(criterion);
                classifiers.add(dt);
            }

            StringBuilder resJson = new StringBuilder("{\"results\": [");
            for (int i = 0; i < classifiers.size(); i++) {
                IClassifier clf = classifiers.get(i);

                long trainStart = System.currentTimeMillis();
                clf.train(trainList);
                long trainTime = System.currentTimeMillis() - trainStart;

                Evaluator.EvaluationResult res = Evaluator.evaluate(clf, testList);
                res.trainTimeMs = trainTime;

                System.out.println("[Train] " + res.classifierName +
                        " -> Accuracy: " + String.format("%.2f", res.accuracy) + "%" +
                        ", Train: " + res.trainTimeMs + "ms" +
                        ", Predict: " + res.predictTimeMs + "ms");

                resJson.append("{");
                resJson.append("\"algorithm\": \"").append(escapeJson(res.classifierName)).append("\",");
                resJson.append("\"accuracy\": ").append(res.accuracy).append(",");
                resJson.append("\"trainTimeMs\": ").append(res.trainTimeMs).append(",");
                resJson.append("\"predictTimeMs\": ").append(res.predictTimeMs).append(",");

                resJson.append("\"confusionMatrix\": {");
                int m1 = 0;
                List<String> actualKeys = new ArrayList<>(res.confusionMatrix.keySet());
                Collections.sort(actualKeys);
                for (String actualKey : actualKeys) {
                    Map<String, Integer> predMap = res.confusionMatrix.get(actualKey);
                    resJson.append("\"").append(escapeJson(actualKey)).append("\": {");
                    int m2 = 0;
                    List<String> predKeys = new ArrayList<>(predMap.keySet());
                    Collections.sort(predKeys);
                    for (String predKey : predKeys) {
                        resJson.append("\"").append(escapeJson(predKey)).append("\": ").append(predMap.get(predKey));
                        if (++m2 < predMap.size()) resJson.append(",");
                    }
                    resJson.append("}");
                    if (++m1 < res.confusionMatrix.size()) resJson.append(",");
                }
                resJson.append("},");

                resJson.append("\"perClassAccuracy\": {");
                int p1 = 0;
                List<String> classKeys = new ArrayList<>(res.perClassAccuracy.keySet());
                Collections.sort(classKeys);
                for (String classKey : classKeys) {
                    resJson.append("\"").append(escapeJson(classKey)).append("\": ").append(res.perClassAccuracy.get(classKey));
                    if (++p1 < res.perClassAccuracy.size()) resJson.append(",");
                }
                resJson.append("}");

                resJson.append("}");
                if (i < classifiers.size() - 1) resJson.append(",");
            }
            resJson.append("]}");

            sendJson(exchange, 200, resJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Training failed: " + e.getMessage());
        }
    }

    // frontendin gonderdigi basit json icin yeterli kucuk helperlar

    private String extractString(String json, String key) {
        String[] patterns = {"\"" + key + "\":\"", "\"" + key + "\": \""};
        for (String match : patterns) {
            int idx = json.indexOf(match);
            if (idx != -1) {
                int start = idx + match.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end);
            }
        }
        return null;
    }

    private int extractInt(String json, String key, int def) {
        String match = "\"" + key + "\":";
        int idx = json.indexOf(match);
        if (idx == -1) {
            match = "\"" + key + "\": ";
            idx = json.indexOf(match);
        }
        if (idx == -1) return def;
        int start = idx + match.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (start == end) return def;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private double extractDouble(String json, String key, double def) {
        String match = "\"" + key + "\":";
        int idx = json.indexOf(match);
        if (idx == -1) {
            match = "\"" + key + "\": ";
            idx = json.indexOf(match);
        }
        if (idx == -1) return def;
        int start = idx + match.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        if (start == end) return def;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String err = "{\"error\": \"" + escapeJson(message) + "\"}";
        sendJson(exchange, code, err);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
