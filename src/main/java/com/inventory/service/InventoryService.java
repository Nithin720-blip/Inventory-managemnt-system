package com.inventory.service;

import com.inventory.dao.InventoryDAO;

import java.sql.SQLException;

public class InventoryService {

    private final InventoryDAO inventoryDAO;

    public InventoryService(InventoryDAO inventoryDAO) {
        this.inventoryDAO = inventoryDAO;
    }

    public void addStock(long productId, int quantity)
            throws SQLException {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        inventoryDAO.increaseStock(productId, quantity);
    }

    public int getStock(long productId) throws SQLException {
        return inventoryDAO.getQuantity(productId);
    }

    public void removeStock(long productId, int quantity) throws SQLException {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        inventoryDAO.decreaseStock(productId, quantity);
    }
}
