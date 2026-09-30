package com.inventory.gui;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.Inventory;
import com.inventory.model.Product;
import com.inventory.model.User;
import com.inventory.service.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTestPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ProductDAO productDAO = new ProductDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ReservationService reservationService = new ReservationService();

    private JComboBox<ProductItem> productComboBox;
    private JSpinner threadCountSpinner;
    private JSpinner qtyPerThreadSpinner;
    private JButton runTestBtn;

    private JTextArea logArea;
    private JLabel successCountVal;
    private JLabel failedCountVal;
    private JLabel finalAvailVal;
    private JLabel finalReservedVal;
    private JLabel oversellStatusVal;

    public ConcurrencyTestPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
        loadProducts();
    }

    private void initComponents() {
        // Controls Header
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        configPanel.setBorder(BorderFactory.createTitledBorder("Interactive Stress Test Parameters"));

        productComboBox = new JComboBox<>();
        threadCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        qtyPerThreadSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));

        runTestBtn = new JButton("⚡ Run Concurrency Stress Test");
        runTestBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        runTestBtn.setBackground(new Color(230, 126, 34));
        runTestBtn.setForeground(Color.BLACK);
        runTestBtn.addActionListener(e -> runStressTest());

        configPanel.add(new JLabel("Target Product:"));
        configPanel.add(productComboBox);
        configPanel.add(new JLabel("Concurrent Threads:"));
        configPanel.add(threadCountSpinner);
        configPanel.add(new JLabel("Qty Per Thread:"));
        configPanel.add(qtyPerThreadSpinner);
        configPanel.add(runTestBtn);

        add(configPanel, BorderLayout.NORTH);

        // Center Split (Live Log on Left, Metrics Results on Right)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 127));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Real-Time Execution & Transaction Lock Log"));

        JPanel resultsPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Test Results Summary"));
        resultsPanel.setPreferredSize(new Dimension(320, 0));

        successCountVal = createMetricRow(resultsPanel, "Successful Reservations:", "0", new Color(39, 174, 96));
        failedCountVal = createMetricRow(resultsPanel, "Rejected (Out of Stock):", "0", new Color(192, 57, 43));
        finalAvailVal = createMetricRow(resultsPanel, "Remaining Available Stock:", "0", Color.DARK_GRAY);
        finalReservedVal = createMetricRow(resultsPanel, "Total Reserved Stock:", "0", new Color(41, 128, 185));
        oversellStatusVal = createMetricRow(resultsPanel, "Anti-Overselling Guard:", "NOT TESTED", Color.GRAY);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, resultsPanel);
        centerSplit.setResizeWeight(0.7);

        add(centerSplit, BorderLayout.CENTER);
    }

    private JLabel createMetricRow(JPanel parent, String title, String initialVal, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel v = new JLabel(initialVal, SwingConstants.RIGHT);
        v.setFont(new Font("Segoe UI", Font.BOLD, 16));
        v.setForeground(color);
        p.add(t, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        parent.add(p);
        return v;
    }

    public void loadProducts() {
        SwingWorker<List<Product>, Void> worker = new SwingWorker<>() {
            @Override protected List<Product> doInBackground() throws Exception { return productDAO.findAll(); }
            @Override protected void done() {
                try {
                    List<Product> products = get();
                    productComboBox.removeAllItems();
                    for (Product p : products) productComboBox.addItem(new ProductItem(p));
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void runStressTest() {
        ProductItem selected = (ProductItem) productComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a product for the test.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int threads = (int) threadCountSpinner.getValue();
        int qtyPerThread = (int) qtyPerThreadSpinner.getValue();
        long productId = selected.product.getId();

        runTestBtn.setEnabled(false);
        logArea.setText("====================================================\n");
        logArea.append(" STARTING CONCURRENCY STRESS TEST\n");
        logArea.append(" Product: " + selected.product.getName() + " (ID: " + productId + ")\n");
        logArea.append(" Threads: " + threads + " | Qty requested per thread: " + qtyPerThread + "\n");
        logArea.append(" Concurrency Control: MySQL SELECT ... FOR UPDATE\n");
        logArea.append("====================================================\n\n");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            int success = 0;
            int failed = 0;
            int initialStock = 0;
            Inventory finalInventory;

            @Override
            protected Void doInBackground() throws Exception {
                Inventory inv = inventoryDAO.findByProductId(productId);
                initialStock = inv != null ? inv.getAvailableQty() : 0;
                publish("Initial available stock in DB: " + initialStock + "\n");

                // Generate dummy test users if needed
                List<Long> userIds = new ArrayList<>();
                long runId = System.currentTimeMillis();
                for (int i = 1; i <= threads; i++) {
                    userIds.add(userDAO.create(new User(0, "TestUser-" + i, "test-" + runId + "-" + i + "@stress.test")));
                }
                publish("Created " + threads + " concurrent user sessions.\nLaunching simultaneous threads...\n\n");

                ExecutorService executor = Executors.newFixedThreadPool(threads);
                CountDownLatch ready = new CountDownLatch(threads);
                CountDownLatch startSignal = new CountDownLatch(1);
                AtomicInteger successCounter = new AtomicInteger();
                AtomicInteger failedCounter = new AtomicInteger();

                List<Future<Void>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    final long userId = userIds.get(i);
                    final int threadNum = i + 1;
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        startSignal.await();
                        try {
                            reservationService.reserveStock(userId, productId, qtyPerThread);
                            successCounter.incrementAndGet();
                            publish("[SUCCESS] Thread #" + threadNum + " (User " + userId + ") reserved " + qtyPerThread + " units.\n");
                        } catch (IllegalArgumentException ex) {
                            failedCounter.incrementAndGet();
                            publish("[REJECTED] Thread #" + threadNum + " (User " + userId + ") failed: " + ex.getMessage() + "\n");
                        } catch (Exception ex) {
                            failedCounter.incrementAndGet();
                            publish("[ERROR] Thread #" + threadNum + " failed: " + ex.getMessage() + "\n");
                        }
                        return null;
                    }));
                }

                ready.await();
                long startTime = System.currentTimeMillis();
                startSignal.countDown(); // Launch all threads simultaneously!

                for (Future<Void> f : futures) f.get();
                executor.shutdown();
                long elapsed = System.currentTimeMillis() - startTime;

                success = successCounter.get();
                failed = failedCounter.get();
                finalInventory = inventoryDAO.findByProductId(productId);

                publish("\n----------------------------------------------------\n");
                publish("Stress test execution completed in " + elapsed + " ms.\n");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    logArea.append(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    successCountVal.setText(String.valueOf(success));
                    failedCountVal.setText(String.valueOf(failed));
                    if (finalInventory != null) {
                        finalAvailVal.setText(String.valueOf(finalInventory.getAvailableQty()));
                        finalReservedVal.setText(String.valueOf(finalInventory.getReservedQty()));

                        boolean oversold = finalInventory.getAvailableQty() < 0;
                        if (!oversold && (success * qtyPerThread <= initialStock)) {
                            oversellStatusVal.setText("PASSED (SAFE)");
                            oversellStatusVal.setForeground(new Color(39, 174, 96));
                        } else {
                            oversellStatusVal.setText("FAILED (OVERSOLD)");
                            oversellStatusVal.setForeground(new Color(192, 57, 43));
                        }
                    }
                    mainFrame.setStatus("Concurrency stress test completed.");
                    mainFrame.refreshAllPanels();
                } catch (Exception e) {
                    logArea.append("\n[TEST ERROR] " + e.getMessage() + "\n");
                    mainFrame.setStatus("Concurrency test error: " + e.getMessage());
                } finally {
                    runTestBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private record ProductItem(Product product) {
        @Override public String toString() { return product.getName() + " (SKU: " + product.getSku() + ")"; }
    }
}
