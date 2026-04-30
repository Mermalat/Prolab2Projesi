package data;

import model.UserRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreProcessor {

    public static Map<String, Integer> buildBrandEncoding(List<UserRecord> allData) {
        Set<String> uniqueBrands = new HashSet<>();
        for (UserRecord record : allData) {
            if (record.getBrand() != null) {
                uniqueBrands.add(record.getBrand());
            }
        }
        List<String> sortedBrands = new ArrayList<>(uniqueBrands);
        Collections.sort(sortedBrands);
        Map<String, Integer> encoding = new HashMap<>();
        for (int i = 0; i < sortedBrands.size(); i++) {
            encoding.put(sortedBrands.get(i), i);
        }
        return encoding;
    }

    public static void encode(List<UserRecord> allData) {
        Map<String, Integer> brandEncoding = buildBrandEncoding(allData);
        for (UserRecord record : allData) {
            // E/Erkek/erkek ise 1 yapiyorum, kalanlari 0 kabul ediyorum
            String g = record.getGender();
            if ("E".equalsIgnoreCase(g) || "Male".equalsIgnoreCase(g) || "Erkek".equalsIgnoreCase(g)) {
                record.setGenderEncoded(1);
            } else {
                record.setGenderEncoded(0);
            }

            if (record.getBrand() != null && brandEncoding.containsKey(record.getBrand())) {
                record.setBrandCode(brandEncoding.get(record.getBrand()));
            }
        }
    }

    public static void normalize(List<UserRecord> allData) {
        if (allData.isEmpty()) return;

        double minAmount = Double.MAX_VALUE, maxAmount = -Double.MAX_VALUE;
        double minPrice = Double.MAX_VALUE, maxPrice = -Double.MAX_VALUE;
        double minBrand = Double.MAX_VALUE, maxBrand = -Double.MAX_VALUE;

        for (UserRecord record : allData) {
            if (record.getAmount() < minAmount) minAmount = record.getAmount();
            if (record.getAmount() > maxAmount) maxAmount = record.getAmount();
            if (record.getPrice() < minPrice) minPrice = record.getPrice();
            if (record.getPrice() > maxPrice) maxPrice = record.getPrice();
            if (record.getBrandCode() < minBrand) minBrand = record.getBrandCode();
            if (record.getBrandCode() > maxBrand) maxBrand = record.getBrandCode();
        }

        for (UserRecord record : allData) {
            if (Double.compare(minAmount, maxAmount) == 0) {
                record.setAmountNorm(0.0);
            } else {
                record.setAmountNorm((record.getAmount() - minAmount) / (maxAmount - minAmount));
            }

            if (Double.compare(minPrice, maxPrice) == 0) {
                record.setPriceNorm(0.0);
            } else {
                record.setPriceNorm((record.getPrice() - minPrice) / (maxPrice - minPrice));
            }

            if (Double.compare(minBrand, maxBrand) == 0) {
                record.setBrandCodeNorm(0.0);
            } else {
                record.setBrandCodeNorm((record.getBrandCode() - minBrand) / (maxBrand - minBrand));
            }
        }

        System.out.println("[PreProcessor] Normalization complete.");
        System.out.println("  Amount range: [" + String.format("%.2f", minAmount) + ", " + String.format("%.2f", maxAmount) + "]");
        System.out.println("  Price range: [" + String.format("%.2f", minPrice) + ", " + String.format("%.2f", maxPrice) + "]");
        System.out.println("  BrandCode range: [" + (int) minBrand + ", " + (int) maxBrand + "]");
    }
}
