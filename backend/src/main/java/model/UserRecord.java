package model;

public class UserRecord {
    private String clientCode;
    private String gender;
    private int genderEncoded;        // 0 kadin, 1 erkek
    private double amount;
    private double amountNorm;        // min-max ile 0-1 arasina cekilmis hali
    private double price;
    private double priceNorm;         // fiyat icin normalize edilmis deger
    private int brandCode;
    private double brandCodeNorm;     // marka kodunun normalize hali
    private String brand;
    private String category;          // tahmin etmeye calistigimiz etiket

    public UserRecord(String clientCode, String gender, double amount, double price,
                      String brand, int brandCode, String category) {
        this.clientCode = clientCode;
        this.gender = gender;
        this.amount = amount;
        this.price = price;
        this.brand = brand;
        this.brandCode = brandCode;
        this.category = category;
    }

    // getterlar
    public String getClientCode()   { return clientCode; }             // hocanin neden istedigini tam anlamadim
    public String getGender()       { return gender; }
    public int getGenderEncoded()   { return genderEncoded; }
    public double getAmount()       { return amount; }
    public double getAmountNorm()   { return amountNorm; }
    public double getPrice()        { return price; }
    public double getPriceNorm()    { return priceNorm; }
    public int getBrandCode()       { return brandCode; }              // bu da biraz kotu pratik gibi, model ogrenmek yerine ezberleyebilir
    public double getBrandCodeNorm(){ return brandCodeNorm; }
    public String getBrand()        { return brand; }
    public String getCategory()     { return category; }

    // excel yuklendikten sonra hesaplanan degerler
    public void setGenderEncoded(int genderEncoded)     { this.genderEncoded = genderEncoded; }
    public void setAmountNorm(double amountNorm)        { this.amountNorm = amountNorm; }
    public void setPriceNorm(double priceNorm)          { this.priceNorm = priceNorm; }
    public void setBrandCode(int brandCode)             { this.brandCode = brandCode; }
    public void setBrandCodeNorm(double brandCodeNorm)  { this.brandCodeNorm = brandCodeNorm; }

    @Override
    public String toString() {
        return "UserRecord{" +
                "clientCode='" + clientCode + '\'' +
                ", gender='" + gender + '\'' +
                ", genderEncoded=" + genderEncoded +
                ", amount=" + amount +
                ", amountNorm=" + String.format("%.4f", amountNorm) +
                ", price=" + price +
                ", priceNorm=" + String.format("%.4f", priceNorm) +
                ", brand='" + brand + '\'' +
                ", brandCode=" + brandCode +
                ", brandCodeNorm=" + String.format("%.4f", brandCodeNorm) +
                ", category='" + category + '\'' +
                '}';
    }
}
