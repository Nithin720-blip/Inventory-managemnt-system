package com.inventory.gui;

import com.inventory.service.ReservationExpiryWorker;
import com.inventory.service.ReservationService;
import com.inventory.util.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private JLabel statusLabel;
    private JLabel dbStatusLabel;

    private DashboardPanel dashboardPanel;
    private ProductManagementPanel productManagementPanel;
    private ReservationPanel reservationPanel;
    private OrderHistoryPanel orderHistoryPanel;
    private ConcurrencyTestPanel concurrencyTestPanel;
    private ReservationExpiryWorker expiryWorker;

    public MainFrame() {
        setTitle("Inventory Management & Reservation Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        initComponents();
        initAutoExpiryWorker();
        checkDatabaseConnection();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(33, 43, 54));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("INVENTORY RESERVATION ENGINE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Real-time Concurrency & Inventory Control System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(180, 193, 205));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        dbStatusLabel = new JLabel("Checking DB connection...");
        dbStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dbStatusLabel.setForeground(Color.YELLOW);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(dbStatusLabel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Footer Status Bar initialized before sub-panels to prevent NPE during panel creation
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        dashboardPanel = new DashboardPanel(this);
        productManagementPanel = new ProductManagementPanel(this);
        reservationPanel = new ReservationPanel(this);
        orderHistoryPanel = new OrderHistoryPanel(this);
        concurrencyTestPanel = new ConcurrencyTestPanel(this);

        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Products & Stock", productManagementPanel);
        tabbedPane.addTab("Reservations", reservationPanel);
        tabbedPane.addTab("Orders", orderHistoryPanel);
        tabbedPane.addTab("Concurrency Test", concurrencyTestPanel);

        // Add Tab change listener to auto-refresh views
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case 0 -> dashboardPanel.refresh();
                case 1 -> productManagementPanel.refresh();
                case 2 -> reservationPanel.refresh();
                case 3 -> orderHistoryPanel.refresh();
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void refreshAllPanels() {
        dashboardPanel.refresh();
        productManagementPanel.refresh();
        reservationPanel.refresh();
        orderHistoryPanel.refresh();
    }

    private void initAutoExpiryWorker() {
        expiryWorker = new ReservationExpiryWorker(new ReservationService());
        expiryWorker.setOnExpiryCallback(count -> {
            SwingUtilities.invokeLater(() -> {
                setStatus("Auto-Expiry Engine: Expired " + count + " overdue reservation(s) and restored inventory stock.");
                refreshAllPanels();
            });
        });
        expiryWorker.start(3); // Sweep every 3 seconds

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (expiryWorker != null) {
                    expiryWorker.stop();
                }
            }
        });
    }

    private void checkDatabaseConnection() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection connection = DBConnection.getConnection()) {
                    return connection != null && !connection.isClosed();
                } catch (SQLException e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (connected) {
                        dbStatusLabel.setText("● Connected to MySQL");
                        dbStatusLabel.setForeground(new Color(76, 175, 80));
                        setStatus("Database connected successfully.");
                    } else {
                        dbStatusLabel.setText("● DB Connection Failed");
                        dbStatusLabel.setForeground(new Color(244, 67, 54));
                        setStatus("Failed to connect to MySQL database. Check DB credentials!");
                    }
                } catch (Exception e) {
                    dbStatusLabel.setText("● DB Connection Error");
                    dbStatusLabel.setForeground(new Color(244, 67, 54));
                }
            }
        };
        worker.execute();
    }
}
