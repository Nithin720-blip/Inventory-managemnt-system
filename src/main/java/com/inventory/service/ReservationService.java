package com.inventory.service;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.ReservationDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class ReservationService {
    private final InventoryDAO inventoryDAO;
    private final ReservationDAO reservationDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    public ReservationService() { this(new InventoryDAO(), new ReservationDAO(), new ProductDAO(), new UserDAO()); }

    public ReservationService(InventoryDAO inventoryDAO, ReservationDAO reservationDAO, ProductDAO productDAO, UserDAO userDAO) {
        this.inventoryDAO = inventoryDAO; this.reservationDAO = reservationDAO; this.productDAO = productDAO; this.userDAO = userDAO;
    }

    public Reservation reserveStock(long userId, long productId, int quantity) throws SQLException {
        return reserveStock(userId, productId, quantity, 120); // Default 2-minute (120 seconds) TTL
    }

    public Reservation reserveStock(long userId, long productId, int quantity, int ttlSeconds) throws SQLException {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        if (ttlSeconds <= 0) throw new IllegalArgumentException("TTL must be greater than zero");
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!productDAO.exists(connection, productId)) throw new IllegalArgumentException("Product not found");
                if (!userDAO.exists(connection, userId)) throw new IllegalArgumentException("User not found");
                int available = inventoryDAO.getQuantityForUpdate(connection, productId);
                if (available < quantity) throw new IllegalArgumentException("Insufficient inventory");
                if (!inventoryDAO.reserveStock(connection, productId, quantity)) throw new SQLException("Inventory update failed");
                java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusSeconds(ttlSeconds);
                Reservation reservation = reservationDAO.create(connection, userId, productId, quantity, ReservationStatus.ACTIVE, expiresAt);
                connection.commit();
                return reservation;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public int expireOverdueReservations() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                java.util.List<Reservation> expiredList = reservationDAO.findExpiredActive(connection);
                int count = 0;
                for (Reservation reservation : expiredList) {
                    if (reservationDAO.updateStatus(connection, reservation.getId(), ReservationStatus.EXPIRED)) {
                        reservationDAO.releaseReservedStock(connection, reservation.getProductId(), reservation.getQuantity());
                        count++;
                    }
                }
                connection.commit();
                return count;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public boolean confirmReservation(long reservationId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = reservationDAO.findById(connection, reservationId);
                if (reservation == null) throw new IllegalArgumentException("Reservation not found");
                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                    throw new IllegalStateException("Only ACTIVE reservations can be confirmed");
                }

                if (!reservationDAO.updateStatus(connection, reservationId, ReservationStatus.CONFIRMED)) {
                    throw new SQLException("Failed to update reservation status");
                }
                if (!reservationDAO.confirmReservedStock(connection, reservation.getProductId(), reservation.getQuantity())) {
                    throw new SQLException("Failed to update inventory reserved quantity");
                }

                com.inventory.model.Product product = productDAO.findById(connection, reservation.getProductId());
                com.inventory.model.OrderItem item = new com.inventory.model.OrderItem(0, 0, reservation.getProductId(), reservation.getQuantity(), product.getPrice());
                com.inventory.model.Order order = new com.inventory.model.Order(0, reservation.getUserId(), product.getPrice().multiply(java.math.BigDecimal.valueOf(reservation.getQuantity())), com.inventory.model.OrderStatus.CONFIRMED, null);
                
                long orderId = new com.inventory.dao.OrderDAO().create(connection, order);
                item.setOrderId(orderId);
                new com.inventory.dao.OrderItemDAO().create(connection, item);

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public boolean cancelReservation(long reservationId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = reservationDAO.findById(connection, reservationId);
                if (reservation == null) throw new IllegalArgumentException("Reservation not found");
                if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                    throw new IllegalStateException("Only ACTIVE reservations can be cancelled");
                }

                if (!reservationDAO.updateStatus(connection, reservationId, ReservationStatus.CANCELLED)) {
                    throw new SQLException("Failed to cancel reservation");
                }
                if (!reservationDAO.releaseReservedStock(connection, reservation.getProductId(), reservation.getQuantity())) {
                    throw new SQLException("Failed to release reserved inventory");
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public java.util.List<Reservation> getAllReservations() throws SQLException {
        return reservationDAO.findAll();
    }
}

