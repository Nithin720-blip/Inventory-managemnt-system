package com.inventory.gui;

import com.inventory.dao.ProductDAO;
import com.inventory.model.Order;
import com.inventory.model.OrderItem;
import com.inventory.model.Product;
import com.inventory.service.OrderService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class OrderHistoryPanel extends JPanel {

    private final MainFrame mainFrame;
    private final OrderService orderService = new OrderService();
    private final ProductDAO productDAO = new ProductDAO();

    private JTable ordersTable;
    private DefaultTableModel ordersTableModel;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;

    public OrderHistoryPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
        refresh();
    }

    private void initComponents() {
        // Top Header
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Order History & Sales Records");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton refreshBtn = new JButton("🔄 Refresh Orders");
        refreshBtn.addActionListener(e -> refresh());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Split Pane (Orders on Top, Order Items on Bottom)
        String[] orderColumns = {"Order ID", "User ID", "Total Amount ($)", "Status", "Order Date"};
        ordersTableModel = new DefaultTableModel(orderColumns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(ordersTableModel);
        ordersTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ordersTable.setRowHeight(24);
        ordersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedOrderItems();
            }
        });

        String[] itemColumns = {"Item ID", "Product Name", "Quantity", "Unit Price ($)", "Subtotal ($)"};
        itemsTableModel = new DefaultTableModel(itemColumns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemsTable.setRowHeight(24);
        itemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane ordersScroll = new JScrollPane(ordersTable);
        ordersScroll.setBorder(BorderFactory.createTitledBorder("Orders List (Click an order to view items)"));

        JScrollPane itemsScroll = new JScrollPane(itemsTable);
        itemsScroll.setBorder(BorderFactory.createTitledBorder("Order Line Items Details"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersScroll, itemsScroll);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
    }

    public void refresh() {
        mainFrame.setStatus("Loading order history...");
        SwingWorker<List<Order>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Order> doInBackground() throws Exception {
                return orderService.getAllOrders();
            }

            @Override
            protected void done() {
                try {
                    List<Order> orders = get();
                    ordersTableModel.setRowCount(0);
                    itemsTableModel.setRowCount(0);
                    for (Order o : orders) {
                        ordersTableModel.addRow(new Object[]{
                                o.getId(), o.getUserId(),
                                o.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                                o.getStatus().name(), o.getCreatedAt()
                        });
                    }
                    mainFrame.setStatus("Orders history updated (" + orders.size() + " orders).");
                } catch (Exception e) {
                    mainFrame.setStatus("Error loading order history: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadSelectedOrderItems() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) return;

        long orderId = (long) ordersTableModel.getValueAt(row, 0);
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<OrderItem> items = orderService.getOrderItems(orderId);
                List<Object[]> rows = new java.util.ArrayList<>();
                for (OrderItem item : items) {
                    Product p = productDAO.findById(item.getProductId());
                    String name = p != null ? p.getName() + " (" + p.getSku() + ")" : "Product #" + item.getProductId();
                    BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    rows.add(new Object[]{
                            item.getId(), name, item.getQuantity(),
                            item.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                            subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toString()
                    });
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    itemsTableModel.setRowCount(0);
                    for (Object[] r : rows) itemsTableModel.addRow(r);
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
