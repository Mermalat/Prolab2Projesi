package classifier;

import model.UserRecord;
import java.util.*;

public class DecisionTreeClassifier extends BaseAlgorithm {

    class TreeNode {
        String featureName;     // gender, amount, price veya brandCode
        double threshold;       // bolme noktasi, mesela gender icin 0.5
        String predictedLabel;  // sadece yaprak dugumde dolu oluyor
        TreeNode left, right;   // sol: deger <= threshold, sag: deger > threshold
    }

    private int maxDepth;
    private TreeNode root;
    private String criterion = "gini"; // gini veya infogain

    public DecisionTreeClassifier(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
    }

    @Override
    public void train(List<UserRecord> trainingData) {
        this.trainingData = trainingData;
        // agaci burada kuruyoruz, tahmin ederken sadece agacta gezecek
        this.root = buildTree(trainingData, 0);
    }

    private TreeNode buildTree(List<UserRecord> data, int depth) {
        TreeNode node = new TreeNode();

        // bu dala hic satir gelmediyse ogrenilecek bir sey yok
        if (data == null || data.isEmpty()) {
            node.predictedLabel = "Unknown";
            return node;
        }

        // cogunluk etiketini hazir tutuyorum, bolunemezse yaprak buna donecek
        String majority = majorityLabel(data);

        // agac yeterince derinse veya dal zaten tek kategoriyse duruyoruz
        if (depth >= maxDepth || allSameCategory(data)) {
            node.predictedLabel = majority;
            return node;
        }

        // her ozellik icin deneme yapiyorum, kategorileri en iyi ayiran bolme secilecek
        String[] features = {"gender", "amount", "price", "brandCode"};
        double bestScore = "gini".equalsIgnoreCase(criterion) ? Double.MAX_VALUE : -Double.MAX_VALUE;
        String bestFeature = null;
        double bestThreshold = 0;
        List<UserRecord> bestLeft = null;
        List<UserRecord> bestRight = null;

        for (String feature : features) {
            // threshold dedigimiz bolme noktasi, ornek: amount <= 0.42
            Set<Double> thresholds = getThresholdCandidates(data, feature);

            for (double t : thresholds) {
                List<UserRecord> left = new ArrayList<>();
                List<UserRecord> right = new ArrayList<>();

                for (UserRecord r : data) {
                    if (getFeatureValue(r, feature) <= t) {
                        left.add(r);
                    } else {
                        right.add(r);
                    }
                }

                // tum satirlar ayni tarafa gidiyorsa bolme hic ise yaramiyor
                if (left.isEmpty() || right.isEmpty()) continue;

                double score;
                if ("gini".equalsIgnoreCase(criterion)) {
                    // gini alt dallarin ne kadar karisik oldugunu olcer, dusuk daha temiz
                    score = (left.size() * calculateGiniImpurity(left) +
                             right.size() * calculateGiniImpurity(right)) / data.size();
                    if (score < bestScore) {
                        bestScore = score;
                        bestFeature = feature;
                        bestThreshold = t;
                        bestLeft = left;
                        bestRight = right;
                    }
                } else {
                    // bilgi kazanci belirsizligin ne kadar azaldigina bakiyor.
                    // buyukse bu bolme etiketleri daha iyi aciklamis demek.
                    double parentEntropy = calculateEntropy(data);
                    double childEntropy = (left.size() * calculateEntropy(left) +
                                           right.size() * calculateEntropy(right)) / data.size();
                    score = parentEntropy - childEntropy;
                    if (score > bestScore) {
                        bestScore = score;
                        bestFeature = feature;
                        bestThreshold = t;
                        bestLeft = left;
                        bestRight = right;
                    }
                }
            }
        }

        // hicbir ozellik datayi bolemediyse burada cogunluk etiketini tahmin ediyoruz
        if (bestFeature == null) {
            node.predictedLabel = majority;
            return node;
        }

        // bu dugum artik bir soru gibi: deger <= threshold ise sol, degilse sag
        node.featureName = bestFeature;
        node.threshold = bestThreshold;
        node.left = buildTree(bestLeft, depth + 1);
        node.right = buildTree(bestRight, depth + 1);

        return node;
    }

    private double getFeatureValue(UserRecord r, String feature) {
        // agac ozellik ismiyle calisiyor, burada onu satirdaki sayisal degere ceviriyorum
        switch (feature) {
            case "gender":    return r.getGenderEncoded();
            case "amount":    return r.getAmountNorm();
            case "price":     return r.getPriceNorm();
            case "brandCode": return r.getBrandCodeNorm();
            default:          return 0.0;
        }
    }

    private Set<Double> getThresholdCandidates(List<UserRecord> data, String feature) {
        List<Double> values = new ArrayList<>();
        for (UserRecord r : data) {
            values.add(getFeatureValue(r, feature));
        }
        Collections.sort(values);

        // iki farkli degerin ortasini almak daha mantikli.
        // mesela 0.2 ve 0.6 varsa threshold 0.4 deneniyor.
        Set<Double> thresholds = new LinkedHashSet<>();
        for (int i = 0; i < values.size() - 1; i++) {
            if (!values.get(i).equals(values.get(i + 1))) {
                thresholds.add((values.get(i) + values.get(i + 1)) / 2.0);
            }
        }
        if ("gender".equals(feature)) {
            // gender 0/1 oldugu icin 0.5 iki tarafi temiz ayiriyor
            thresholds.clear();
            thresholds.add(0.5);
        }
        return thresholds;
    }

    private boolean allSameCategory(List<UserRecord> data) {
        String first = data.get(0).getCategory();
        for (UserRecord r : data) {
            if (!r.getCategory().equals(first)) return false;
        }
        return true;
    }

    private double calculateGiniImpurity(List<UserRecord> data) {
        Map<String, Integer> counts = new HashMap<>();
        for (UserRecord r : data) {
            counts.put(r.getCategory(), counts.getOrDefault(r.getCategory(), 0) + 1);
        }
        double gini = 1.0;
        for (int count : counts.values()) {
            double p = (double) count / data.size();
            // her sinif icin olasiligin karesini cikariyoruz: 1 - sum(p^2)
            gini -= p * p;
        }
        return gini;
    }

    private double calculateEntropy(List<UserRecord> data) {
        Map<String, Integer> counts = new HashMap<>();
        for (UserRecord r : data) {
            counts.put(r.getCategory(), counts.getOrDefault(r.getCategory(), 0) + 1);
        }
        double entropy = 0.0;
        for (int count : counts.values()) {
            double p = (double) count / data.size();
            if (p > 0) {
                // entropy genelde bit olarak yazildigi icin log base 2 kullaniyorum
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private String majorityLabel(List<UserRecord> data) {
        Map<String, Integer> counts = new HashMap<>();
        for (UserRecord r : data) {
            counts.put(r.getCategory(), counts.getOrDefault(r.getCategory(), 0) + 1);
        }
        int max = -1;
        String best = null;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            // yaprak icin cogunluk oyu. esitlik varsa alfabetik secip sabit tutuyorum
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            } else if (e.getValue() == max) {
                if (best == null || e.getKey().compareTo(best) < 0) {
                    best = e.getKey();
                }
            }
        }
        return best != null ? best : "Unknown";
    }

    @Override
    public String predict(UserRecord user) {
        if (root == null) return "Unknown";
        TreeNode current = root;
        while (current.predictedLabel == null) {
            // bu dugumun ozelligine bakip sorusunu cevapliyoruz
            double val = getFeatureValue(user, current.featureName);
            if (val <= current.threshold) {
                current = current.left;
            } else {
                current = current.right;
            }
            if (current == null) return "Unknown";
        }
        return current.predictedLabel;
    }

    @Override
    public String getName() {
        return "Decision Tree (Depth=" + maxDepth + ", " + criterion + ")";
    }
}
