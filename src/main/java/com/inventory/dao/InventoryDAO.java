package com.inventory.dao;

import com.inventory.model.Inventory;
import com.inventory.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class InventoryDAO {
    public Inventory create(long productId, int availableQty) throws SQLException {
        if (availableQty < 0) throw new IllegalArgumentException("Available quantity cannot be negative");
        String sql = "INSERT INTO inventory (product_id, available_qty, reserved_qty) VALUES (?, ?, 0)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, productId);
            s.setInt(2, availableQty);
            s.executeUpdate();
        }
        return findByProductId(productId);
    }

    public Inventory findByProductId(long productId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return findByProductId(c, productId); }
    }

    public Inventory findByProductId(Connection c, long productId) throws SQLException {
        String sql = "SELECT product_id, available_qty, reserved_qty, updated_at FROM inventory WHERE product_id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, productId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                Timestamp updated = r.getTimestamp("updated_at");
                return new Inventory(r.getLong("product_id"), r.getInt("available_qty"), r.getInt("reserved_qty"),
                        updated == null ? null : updated.toLocalDateTime());
            }
        }
    }

    public int getQuantity(long productId) throws SQLException {
        Inventory inventory = findByProductId(productId);
        if (inventory == null) throw new IllegalArgumentException("Inventory not found");
        return inventory.getAvailableQty();
    }

    public int getQuantityForUpdate(Connection c, long productId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT available_qty FROM inventory WHERE product_id = ? FOR UPDATE")) {
            s.setLong(1, productId);
            try (ResultSet r = s.executeQuery()) { if (r.next()) return r.getInt("available_qty"); }
        }
        throw new IllegalArgumentException("Inventory not found");
    }

    public boolean reserveStock(Connection c, long productId, int quantity) throws SQLException {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        String sql = "UPDATE inventory SET available_qty = available_qty - ?, reserved_qty = reserved_qty + ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND available_qty >= ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, quantity);
            s.setInt(2, quantity);
            s.setLong(3, productId);
            s.setInt(4, quantity);
            return s.executeUpdate() == 1;
        }
    }

    public void increaseStock(long productId, int amount) throws SQLException {
        validatePositive(amount);
        String sql = "UPDATE inventory SET available_qty = available_qty + ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, amount);
            s.setLong(2, productId);
            if (s.executeUpdate() != 1) throw new IllegalArgumentException("Inventory not found");
        }
    }

    public void decreaseStock(long productId, int amount) throws SQLException {
        validatePositive(amount);
        String sql = "UPDATE inventory SET available_qty = available_qty - ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND available_qty >= ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, amount);
            s.setLong(2, productId);
            s.setInt(3, amount);
            if (s.executeUpdate() != 1) throw new IllegalArgumentException("Insufficient inventory or inventory not found");
        }
    }

    private void validatePositive(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
    }
}
