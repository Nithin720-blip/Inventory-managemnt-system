package com.inventory.dao;

import com.inventory.model.Order;
import com.inventory.model.OrderStatus;
import java.sql.*;
import java.time.LocalDateTime;

public class OrderDAO {
    public long create(Connection c, Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id,total_amount,status) VALUES (?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, order.getUserId()); s.setBigDecimal(2, order.getTotalAmount()); s.setString(3, order.getStatus().name()); s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) { if (r.next()) return r.getLong(1); }
        }
        throw new SQLException("Failed to create order");
    }

    public Order findById(long id) throws SQLException {
        try (Connection c = com.inventory.util.DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT id,user_id,total_amount,status,created_at FROM orders WHERE id=?")) {
            s.setLong(1,id); try(ResultSet r=s.executeQuery()) {
                if(!r.next()) return null;
                Timestamp timestamp=r.getTimestamp("created_at");
                return new Order(r.getLong("id"),r.getLong("user_id"),r.getBigDecimal("total_amount"),OrderStatus.valueOf(r.getString("status")),timestamp.toLocalDateTime());
            }
        }
    }

    public java.util.List<Order> findAll() throws SQLException {
        java.util.List<Order> list = new java.util.ArrayList<>();
        try (Connection c = com.inventory.util.DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT id,user_id,total_amount,status,created_at FROM orders ORDER BY id DESC");
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                Timestamp timestamp = r.getTimestamp("created_at");
                list.add(new Order(r.getLong("id"), r.getLong("user_id"), r.getBigDecimal("total_amount"), OrderStatus.valueOf(r.getString("status")), timestamp.toLocalDateTime()));
            }
        }
        return list;
    }
}

