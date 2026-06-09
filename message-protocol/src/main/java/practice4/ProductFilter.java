package practice4;

public class ProductFilter {
    private final String name;
    private final String category;
    private final Integer minQuantity;
    private final Integer maxQuantity;
    private final Double minPrice;
    private final Double maxPrice;
    private final int page;
    private final int pageSize;

    private ProductFilter(Builder builder) {
        this.name = builder.name;
        this.category = builder.category;
        this.minQuantity = builder.minQuantity;
        this.maxQuantity = builder.maxQuantity;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.page = builder.page;
        this.pageSize = builder.pageSize;
    }

    public static Builder builder() { return new Builder(); }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public Integer getMinQuantity() { return minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    public static ProductFilter parse(String content) {
        Builder builder = builder();
        if (content == null || content.isBlank()) return builder.build();

        for (String part : content.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "name" -> builder.name(kv[1].trim());
                case "category" -> builder.category(kv[1].trim());
                case "minPrice" -> builder.minPrice(Double.parseDouble(kv[1].trim()));
                case "maxPrice" -> builder.maxPrice(Double.parseDouble(kv[1].trim()));
                case "minQty" -> builder.minQuantity(Integer.parseInt(kv[1].trim()));
                case "maxQty" -> builder.maxQuantity(Integer.parseInt(kv[1].trim()));
                case "page" -> builder.page(Integer.parseInt(kv[1].trim()));
                case "pageSize" -> builder.pageSize(Integer.parseInt(kv[1].trim()));
            }
        }
        return builder.build();
    }

    public static class Builder {
        private String name;
        private String category;
        private Integer minQuantity;
        private Integer maxQuantity;
        private Double minPrice;
        private Double maxPrice;
        private int page = 0;
        private int pageSize = 10;

        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder minQuantity(Integer v) { this.minQuantity = v; return this; }
        public Builder maxQuantity(Integer v) { this.maxQuantity = v; return this; }
        public Builder minPrice(Double v) { this.minPrice = v; return this; }
        public Builder maxPrice(Double v) { this.maxPrice = v; return this; }
        public Builder page(int page) { this.page = page; return this; }
        public Builder pageSize(int pageSize) { this.pageSize = pageSize; return this; }
        public ProductFilter build() { return new ProductFilter(this); }
    }
}