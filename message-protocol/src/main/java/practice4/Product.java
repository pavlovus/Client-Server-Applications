package practice4;

public class Product {
    private long id;
    private String name;
    private String category;
    private int quantity;
    private double price;

    public Product() {}

    public Product(String name, String category, int quantity, double price) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String serialize() { return id + "|" + name + "|" + (category != null ? category : "") + "|" + quantity + "|" + price; }

    public static Product deserialize(String s) {
        String[] parts = s.split("\\|", -1);
        Product p = new Product();
        p.setId(Long.parseLong(parts[0]));
        p.setName(parts[1]);
        p.setCategory(parts[2].isEmpty() ? null : parts[2]);
        p.setQuantity(Integer.parseInt(parts[3]));
        p.setPrice(Double.parseDouble(parts[4]));
        return p;
    }

    @Override
    public String toString() { return "Product{id=" + id + ", name='" + name + "', category='" + category + "', quantity=" + quantity + ", price=" + price + "}"; }
}