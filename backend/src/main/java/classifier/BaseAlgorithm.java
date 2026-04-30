package classifier;

import model.UserRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class BaseAlgorithm implements IClassifier {
    protected List<UserRecord> trainingData;

    protected double calculateAccuracy(List<UserRecord> testData) {
        if (testData == null || testData.isEmpty()) return 0.0;
        int correct = 0;
        for (UserRecord record : testData) {
            String prediction = predict(record);
            if (prediction != null && prediction.equals(record.getCategory())) {
                correct++;
            }
        }
        return ((double) correct / testData.size()) * 100.0;
    }

    protected List<List<UserRecord>> splitData(List<UserRecord> data, double trainRatio) {
        List<UserRecord> copy = new ArrayList<>(data);
        Collections.shuffle(copy, new Random(42));
        int trainSize = (int) (copy.size() * trainRatio);
        List<UserRecord> trainList = new ArrayList<>(copy.subList(0, trainSize));
        List<UserRecord> testList = new ArrayList<>(copy.subList(trainSize, copy.size()));
        List<List<UserRecord>> split = new ArrayList<>();
        split.add(trainList);
        split.add(testList);
        return split;
    }

    protected Map<String, Map<String, Integer>> buildConfusionMatrix(List<UserRecord> testData) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        for (UserRecord record : testData) {
            String actual = record.getCategory();
            String predicted = predict(record);
            if (predicted == null) predicted = "Unknown";

            matrix.putIfAbsent(actual, new HashMap<>());
            Map<String, Integer> row = matrix.get(actual);
            row.put(predicted, row.getOrDefault(predicted, 0) + 1);
        }
        return matrix;
    }
}
