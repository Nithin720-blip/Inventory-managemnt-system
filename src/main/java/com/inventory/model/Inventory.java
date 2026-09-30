package com.inventory.model;

import java.time.LocalDateTime;

public class Inventory {
    private long productId;
    private int availableQty;
    private int reservedQty;
    private LocalDateTime updatedAt;

    public Inventory() { }

    public Inventory(long productId, int availableQty, int reservedQty, LocalDateTime updatedAt) {
        this.productId = productId;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.updatedAt = updatedAt;
    }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public int getReservedQty() { return reservedQty; }
    public void setReservedQty(int reservedQty) { this.reservedQty = reservedQty; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Inventory{productId=" + productId + ", availableQty=" + availableQty + ", reservedQty=" + reservedQty + ", updatedAt=" + updatedAt + "}";
    }
}
