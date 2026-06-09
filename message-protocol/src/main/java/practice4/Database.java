package practice4;

import java.sql.*;

public class Database {
    private final String url;

    public Database(String url) {
        this.url = url;
        try {
            initialize();
        } catch (SQLException e) { throw new RuntimeException("Failed to initialize database schema", e); }
    }

    public Connection getConnection() throws SQLException { return DriverManager.getConnection(url); }

    private void initialize() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    name     TEXT    NOT NULL UNIQUE,
                    category TEXT,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    price    REAL    NOT NULL DEFAULT 0.0
                )
            """);
        }
    }
}