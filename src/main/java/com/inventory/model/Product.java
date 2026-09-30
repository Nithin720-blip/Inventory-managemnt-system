package com.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public Product() { }

    public Product(long id, String name, String description, BigDecimal price) {
        this(id, null, name, description, price);
    }

    public Product(long id, String sku, String name, String description, BigDecimal price) {
        this(id, sku, name, description, price, null);
    }

    public Product(long id, String sku, String name, String description, BigDecimal price, LocalDateTime createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "', name='" + name + "', description='" + description + "', price=" + price + ", createdAt=" + createdAt + "}";
    }
}
