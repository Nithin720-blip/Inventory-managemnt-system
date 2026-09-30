package com.inventory.dao;

import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ReservationDAO {
    public Reservation create(Connection c, long userId, long productId, int quantity, ReservationStatus status) throws SQLException {
        return create(c, userId, productId, quantity, status, null);
    }

    public Reservation create(Connection c, long userId, long productId, int quantity, ReservationStatus status, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO reservations (user_id, product_id, quantity, status, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, userId);
            s.setLong(2, productId);
            s.setInt(3, quantity);
            s.setString(4, status.name());
            if (expiresAt != null) {
                s.setTimestamp(5, Timestamp.valueOf(expiresAt));
            } else {
                s.setNull(5, Types.TIMESTAMP);
            }
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) return new Reservation(r.getLong(1), userId, productId, quantity, status, LocalDateTime.now(), expiresAt);
            }
        }
        throw new SQLException("Failed to create reservation");
    }

    public List<Reservation> findExpiredActive(Connection c) throws SQLException {
        String sql = "SELECT id, user_id, product_id, quantity, status, created_at, expires_at FROM reservations WHERE status = 'ACTIVE' AND expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP FOR UPDATE";
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                list.add(map(r));
            }
        }
        return list;
    }

    public Reservation findById(long id) throws SQLException {
        try (Connection c = com.inventory.util.DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT id,user_id,product_id,quantity,status,created_at,expires_at FROM reservations WHERE id = ?")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return map(r);
            }
        }
    }

    public Reservation findById(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT id,user_id,product_id,quantity,status,created_at,expires_at FROM reservations WHERE id = ? FOR UPDATE")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return map(r);
            }
        }
    }

    public List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = com.inventory.util.DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT id,user_id,product_id,quantity,status,created_at,expires_at FROM reservations ORDER BY id DESC");
             ResultSet r = s.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public boolean updateStatus(Connection c, long id, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status.name());
            s.setLong(2, id);
            return s.executeUpdate() == 1;
        }
    }

    public boolean releaseReservedStock(Connection c, long productId, int quantity) throws SQLException {
        String sql = "UPDATE inventory SET available_qty = available_qty + ?, reserved_qty = reserved_qty - ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND reserved_qty >= ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, quantity);
            s.setInt(2, quantity);
            s.setLong(3, productId);
            s.setInt(4, quantity);
            return s.executeUpdate() == 1;
        }
    }

    public boolean confirmReservedStock(Connection c, long productId, int quantity) throws SQLException {
        String sql = "UPDATE inventory SET reserved_qty = reserved_qty - ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ? AND reserved_qty >= ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, quantity);
            s.setLong(2, productId);
            s.setInt(3, quantity);
            return s.executeUpdate() == 1;
        }
    }

    private Reservation map(ResultSet r) throws SQLException {
        Timestamp created = r.getTimestamp("created_at");
        Timestamp expires = r.getTimestamp("expires_at");
        return new Reservation(r.getLong("id"), r.getLong("user_id"), r.getLong("product_id"), r.getInt("quantity"), ReservationStatus.valueOf(r.getString("status")), created.toLocalDateTime(), expires == null ? null : expires.toLocalDateTime());
    }
}

