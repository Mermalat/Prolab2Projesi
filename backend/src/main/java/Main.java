import model.UserRecord;
import data.DataLoader;
import data.PreProcessor;
import classifier.IClassifier;
import classifier.KNNClassifier;
import classifier.DecisionTreeClassifier;
import evaluation.Evaluator;
import server.ApiServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  Kocaeli University - ML Tahmin Sistemi          ║");
            System.out.println("║  El Yazımı KNN & Karar Ağacı                     ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();

            // once backend ayaga kalksin, data sonra ui'dan da yuklenebilir
            ApiServer.start();

            File dataFile = new File("data/MarketSalesKocaeli.xlsx");
            if (dataFile.exists()) {
                System.out.println("\n[Main] Loading dataset from: " + dataFile.getAbsolutePath());
                List<UserRecord> data = DataLoader.load(dataFile.getAbsolutePath());

                if (!data.isEmpty()) {
                    PreProcessor.encode(data);
                    PreProcessor.normalize(data);
                    ApiServer.currentData = data;
                    System.out.println("[Main] Data preprocessed and ready. Records: " + data.size());

                    List<UserRecord> shuffled = new ArrayList<>(data);
                    Collections.shuffle(shuffled, new Random(42));
                    int trainSize = (int) (shuffled.size() * 0.8);
                    List<UserRecord> trainData = new ArrayList<>(shuffled.subList(0, trainSize));
                    List<UserRecord> testData = new ArrayList<>(shuffled.subList(trainSize, shuffled.size()));
                    System.out.println("[Main] Train size: " + trainData.size() + ", Test size: " + testData.size());

                    List<IClassifier> classifiers = new ArrayList<>();
                    classifiers.add(new KNNClassifier(5));
                    classifiers.add(new DecisionTreeClassifier(10));

                    System.out.println("\n--- Console Evaluation Results ---");

                    for (IClassifier clf : classifiers) {
                        long trainStart = System.currentTimeMillis();
                        clf.train(trainData);
                        long trainTime = System.currentTimeMillis() - trainStart;

                        Evaluator.EvaluationResult result = Evaluator.evaluate(clf, testData);
                        result.trainTimeMs = trainTime;

                        System.out.println("\n" + clf.getName());
                        System.out.println("  Accuracy:       " + String.format("%.2f%%", result.accuracy));
                        System.out.println("  Training time:  " + result.trainTimeMs + " ms");
                        System.out.println("  Prediction time:" + result.predictTimeMs + " ms");
                    }

                    System.out.println("\n--- Console evaluation complete ---");
                }
            } else {
                System.out.println("\n[Main] Dataset not found at data/MarketSalesKocaeli.xlsx");
                System.out.println("[Main] Upload via the frontend at http://localhost:8080");
            }

            System.out.println("\n[Main] Server is running. To find the server in web:.");
            System.out.println("[Main] Write file:///(YOUR_INDEX.HTML_PATH_HERE) on your browser");
            System.out.println("[Main] Press Ctrl+C to stop.");

        } catch (Exception e) {
            System.err.println("[Main] Critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
