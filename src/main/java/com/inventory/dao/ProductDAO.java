package com.inventory.dao;

import com.inventory.model.Product;
import com.inventory.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public boolean exists(java.sql.Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) { return resultSet.next(); }
        }
    }

    public long create(Product product) throws SQLException {

        String sql = """
        INSERT INTO products (sku, name, price, description)
        VALUES (?, ?, ?, ?)
        """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, product.getSku());
            statement.setString(2, product.getName());
            statement.setBigDecimal(3, product.getPrice());
            statement.setString(4, product.getDescription());


            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to create product");
    }

    public Product findById(long id) throws SQLException {

        String sql = """
                SELECT id, name, sku, price, created_at, description
                FROM products
                WHERE id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return new Product(
                            rs.getLong("id"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBigDecimal("price"),
                            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }

        return null;
    }

    public Product findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, name, sku, price, created_at, description FROM products WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return new Product(rs.getLong("id"), rs.getString("sku"), rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
            }
        }
    }

    public List<Product> findAll() throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT id, name, sku, price, created_at, description
                FROM products
                ORDER BY id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                Product product = new Product(
                        rs.getLong("id"),
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                );

                products.add(product);
            }
        }

        return products;
    }

    public boolean update(Product product) throws SQLException {

        String sql = """
                UPDATE products
                SET sku = ?, name = ?, description = ?, price = ?
                WHERE id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getSku());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setBigDecimal(4, product.getPrice());
            statement.setLong(5, product.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) throws SQLException {

        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;
        }
    }
}
