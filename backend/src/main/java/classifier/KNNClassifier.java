package classifier;

import model.UserRecord;
import java.util.*;

public class KNNClassifier extends BaseAlgorithm {
    private int k;

    public KNNClassifier(int k) {
        this.k = k;
    }

    @Override
    public void train(List<UserRecord> trainingData) {
        // KNN aslinda egitilmiyor, sadece eski satirlari aklinda tutuyor.
        // yani buradaki egitim kismi biraz "datayi kaydet" gibi.
        this.trainingData = trainingData;
    }

    @Override
    public String predict(UserRecord user) {
        if (trainingData == null || trainingData.isEmpty()) return "Unknown";

        // k datadan buyukse eldeki tum satirlari kullanmak lazim.
        // yoksa asagidaki dongu olmayan komsuya bakmaya calisir.
        int actualK = Math.min(this.k, trainingData.size());

        // mesafeyi ve satirin indexini birlikte tutuyorum
        List<double[]> distances = new ArrayList<>();
        for (int i = 0; i < trainingData.size(); i++) {
            double dist = euclideanDistance(user, trainingData.get(i));
            distances.add(new double[]{dist, i});
        }

        // en yakin olanlar basa gelsin, cunku knn sadece yakin komsulara bakiyor
        Collections.sort(distances, Comparator.comparingDouble(a -> a[0]));

        // en yakin komsularin kategorilerini sayiyoruz.
        // en cok oy alan kategori tahmin oluyor.
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (int i = 0; i < actualK; i++) {
            int index = (int) distances.get(i)[1];
            String cat = trainingData.get(index).getCategory();
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
        }

        int maxCount = -1;
        String bestCategory = null;

        // en cok oy alan kategoriyi seciyorum.
        // esitlik varsa alfabetik aliyorum ki sonuc her calismada degismesin.
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestCategory = entry.getKey();
            } else if (entry.getValue() == maxCount) {
                if (bestCategory == null || entry.getKey().compareTo(bestCategory) < 0) {
                    bestCategory = entry.getKey();
                }
            }
        }

        return bestCategory != null ? bestCategory : "Unknown";
    }

    private double euclideanDistance(UserRecord a, UserRecord b) {
        // butun ozellikler onceden normalize edildi.
        // boylece fiyat gibi buyuk sayilar mesafeyi tek basina bozmaz.
        double dGender = a.getGenderEncoded() - b.getGenderEncoded();
        double dAmount = a.getAmountNorm()    - b.getAmountNorm();
        double dPrice  = a.getPriceNorm()     - b.getPriceNorm();
        double dBrand  = a.getBrandCodeNorm() - b.getBrandCodeNorm();

        // klasik oklid mesafesi: farklari karele, topla, karekok al.
        // deger kucukse knn icin daha benzer demek.
        return Math.sqrt(dGender * dGender + dAmount * dAmount +
                         dPrice * dPrice + dBrand * dBrand);
    }

    @Override
    public String getName() {
        return "KNN (K=" + k + ")";
    }
}
