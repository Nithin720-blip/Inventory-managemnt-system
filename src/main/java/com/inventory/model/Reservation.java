package com.inventory.model;

import java.time.LocalDateTime;

public class Reservation {
    private long id;
    private long userId;
    private long productId;
    private int quantity;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Reservation() { }

    public Reservation(long id, long userId, long productId, int quantity, ReservationStatus status, LocalDateTime createdAt) {
        this(id, userId, productId, quantity, status, createdAt, null);
    }

    public Reservation(long id, long userId, long productId, int quantity, ReservationStatus status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", userId=" + userId + ", productId=" + productId + ", quantity=" + quantity + ", status=" + status + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt + "}";
    }
}
