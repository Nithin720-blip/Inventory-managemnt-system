package com.inventory;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.ReservationDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.Product;
import com.inventory.model.Reservation;
import com.inventory.model.User;
import com.inventory.service.ReservationService;
import com.inventory.util.DBConnection;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** MySQL integration tests. They run when DB_USERNAME and DB_PASSWORD are configured. */
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
public class InventoryIntegrationTest {
    @Test public void productCrudAndReservationRules() throws Exception {
        ProductDAO products = new ProductDAO(); UserDAO users = new UserDAO(); InventoryDAO inventory = new InventoryDAO();
        String suffix = Long.toString(System.nanoTime());
        long productId = 0, userId = 0;
        try {
            productId = products.create(new Product(0, "TEST-" + suffix, "Test product " + suffix, "Integration test", new BigDecimal("12.50")));
            Product found = products.findById(productId);
            assertEquals("Test product " + suffix, found.getName());
            found.setPrice(new BigDecimal("13.75"));
            assertTrue(products.update(found));
            assertEquals(0, products.findById(productId).getPrice().compareTo(new BigDecimal("13.75")));
            boolean listed = false;
            for (Product candidate : products.findAll()) if (candidate.getId() == productId) listed = true;
            assertTrue(listed);

            userId = users.create(new User(0, "Test user", "test-" + suffix + "@example.test"));
            inventory.create(productId, 5);
            ReservationService service = new ReservationService();
            Reservation reservation = service.reserveStock(userId, productId, 3);
            assertNotNull(new ReservationDAO().findById(reservation.getId()));
            assertEquals(2, inventory.getQuantity(productId));
            assertReservationRejected(service, userId, productId, 3);
            assertReservationRejected(service, userId, productId, 0);
            assertEquals(2, inventory.getQuantity(productId));
            assertReservationRejected(service, Long.MAX_VALUE, productId, 1);
            assertEquals(2, inventory.getQuantity(productId));
        } finally {
            cleanup(productId, userId);
        }
        assertTrue(products.delete(productId));
        assertNull(products.findById(productId));
    }

    @Test public void simultaneousReservationsNeverOversell() throws Exception {
        ProductDAO products = new ProductDAO(); UserDAO users = new UserDAO(); InventoryDAO inventory = new InventoryDAO();
        String suffix = Long.toString(System.nanoTime());
        long productId = 0; List<Long> userIds = new ArrayList<>();
        try {
            productId = products.create(new Product(0, "CONCURRENT-" + suffix, "Concurrent test " + suffix, "Integration test", new BigDecimal("1.00")));
            final long concurrentProductId = productId;
            inventory.create(productId, 10);
            for (int i=0;i<10;i++) userIds.add(users.create(new User(0,"Concurrent user", "concurrent-"+suffix+"-"+i+"@example.test")));
            ExecutorService pool=Executors.newFixedThreadPool(10); CountDownLatch gate=new CountDownLatch(1);
            AtomicInteger successes=new AtomicInteger(); AtomicInteger failures=new AtomicInteger(); List<Future<?>> futures=new ArrayList<>();
            for(final long userId:userIds) futures.add(pool.submit(new Runnable(){
                @Override public void run(){
                    try { gate.await(); new ReservationService().reserveStock(userId,concurrentProductId,2); successes.incrementAndGet(); }
                    catch(IllegalArgumentException e){ if("Insufficient inventory".equals(e.getMessage())) failures.incrementAndGet(); else throw e; }
                    catch(Exception e){ throw new RuntimeException(e); }
                }
            }));
            gate.countDown();
            for(Future<?> future:futures) future.get();
            pool.shutdown();
            assertEquals(5,successes.get()); assertEquals(5,failures.get()); assertEquals(0,inventory.getQuantity(productId));
        } finally { cleanup(productId,userIds); }
        assertTrue(products.delete(productId));
    }

    private void assertReservationRejected(ReservationService service, long userId, long productId, int quantity) {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() throws Throwable { service.reserveStock(userId, productId, quantity); }
        });
    }

    private void cleanup(long productId, long userId) throws Exception { cleanup(productId, userId == 0 ? List.of() : List.of(userId)); }
    private void cleanup(long productId, List<Long> userIds) throws Exception {
        if (productId == 0) return;
        try (Connection c=DBConnection.getConnection()) {
            try (PreparedStatement s=c.prepareStatement("DELETE FROM reservations WHERE product_id=?")) { s.setLong(1,productId); s.executeUpdate(); }
            try (PreparedStatement s=c.prepareStatement("DELETE FROM order_items WHERE product_id=?")) { s.setLong(1,productId); s.executeUpdate(); }
            for(long userId:userIds) {
                try(PreparedStatement s=c.prepareStatement("DELETE oi FROM order_items oi JOIN orders o ON oi.order_id=o.id WHERE o.user_id=?")){s.setLong(1,userId);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("DELETE FROM orders WHERE user_id=?")){s.setLong(1,userId);s.executeUpdate();}
            }
            try (PreparedStatement s=c.prepareStatement("DELETE FROM inventory WHERE product_id=?")) { s.setLong(1,productId); s.executeUpdate(); }
            for(long userId:userIds) try(PreparedStatement s=c.prepareStatement("DELETE FROM users WHERE id=?")){s.setLong(1,userId);s.executeUpdate();}
        }
    }
}
