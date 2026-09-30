package com.inventory.gui;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.OrderDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.ReservationDAO;
import com.inventory.model.Inventory;
import com.inventory.model.Order;
import com.inventory.model.Product;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;

    private JLabel totalProductsVal;
    private JLabel totalAvailableVal;
    private JLabel totalReservedVal;
    private JLabel lowStockVal;
    private JLabel activeReservationsVal;
    private JLabel totalRevenueVal;

    private final ProductDAO productDAO = new ProductDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initComponents();
        refresh();
    }

    private void initComponents() {
        // Title & Actions Bar
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("System Executive Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refresh());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Metrics Grid (2x3)
        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 15, 15));

        totalProductsVal = new JLabel("0", SwingConstants.CENTER);
        totalAvailableVal = new JLabel("0", SwingConstants.CENTER);
        totalReservedVal = new JLabel("0", SwingConstants.CENTER);
        lowStockVal = new JLabel("0", SwingConstants.CENTER);
        activeReservationsVal = new JLabel("0", SwingConstants.CENTER);
        totalRevenueVal = new JLabel("$0.00", SwingConstants.CENTER);

        gridPanel.add(createCard("Total Catalog Products", totalProductsVal, new Color(41, 128, 185)));
        gridPanel.add(createCard("Available Stock", totalAvailableVal, new Color(39, 174, 96)));
        gridPanel.add(createCard("Reserved Stock", totalReservedVal, new Color(230, 126, 34)));
        gridPanel.add(createCard("Low Stock Alerts (< 5)", lowStockVal, new Color(192, 57, 43)));
        gridPanel.add(createCard("Active Reservations", activeReservationsVal, new Color(142, 68, 173)));
        gridPanel.add(createCard("Confirmed Revenue", totalRevenueVal, new Color(44, 62, 80)));

        add(gridPanel, BorderLayout.CENTER);
    }

    private JPanel createCard(String titleText, JLabel valueLabel, Color headerColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(titleText, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(headerColor);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(new Color(44, 62, 80));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    public void refresh() {
        mainFrame.setStatus("Loading dashboard metrics...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int prodCount = 0;
            int totalAvailable = 0;
            int totalReserved = 0;
            int lowStockCount = 0;
            int activeResCount = 0;
            BigDecimal revenue = BigDecimal.ZERO;

            @Override
            protected Void doInBackground() throws Exception {
                List<Product> products = productDAO.findAll();
                prodCount = products.size();

                for (Product p : products) {
                    Inventory inv = inventoryDAO.findByProductId(p.getId());
                    if (inv != null) {
                        totalAvailable += inv.getAvailableQty();
                        totalReserved += inv.getReservedQty();
                        if (inv.getAvailableQty() < 5) {
                            lowStockCount++;
                        }
                    }
                }

                List<Reservation> reservations = reservationDAO.findAll();
                for (Reservation r : reservations) {
                    if (r.getStatus() == ReservationStatus.ACTIVE) {
                        activeResCount++;
                    }
                }

                List<Order> orders = orderDAO.findAll();
                for (Order o : orders) {
                    revenue = revenue.add(o.getTotalAmount());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    totalProductsVal.setText(String.valueOf(prodCount));
                    totalAvailableVal.setText(String.valueOf(totalAvailable));
                    totalReservedVal.setText(String.valueOf(totalReserved));
                    lowStockVal.setText(String.valueOf(lowStockCount));
                    activeReservationsVal.setText(String.valueOf(activeResCount));
                    totalRevenueVal.setText("$" + revenue.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                    mainFrame.setStatus("Dashboard metrics updated.");
                } catch (Exception e) {
                    mainFrame.setStatus("Error loading dashboard metrics: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
