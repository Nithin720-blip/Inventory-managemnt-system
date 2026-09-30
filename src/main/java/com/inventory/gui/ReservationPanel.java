package com.inventory.gui;

import com.inventory.dao.ProductDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.Product;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.User;
import com.inventory.service.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ReservationPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ReservationService reservationService;
    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private JComboBox<UserItem> userComboBox;
    private JComboBox<ProductItem> productComboBox;
    private JSpinner quantitySpinner;

    private JTable reservationTable;
    private DefaultTableModel tableModel;

    public ReservationPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.reservationService = new ReservationService();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
        refresh();
    }

    private void initComponents() {
        // Top Form Panel for placing reservation
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Place Stock Reservation"));

        userComboBox = new JComboBox<>();
        productComboBox = new JComboBox<>();
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        JButton addUserBtn = new JButton("👤 + User");
        addUserBtn.addActionListener(e -> showAddUserDialog());

        JButton reserveBtn = new JButton("🎟️ Reserve Stock");
        reserveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reserveBtn.setBackground(new Color(41, 128, 185));
        reserveBtn.addActionListener(e -> placeReservation());

        formPanel.add(new JLabel("User:"));
        formPanel.add(userComboBox);
        formPanel.add(addUserBtn);
        formPanel.add(new JLabel("Product:"));
        formPanel.add(productComboBox);
        formPanel.add(new JLabel("Qty:"));
        formPanel.add(quantitySpinner);
        formPanel.add(reserveBtn);

        add(formPanel, BorderLayout.NORTH);

        // Table Panel
        String[] columns = {"Res ID", "User ID", "Product", "Quantity", "Status", "Created At", "Expires At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        reservationTable = new JTable(tableModel);
        reservationTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reservationTable.setRowHeight(25);
        reservationTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Custom renderer for Status column
        reservationTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String status = value.toString();
                    switch (status) {
                        case "ACTIVE" -> {
                            c.setForeground(new Color(230, 126, 34));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        }
                        case "CONFIRMED" -> {
                            c.setForeground(new Color(39, 174, 96));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        }
                        case "CANCELLED" -> {
                            c.setForeground(new Color(192, 57, 43));
                            c.setFont(c.getFont().deriveFont(Font.PLAIN));
                        }
                        case "EXPIRED" -> {
                            c.setForeground(new Color(127, 140, 141));
                            c.setFont(c.getFont().deriveFont(Font.ITALIC));
                        }
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(reservationTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions Panel
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton confirmBtn = new JButton("✅ Confirm Reservation (Create Order)");
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        confirmBtn.addActionListener(e -> confirmSelectedReservation());

        JButton cancelBtn = new JButton("❌ Cancel / Release Stock");
        cancelBtn.addActionListener(e -> cancelSelectedReservation());

        JButton refreshBtn = new JButton("🔄 Refresh List");
        refreshBtn.addActionListener(e -> refresh());

        bottomBar.add(confirmBtn);
        bottomBar.add(cancelBtn);
        bottomBar.add(refreshBtn);

        add(bottomBar, BorderLayout.SOUTH);
    }

    public void refresh() {
        mainFrame.setStatus("Loading reservations...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<User> users;
            List<Product> products;
            List<Reservation> reservations;

            @Override
            protected Void doInBackground() throws Exception {
                users = userDAO.findAll();
                products = productDAO.findAll();
                reservations = reservationService.getAllReservations();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    userComboBox.removeAllItems();
                    for (User u : users) userComboBox.addItem(new UserItem(u));

                    productComboBox.removeAllItems();
                    for (Product p : products) productComboBox.addItem(new ProductItem(p));

                    tableModel.setRowCount(0);
                    for (Reservation r : reservations) {
                        Product p = productDAO.findById(r.getProductId());
                        String prodName = p != null ? p.getName() + " (" + p.getSku() + ")" : "Product #" + r.getProductId();
                        tableModel.addRow(new Object[]{
                                r.getId(), r.getUserId(), prodName, r.getQuantity(), r.getStatus().name(), r.getCreatedAt(),
                                r.getExpiresAt() != null ? r.getExpiresAt() : "Never"
                        });
                    }
                    mainFrame.setStatus("Reservations updated (" + reservations.size() + " records).");
                } catch (Exception e) {
                    mainFrame.setStatus("Error loading reservations: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void placeReservation() {
        UserItem selectedUser = (UserItem) userComboBox.getSelectedItem();
        ProductItem selectedProduct = (ProductItem) productComboBox.getSelectedItem();
        int quantity = (int) quantitySpinner.getValue();

        if (selectedUser == null || selectedProduct == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select both a User and a Product.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Reservation r = reservationService.reserveStock(selectedUser.user.getId(), selectedProduct.product.getId(), quantity);
            JOptionPane.showMessageDialog(mainFrame, "Stock reserved successfully! Reservation ID: " + r.getId());
            refresh();
            mainFrame.refreshAllPanels();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Reservation failed: " + e.getMessage(), "Reservation Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmSelectedReservation() {
        int row = reservationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an ACTIVE reservation to confirm.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long resId = (long) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 4);

        if (!"ACTIVE".equals(status)) {
            JOptionPane.showMessageDialog(mainFrame, "Only ACTIVE reservations can be confirmed!", "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            reservationService.confirmReservation(resId);
            JOptionPane.showMessageDialog(mainFrame, "Reservation #" + resId + " confirmed and order created!");
            refresh();
            mainFrame.refreshAllPanels();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Confirmation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedReservation() {
        int row = reservationTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please select an ACTIVE reservation to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long resId = (long) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 4);

        if (!"ACTIVE".equals(status)) {
            JOptionPane.showMessageDialog(mainFrame, "Only ACTIVE reservations can be cancelled!", "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            reservationService.cancelReservation(resId);
            JOptionPane.showMessageDialog(mainFrame, "Reservation #" + resId + " cancelled and reserved stock released.");
            refresh();
            mainFrame.refreshAllPanels();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Cancellation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddUserDialog() {
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField("user-" + System.currentTimeMillis() % 1000 + "@example.com");

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("User Name:")); panel.add(nameField);
        panel.add(new JLabel("Email Address:")); panel.add(emailField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Add New User", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                User user = new User(0, nameField.getText().trim(), emailField.getText().trim());
                long id = userDAO.create(user);
                JOptionPane.showMessageDialog(mainFrame, "User created with ID: " + id);
                refresh();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Failed to create user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ComboBox Wrappers for clean display
    private record UserItem(User user) {
        @Override public String toString() { return user.getName() + " (#" + user.getId() + ")"; }
    }
    private record ProductItem(Product product) {
        @Override public String toString() { return product.getName() + " [SKU: " + product.getSku() + "]"; }
    }
}
