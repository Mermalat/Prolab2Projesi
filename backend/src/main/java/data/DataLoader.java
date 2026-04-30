package data;

import model.UserRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    public static List<UserRecord> load(String filePath) {
        try (InputStream is = new FileInputStream(filePath)) {
            return load(is);
        } catch (Exception e) {
            System.err.println("[DataLoader] Error reading file: " + filePath);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static List<UserRecord> load(InputStream is) {
        List<UserRecord> records = new ArrayList<>();
        int skipped = 0;

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                System.err.println("[DataLoader] No header row found.");
                return records;
            }

            // kolonlari basliga gore buluyorum, excel dosyasinda siralari degisebiliyor
            int genderIdx = -1, amountIdx = -1, priceIdx = -1;
            int brandIdx = -1, brandCodeIdx = -1, categoryIdx = -1, clientCodeIdx = -1;

            for (Cell cell : headerRow) {
                String headerName = getCellString(cell);
                if (headerName == null) continue;
                headerName = headerName.trim().toUpperCase().replace(" ", "");

                switch (headerName) {
                    case "CLIENTCODE":
                        clientCodeIdx = cell.getColumnIndex();
                        break;
                    case "GENDER":
                        genderIdx = cell.getColumnIndex();
                        break;
                    case "AMOUNT":
                        amountIdx = cell.getColumnIndex();
                        break;
                    case "PRICE":
                        priceIdx = cell.getColumnIndex();
                        break;
                    case "BRAND":
                    case "BRAND_NAME":
                    case "BRANDNAME":
                        brandIdx = cell.getColumnIndex();
                        break;
                    case "BRANDCODE":
                    case "BRAND_CODE":
                        brandCodeIdx = cell.getColumnIndex();
                        break;
                    case "CATEGORY_NAME1":
                    case "CATEGORY":
                    case "CATEGORYNAME1":
                    case "CATEGORYNAME":
                        categoryIdx = cell.getColumnIndex();
                        break;
                }
            }

            System.out.println("[DataLoader] Column indices: clientCode=" + clientCodeIdx +
                    ", gender=" + genderIdx + ", amount=" + amountIdx +
                    ", price=" + priceIdx + ", brand=" + brandIdx +
                    ", brandCode=" + brandCodeIdx + ", category=" + categoryIdx);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) {
                    skipped++;
                    continue;
                }

                try {
                    String clientCode = clientCodeIdx != -1 ? getCellString(row.getCell(clientCodeIdx)) : "";
                    String gender = getCellString(row.getCell(genderIdx));
                    double amount = getCellDouble(row.getCell(amountIdx));
                    double price = getCellDouble(row.getCell(priceIdx));
                    String brand = getCellString(row.getCell(brandIdx));

                    int brandCode = -1;
                    if (brandCodeIdx != -1) {
                        try {
                            brandCode = (int) getCellDouble(row.getCell(brandCodeIdx));
                        } catch (Exception ignored) {
                            // zaten sonra tekrar encode edilecek, burada patlamasi onemli degil
                        }
                    }

                    String category = getCellString(row.getCell(categoryIdx));

                    if (isBlank(gender) || isBlank(brand) || isBlank(category)) {
                        skipped++;
                        continue;
                    }

                    if (amount <= 0 || price <= 0) {
                        skipped++;
                        continue;
                    }

                    records.add(new UserRecord(
                            clientCode != null ? clientCode.trim() : "",
                            gender.trim(), amount, price,
                            brand.trim(), brandCode, category.trim()));

                } catch (Exception e) {
                    skipped++;
                }
            }

            System.out.println("[DataLoader] " + records.size() + " records loaded, " + skipped + " rows skipped.");
        } catch (Exception e) {
            System.err.println("[DataLoader] Error parsing Excel workbook.");
            e.printStackTrace();
        }
        return records;
    }

    private static int lastSkipped = 0;

    public static int getLastSkipped() {
        return lastSkipped;
    }

    public static List<UserRecord> loadWithSkipCount(InputStream is) {
        List<UserRecord> records = new ArrayList<>();
        lastSkipped = 0;

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                System.err.println("[DataLoader] No header row found.");
                return records;
            }

            int genderIdx = -1, amountIdx = -1, priceIdx = -1;
            int brandIdx = -1, brandCodeIdx = -1, categoryIdx = -1, clientCodeIdx = -1;

            for (Cell cell : headerRow) {
                String headerName = getCellString(cell);
                if (headerName == null) continue;
                headerName = headerName.trim().toUpperCase().replace(" ", "");

                switch (headerName) {
                    case "CLIENTCODE":
                        clientCodeIdx = cell.getColumnIndex();
                        break;
                    case "GENDER":
                        genderIdx = cell.getColumnIndex();
                        break;
                    case "AMOUNT":
                        amountIdx = cell.getColumnIndex();
                        break;
                    case "PRICE":
                        priceIdx = cell.getColumnIndex();
                        break;
                    case "BRAND":
                    case "BRAND_NAME":
                    case "BRANDNAME":
                        brandIdx = cell.getColumnIndex();
                        break;
                    case "BRANDCODE":
                    case "BRAND_CODE":
                        brandCodeIdx = cell.getColumnIndex();
                        break;
                    case "CATEGORY_NAME1":
                    case "CATEGORY":
                    case "CATEGORYNAME1":
                    case "CATEGORYNAME":
                        categoryIdx = cell.getColumnIndex();
                        break;
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) { lastSkipped++; continue; }

                try {
                    String clientCode = clientCodeIdx != -1 ? getCellString(row.getCell(clientCodeIdx)) : "";
                    String gender = getCellString(row.getCell(genderIdx));
                    double amount = getCellDouble(row.getCell(amountIdx));
                    double price = getCellDouble(row.getCell(priceIdx));
                    String brand = getCellString(row.getCell(brandIdx));

                    int brandCode = -1;
                    if (brandCodeIdx != -1) {
                        try { brandCode = (int) getCellDouble(row.getCell(brandCodeIdx)); }
                        catch (Exception ignored) {}
                    }

                    String category = getCellString(row.getCell(categoryIdx));

                    if (isBlank(gender) || isBlank(brand) || isBlank(category)) {
                        lastSkipped++; continue;
                    }
                    if (amount <= 0 || price <= 0) {
                        lastSkipped++; continue;
                    }

                    records.add(new UserRecord(
                            clientCode != null ? clientCode.trim() : "",
                            gender.trim(), amount, price,
                            brand.trim(), brandCode, category.trim()));

                } catch (Exception e) {
                    lastSkipped++;
                }
            }

            System.out.println("[DataLoader] " + records.size() + " records loaded, " + lastSkipped + " rows skipped.");
        } catch (Exception e) {
            System.err.println("[DataLoader] Error parsing Excel workbook.");
            e.printStackTrace();
        }
        return records;
    }

    // hucreden string almaya calisiyor, numeric/formula gelirse de idare ediyor
    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    // sayi client code gibi gelirse bilimsel format olmasin
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val) && !Double.isInfinite(val)) {
                        return String.valueOf((long) val);
                    }
                    return String.valueOf(val);
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // hucreden double deger aliyor, virgullu turkce sayilari da nokta yapiyor
    private static double getCellDouble(Cell cell) {
        if (cell == null) throw new IllegalArgumentException("Null cell");
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                String str = cell.getStringCellValue().trim().replace(",", ".");
                return Double.parseDouble(str);
            case FORMULA:
                return cell.getNumericCellValue();
            default:
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getCellType());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
