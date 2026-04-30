package server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import data.DataLoader;
import data.PreProcessor;
import model.UserRecord;
import server.ApiServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LoadHandler implements HttpHandler {

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
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] body = baos.toByteArray();

            // xlsx aslinda zip gibi, o yuzden dosya buradaki PK imzasi ile basliyor
            byte[] pkSig = {0x50, 0x4B, 0x03, 0x04};
            int xlsxStart = -1;
            for (int i = 0; i < body.length - 4; i++) {
                if (body[i] == pkSig[0] && body[i + 1] == pkSig[1] &&
                    body[i + 2] == pkSig[2] && body[i + 3] == pkSig[3]) {
                    xlsxStart = i;
                    break;
                }
            }

            if (xlsxStart == -1) {
                sendError(exchange, 400, "No valid XLSX file found in upload");
                return;
            }

            int xlsxEnd = body.length;
            byte[] eocdSig = {0x50, 0x4B, 0x05, 0x06};
            for (int i = body.length - 4; i >= xlsxStart; i--) {
                if (body[i] == eocdSig[0] && body[i + 1] == eocdSig[1] &&
                    body[i + 2] == eocdSig[2] && body[i + 3] == eocdSig[3]) {
                    if (i + 22 <= body.length) {
                        int commentLen = (body[i + 20] & 0xFF) | ((body[i + 21] & 0xFF) << 8);
                        xlsxEnd = i + 22 + commentLen;
                    }
                    break;
                }
            }

            File tempFile = File.createTempFile("upload_", ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(body, xlsxStart, Math.min(xlsxEnd, body.length) - xlsxStart);
            }

            List<UserRecord> data = DataLoader.loadWithSkipCount(new FileInputStream(tempFile));
            int skipped = DataLoader.getLastSkipped();
            PreProcessor.encode(data);
            PreProcessor.normalize(data);

            ApiServer.currentData = data;

            Set<String> categories = new TreeSet<>();
            for (UserRecord r : data) {
                categories.add(r.getCategory());
            }

            StringBuilder cats = new StringBuilder("[");
            int c = 0;
            for (String cat : categories) {
                if (c > 0) cats.append(",");
                cats.append("\"").append(escapeJson(cat)).append("\"");
                c++;
            }
            cats.append("]");

            String resp = "{\"recordCount\": " + data.size() +
                          ", \"skipped\": " + skipped +
                          ", \"categories\": " + cats.toString() + "}";

            sendJson(exchange, 200, resp);

            tempFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Failed to load file: " + e.getMessage());
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
