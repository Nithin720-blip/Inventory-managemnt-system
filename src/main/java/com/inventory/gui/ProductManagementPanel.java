package com.inventory.gui;

import com.inventory.dao.InventoryDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.model.Inventory;
import com.inventory.model.Product;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ProductManagementPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final InventoryDAO inventoryDAO;

    private JTable productTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;

    public ProductManagementPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        ProductDAO productDAO = new ProductDAO();
        this.inventoryDAO = new InventoryDAO();
        this.productService = new ProductService(productDAO);
        this.inventoryService = new InventoryService(inventoryDAO);

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
        refresh();
    }

    private void initComponents() {
        // Top Action Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.add(new JLabel("Search Products:"));
        searchField = new JTextField(18);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.addActionListener(e -> filterTable());
        JButton filterBtn = new JButton("🔍 Search");
        filterBtn.addActionListener(e -> filterTable());
        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            filterTable();
        });

        searchPanel.add(searchField);
        searchPanel.add(filterBtn);
        searchPanel.add(resetBtn);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton addBtn = new JButton("➕ Add Product");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.addActionListener(e -> showAddProductDialog());

        JButton editBtn = new JButton("✏️ Edit Selected");
        editBtn.addActionListener(e -> showEditProductDialog());

        JButton restockBtn = new JButton("📦 Restock Inventory");
        restockBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        restockBtn.addActionListener(e -> showRestockDialog());

        JButton deleteBtn = new JButton("🗑️ Delete");
        deleteBtn.setForeground(new Color(192, 57, 43));
        deleteBtn.addActionListener(e -> deleteSelectedProduct());

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(restockBtn);
        buttonPanel.add(deleteBtn);

        topBar.add(searchPanel, BorderLayout.WEST);
        topBar.add(buttonPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Product JTable
        String[] columnNames = {"ID", "SKU", "Product Name", "Description", "Price (Rs)", "Available Qty", "Reserved Qty"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productTable.setRowHeight(25);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        sorter = new TableRowSorter<>(tableModel);
        productTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(productTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void filterTable() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 2, 3));
        }
    }

    public void refresh() {
        mainFrame.setStatus("Refreshing products inventory...");
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Product> products = productService.findAll();
                List<Object[]> rows = new java.util.ArrayList<>();
                for (Product p : products) {
                    Inventory inv = inventoryDAO.findByProductId(p.getId());
                    int avail = inv != null ? inv.getAvailableQty() : 0;
                    int res = inv != null ? inv.getReservedQty() : 0;
                    rows.add(new Object[]{
                            p.getId(), p.getSku(), p.getName(), p.getDescription(),
                            p.getPrice().setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                            avail, res
                    });
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                    mainFrame.setStatus("Products inventory updated (" + rows.size() + " items).");
                } catch (Exception e) {
                    mainFrame.setStatus("Error loading products: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showAddProductDialog() {
        JTextField nameField = new JTextField();
        JTextField skuField = new JTextField("SKU-" + System.currentTimeMillis() % 10000);
        JTextField priceField = new JTextField("99.99");
        JTextField stockField = new JTextField("20");
        JTextArea descArea = new JTextArea(3, 20);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.add(new JLabel("Product Name:")); panel.add(nameField);
        panel.add(new JLabel("SKU Code:")); panel.add(skuField);
        panel.add(new JLabel("Price ($):")); panel.add(priceField);
        panel.add(new JLabel("Initial Stock:")); panel.add(stockField);
        panel.add(new JLabel("Description:")); panel.add(new JScrollPane(descArea));

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Create New Product", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String sku = skuField.getText().trim();
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                String desc = descArea.getText().trim();

                Product product = new Product(0, sku, name, desc, price);
                long productId = productService.create(product);
                inventoryDAO.create(productId, stock);

                JOptionPane.showMessageDialog(mainFrame, "Product created successfully with ID: " + productId);
                refresh();
                mainFrame.refreshAllPanels();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Error creating product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditProductDialog() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a product to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        long productId = (long) tableModel.getValueAt(modelRow, 0);
        String sku = (String) tableModel.getValueAt(modelRow, 1);
        String name = (String) tableModel.getValueAt(modelRow, 2);
        String desc = (String) tableModel.getValueAt(modelRow, 3);
        String priceStr = (String) tableModel.getValueAt(modelRow, 4);

        JTextField nameField = new JTextField(name);
        JTextField skuField = new JTextField(sku);
        JTextField priceField = new JTextField(priceStr);
        JTextArea descArea = new JTextArea(desc, 3, 20);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Product Name:")); panel.add(nameField);
        panel.add(new JLabel("SKU Code:")); panel.add(skuField);
        panel.add(new JLabel("Price ($):")); panel.add(priceField);
        panel.add(new JLabel("Description:")); panel.add(new JScrollPane(descArea));

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Edit Product #" + productId, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Product updated = new Product(productId, skuField.getText().trim(), nameField.getText().trim(), descArea.getText().trim(), new BigDecimal(priceField.getText().trim()));
                productService.update(updated);
                JOptionPane.showMessageDialog(mainFrame, "Product updated successfully!");
                refresh();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Error updating product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showRestockDialog() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a product to restock.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        long productId = (long) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 2);
        int currentAvail = (int) tableModel.getValueAt(modelRow, 5);

        String qtyStr = JOptionPane.showInputDialog(mainFrame, "Enter additional stock quantity to add for '" + name + "' (Current Stock: " + currentAvail + "):", "Restock Inventory", JOptionPane.PLAIN_MESSAGE);
        if (qtyStr != null && !qtyStr.isBlank()) {
            try {
                int addQty = Integer.parseInt(qtyStr.trim());
                inventoryService.addStock(productId, addQty);
                JOptionPane.showMessageDialog(mainFrame, "Successfully added " + addQty + " units to stock!");
                refresh();
                mainFrame.refreshAllPanels();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Restock failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a product to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        long productId = (long) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to delete product #" + productId + " (" + name + ")?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                productService.delete(productId);
                JOptionPane.showMessageDialog(mainFrame, "Product deleted.");
                refresh();
                mainFrame.refreshAllPanels();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Cannot delete product (may have linked reservations/orders): " + e.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
