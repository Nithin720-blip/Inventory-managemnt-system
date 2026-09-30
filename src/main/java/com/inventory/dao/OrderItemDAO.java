package com.inventory.dao;

import com.inventory.model.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OrderItemDAO {
    public void create(Connection c, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id,product_id,quantity,unit_price) VALUES (?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1,item.getOrderId()); s.setLong(2,item.getProductId()); s.setInt(3,item.getQuantity()); s.setBigDecimal(4,item.getUnitPrice()); s.executeUpdate();
        }
    }

    public java.util.List<OrderItem> findByOrderId(long orderId) throws SQLException {
        java.util.List<OrderItem> list = new java.util.ArrayList<>();
        String sql = "SELECT id, order_id, product_id, quantity, unit_price FROM order_items WHERE order_id = ?";
        try (Connection c = com.inventory.util.DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, orderId);
            try (java.sql.ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    list.add(new OrderItem(r.getLong("id"), r.getLong("order_id"), r.getLong("product_id"), r.getInt("quantity"), r.getBigDecimal("unit_price")));
                }
            }
        }
        return list;
    }
}

