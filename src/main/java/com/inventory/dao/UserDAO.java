package com.inventory.dao;

import com.inventory.model.User;
import com.inventory.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public boolean exists(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM users WHERE id = ?")) { s.setLong(1,id); try(ResultSet r=s.executeQuery()) { return r.next(); } }
    }
    public long create(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, user.getName()); s.setString(2, user.getEmail()); s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) { if (r.next()) return r.getLong(1); }
        }
        throw new SQLException("Failed to create user");
    }

    public User findById(long id) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT id, name, email, created_at FROM users WHERE id = ?")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public User findByEmail(String email) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT id, name, email, created_at FROM users WHERE email = ?")) {
            s.setString(1, email); try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT id, name, email, created_at FROM users ORDER BY id"); ResultSet r = s.executeQuery()) {
            while (r.next()) users.add(map(r));
        }
        return users;
    }

    public boolean update(User user) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?")) {
            s.setString(1, user.getName()); s.setString(2, user.getEmail()); s.setLong(3, user.getId()); return s.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM users WHERE id = ?")) { s.setLong(1, id); return s.executeUpdate() == 1; }
    }

    private User map(ResultSet r) throws SQLException {
        Timestamp created = r.getTimestamp("created_at");
        return new User(r.getLong("id"), r.getString("name"), r.getString("email"), created == null ? null : created.toLocalDateTime());
    }
}
