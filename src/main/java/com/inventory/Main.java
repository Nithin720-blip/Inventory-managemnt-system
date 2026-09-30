package com.inventory;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.OrderItem;
import com.inventory.model.Inventory;
import com.inventory.model.Product;
import com.inventory.model.User;
import com.inventory.service.OrderService;
import com.inventory.service.ReservationService;
import com.inventory.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" INVENTORY RESERVATION ENGINE");
        System.out.println("========================================");
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("Database connected successfully.\n");
            runDemo();
        } catch (SQLException exception) {
            System.err.println("Database operation failed: " + exception.getMessage());
            exception.printStackTrace();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Demonstration was interrupted.");
        }
    }

    private static void runDemo() throws SQLException, InterruptedException {
        ProductDAO productDAO = new ProductDAO();
        UserDAO userDAO = new UserDAO();
        InventoryDAO inventoryDAO = new InventoryDAO();
        String runId = Long.toString(System.currentTimeMillis());

        System.out.println("Creating product...");
        Product product = new Product(0, "LAPTOP-" + runId, "Laptop " + runId, "Concurrency demonstration product", new BigDecimal("999.99"));
        long productId = productDAO.create(product);
        product.setId(productId);
        System.out.println("Product created: " + product.getName());

        System.out.println("\nAdding inventory...");
        inventoryDAO.create(productId, 10);
        System.out.println("Initial stock: 10");

        List<Long> userIds = new ArrayList<>();
        System.out.println("\nCreating users...");
        for (int i = 1; i <= 10; i++) {
            userIds.add(userDAO.create(new User(0, "Demo User " + i, "demo-" + runId + "-" + i + "@example.test")));
        }
        System.out.println("Users created.");

        System.out.println("\nStarting concurrent reservation test...");
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successful = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        ReservationService reservationService = new ReservationService();
        List<Future<Void>> futures = new ArrayList<>();
        for (final long userId : userIds) {
            futures.add(executor.submit(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    ready.countDown();
                    start.await();
                    try {
                        reservationService.reserveStock(userId, productId, 2);
                        successful.incrementAndGet();
                    } catch (IllegalArgumentException exception) {
                        if (!"Insufficient inventory".equals(exception.getMessage())) throw exception;
                        failed.incrementAndGet();
                    }
                    return null;
                }
            }));
        }
        ready.await();
        start.countDown();
        try {
            for (Future<Void> future : futures) future.get();
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new SQLException("Concurrent reservation task failed", exception.getCause());
        } finally {
            executor.shutdown();
        }

        Inventory finalInventory = inventoryDAO.findByProductId(productId);
        int remaining = finalInventory.getAvailableQty();
        System.out.println("Successful reservations: " + successful.get());
        System.out.println("Failed reservations: " + failed.get());
        System.out.println("Remaining inventory: " + remaining);
        System.out.println("Reserved inventory: " + finalInventory.getReservedQty());
        if (remaining >= 0 && successful.get() * 2 == finalInventory.getReservedQty()
                && remaining + finalInventory.getReservedQty() == 10) {
            System.out.println("No overselling occurred.");
        }

        System.out.println("\nCreating a sample order...");
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(new OrderItem(0, 0, productId, 1, product.getPrice()));
        System.out.println("Order created with ID: " + new OrderService().createOrder(userIds.get(0), orderItems).getId());
        System.out.println("\n========================================");
    }
}
