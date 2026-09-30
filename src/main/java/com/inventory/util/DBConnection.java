package com.inventory.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections from environment configuration when explicitly requested. */
public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3308/inventory_reservation_engine";

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        String url = environmentOrDefault("DB_URL", DEFAULT_URL);
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (username == null || password == null) {
            throw new SQLException("Configure DB_USERNAME and DB_PASSWORD environment variables before connecting.");
        }

        return DriverManager.getConnection(url, username, password);
    }

    private static String environmentOrDefault(String variable, String defaultValue) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
