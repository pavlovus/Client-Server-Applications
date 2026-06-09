package practice4;

import java.util.Optional;

public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    public Product create(Product product) {
        if (product.getName() == null || product.getName().isBlank()) throw new IllegalArgumentException("Product name cannot be blank");
        if (product.getQuantity() < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        if (product.getPrice() < 0) throw new IllegalArgumentException("Price cannot be negative");
        return repository.create(product);
    }

    public Optional<Product> getById(long id) { return repository.findById(id); }

    public Optional<Product> getByName(String name) { return repository.findByName(name); }

    public PageResult<Product> search(ProductFilter filter) { return repository.findAll(filter); }

    public boolean update(Product product) {
        if (product.getId() <= 0) throw new IllegalArgumentException("Cannot update product without valid id");
        if (product.getName() == null || product.getName().isBlank()) throw new IllegalArgumentException("Product name cannot be blank");
        if (product.getQuantity() < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        if (product.getPrice() < 0) throw new IllegalArgumentException("Price cannot be negative");
        return repository.update(product);
    }

    public boolean delete(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Product name cannot be blank");
        return repository.delete(name);
    }

    public int addToStock(String name, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return repository.addToStock(name, amount);
    }

    public boolean deleteFromStock(String name, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return repository.deleteFromStock(name, amount);
    }

    public boolean setPrice(String name, double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return repository.setPrice(name, price);
    }

    public boolean addCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) throw new IllegalArgumentException("Category name cannot be blank");
        return repository.addCategory(categoryName);
    }

    public boolean assignCategory(String productName, String categoryName) { return repository.assignCategory(productName, categoryName); }
}