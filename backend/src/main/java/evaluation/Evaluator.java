package evaluation;

import classifier.IClassifier;
import model.UserRecord;
import java.util.*;

public class Evaluator {

    public static class EvaluationResult {
        public String classifierName;
        public double accuracy;
        public long trainTimeMs;
        public long predictTimeMs;
        public Map<String, Map<String, Integer>> confusionMatrix;
        public Map<String, Double> perClassAccuracy;
    }

    public static EvaluationResult evaluate(IClassifier classifier, List<UserRecord> testData) {
        EvaluationResult res = new EvaluationResult();
        res.classifierName = classifier.getName();

        long startPredict = System.currentTimeMillis();

        int correct = 0;
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        Map<String, int[]> classStats = new HashMap<>();

        for (UserRecord record : testData) {
            String actual = record.getCategory();
            String predicted = classifier.predict(record);
            if (predicted == null) predicted = "Unknown";

            if (predicted.equals(actual)) correct++;

            matrix.putIfAbsent(actual, new HashMap<>());
            Map<String, Integer> row = matrix.get(actual);
            row.put(predicted, row.getOrDefault(predicted, 0) + 1);

            classStats.putIfAbsent(actual, new int[]{0, 0});
            classStats.get(actual)[1]++;
            if (actual.equals(predicted)) classStats.get(actual)[0]++;
        }

        res.predictTimeMs = System.currentTimeMillis() - startPredict;
        res.accuracy = testData.isEmpty() ? 0.0 : ((double) correct / testData.size()) * 100.0;
        res.confusionMatrix = matrix;

        res.perClassAccuracy = new HashMap<>();
        for (Map.Entry<String, int[]> entry : classStats.entrySet()) {
            int cor = entry.getValue()[0];
            int tot = entry.getValue()[1];
            double acc = tot == 0 ? 0.0 : ((double) cor / tot) * 100.0;
            res.perClassAccuracy.put(entry.getKey(), acc);
        }

        return res;
    }
}
