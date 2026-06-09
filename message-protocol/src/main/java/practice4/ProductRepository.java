package practice4;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {
    private final Database database;

    public ProductRepository(Database database) {
        this.database = database;
    }

    public Product create(Product product) {
        String sql = "INSERT INTO products (name, category, quantity, price) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setDouble(4, product.getPrice());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) product.setId(keys.getLong(1));
            return product;
        } catch (SQLException e) { throw new RuntimeException("Failed to create product: " + product.getName(), e); }
    }

    public Optional<Product> findById(long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        } catch (SQLException e) { throw new RuntimeException("Failed to find product by id: " + id, e); }
    }

    public Optional<Product> findByName(String name) {
        String sql = "SELECT * FROM products WHERE name = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        } catch (SQLException e) { throw new RuntimeException("Failed to find product by name: " + name, e); }
    }

    public PageResult<Product> findAll(ProductFilter filter) {
        List<Object> countParams = new ArrayList<>();
        List<Object> dataParams  = new ArrayList<>();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        StringBuilder dataSql  = new StringBuilder("SELECT * FROM products WHERE 1=1");

        appendWhereClause(countSql, countParams, filter);
        appendWhereClause(dataSql,  dataParams,  filter);

        dataSql.append(" LIMIT ? OFFSET ?");
        dataParams.add(filter.getPageSize());
        dataParams.add(filter.getPage() * filter.getPageSize());

        try (Connection conn = database.getConnection()) {
            long totalCount;
            try (PreparedStatement ps = conn.prepareStatement(countSql.toString())) {
                setParams(ps, countParams);
                ResultSet rs = ps.executeQuery();
                totalCount = rs.next() ? rs.getLong(1) : 0;
            }

            List<Product> products = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(dataSql.toString())) {
                setParams(ps, dataParams);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) products.add(mapRow(rs));
            }

            return new PageResult<>(products, totalCount, filter.getPage(), filter.getPageSize());
        } catch (SQLException e) { throw new RuntimeException("Failed to search products", e); }
    }

    public boolean update(Product product) {
        String sql = "UPDATE products SET name = ?, category = ?, quantity = ?, price = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setInt(3, product.getQuantity());
            ps.setDouble(4, product.getPrice());
            ps.setLong(5, product.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to update product: " + product.getId(), e); }
    }

    public boolean delete(String name) {
        String sql = "DELETE FROM products WHERE name = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to delete product: " + name, e); }
    }

    public int addToStock(String name, int amount) {
        String sql = """
            INSERT INTO products (name, quantity) VALUES (?, ?)
            ON CONFLICT(name) DO UPDATE SET quantity = quantity + excluded.quantity
        """;
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, amount);
            ps.executeUpdate();
            return findByName(name).map(Product::getQuantity).orElse(0);
        } catch (SQLException e) { throw new RuntimeException("Failed to add stock for: " + name, e); }
    }

    public boolean deleteFromStock(String name, int amount) {
        String sql = "UPDATE products SET quantity = quantity - ? WHERE name = ? AND quantity >= ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, amount);
            ps.setString(2, name);
            ps.setInt(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to delete from stock: " + name, e); }
    }

    public boolean setPrice(String name, double price) {
        String sql = "UPDATE products SET price = ? WHERE name = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, price);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to set price for: " + name, e); }
    }

    public boolean addCategory(String categoryName) {
        String sql = "INSERT OR IGNORE INTO categories (name) VALUES (?)";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoryName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to add category: " + categoryName, e); }
    }

    public boolean assignCategory(String productName, String categoryName) {
        String sql = "UPDATE products SET category = ? WHERE name = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoryName);
            ps.setString(2, productName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Failed to assign category", e); }
    }

    private void appendWhereClause(StringBuilder sql, List<Object> params, ProductFilter filter) {
        if (filter.getName() != null && !filter.getName().isBlank()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + filter.getName() + "%");
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            sql.append(" AND category = ?");
            params.add(filter.getCategory());
        }
        if (filter.getMinQuantity() != null) {
            sql.append(" AND quantity >= ?");
            params.add(filter.getMinQuantity());
        }
        if (filter.getMaxQuantity() != null) {
            sql.append(" AND quantity <= ?");
            params.add(filter.getMaxQuantity());
        }
        if (filter.getMinPrice() != null) {
            sql.append(" AND price >= ?");
            params.add(filter.getMinPrice());
        }
        if (filter.getMaxPrice() != null) {
            sql.append(" AND price <= ?");
            params.add(filter.getMaxPrice());
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setQuantity(rs.getInt("quantity"));
        p.setPrice(rs.getDouble("price"));
        return p;
    }
}