package com.garmentscafeteria.ui;

import com.garmentscafeteria.db.DatabaseManager;
import com.garmentscafeteria.model.Order;
import com.garmentscafeteria.model.OrderItem;
import com.garmentscafeteria.model.User;
import com.garmentscafeteria.model.MenuItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainPOSFrame extends JFrame {
    private User currentUser;
    private Order currentOrder;
    private JTable orderTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private JPanel menuPanel;

    public MainPOSFrame(User user) {
        this.currentUser = user;
        this.currentOrder = new Order(currentUser);

        setTitle("Garments Cafeteria POS - Logged in as: " + user.getUsername());
        setSize(1366, 768); // A common modern screen resolution
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15)); // Increased gaps between components
        // --- UI ENHANCEMENT --- Add padding around the entire frame
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // West: Menu Items
        createMenuPanel();
        add(new JScrollPane(menuPanel), BorderLayout.WEST);

        // Center: Current Order
        add(createOrderPanel(), BorderLayout.CENTER);
        
        // South: Controls and total
        add(createControlsPanel(), BorderLayout.SOUTH);

        populateMenuItems();
    }

    private void createMenuPanel() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(0, 3, 10, 10)); // 0 rows, 3 columns
        // --- UI ENHANCEMENT --- Add padding inside the menu panel
        menuPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Menu"),
            new EmptyBorder(10, 10, 10, 10)
        ));
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Current Order"));

        tableModel = new DefaultTableModel(new Object[]{"Item", "Qty", "Price", "Subtotal"}, 0) {
             @Override
             public boolean isCellEditable(int row, int column) {
                 // Make table cells not editable
                 return false;
             }
        };
        orderTable = new JTable(tableModel);
        // --- UI ENHANCEMENT --- Improve table look and feel
        orderTable.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        orderTable.setRowHeight(35);
        orderTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));

        panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Add top padding

        totalLabel = new JLabel("Total: $0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        // --- UI ENHANCEMENT --- Create buttons with icons
        JButton checkoutButton = createIconButton("Checkout", "icons/checkout.png");
        JButton clearButton = createIconButton("Clear Order", "icons/clear.png");
        JButton discountButton = createIconButton("Apply Discount", "icons/discount.png");
        JButton reportButton = createIconButton("View Reports", "icons/report.png");

        checkoutButton.addActionListener(e -> processCheckout());
        clearButton.addActionListener(e -> clearOrder());
        discountButton.addActionListener(e -> applyDiscount());
        reportButton.addActionListener(e -> new ReportingFrame().setVisible(true));
        
        // Only managers can see the reports button
        reportButton.setVisible("manager".equals(currentUser.getRole()));

        buttonPanel.add(discountButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(reportButton);
        buttonPanel.add(checkoutButton);

        panel.add(totalLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    // --- UI ENHANCEMENT --- Helper method to create buttons with icons
    private JButton createIconButton(String text, String iconPath) {
        JButton button = new JButton(text);
        try {
            URL iconUrl = getClass().getClassLoader().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                // Scale icon to a nice size
                Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(img));
            } else {
                System.err.println("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusable(false); // Improves look
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        return button;
    }

    private void populateMenuItems() {
        List<MenuItem> items = DatabaseManager.getMenuItems();
        for (MenuItem item : items) {
            // --- UI ENHANCEMENT --- Prettier menu buttons
            String buttonText = String.format("<html><div style='text-align: center;'><b>%s</b><br>$%.2f<br><font color=gray>Stock: %d</font></div></html>",
                    item.getName(), item.getPrice(), item.getStock());
            JButton itemButton = new JButton(buttonText);
            itemButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            itemButton.setPreferredSize(new Dimension(150, 80)); // Uniform button size
            itemButton.setFocusable(false);
            itemButton.addActionListener(e -> addItemToOrder(item));
            menuPanel.add(itemButton);
        }
    }
    
    // Unchanged methods from here...
    private void addItemToOrder(MenuItem item) {
        String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity for " + item.getName() + ":", "1");
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity > 0 && quantity <= item.getStock()) {
                currentOrder.addItem(new OrderItem(item, quantity));
                updateOrderTable();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid quantity or not enough stock.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateOrderTable() {
        tableModel.setRowCount(0);
        for (OrderItem item : currentOrder.getItems()) {
            tableModel.addRow(new Object[]{
                    item.getMenuItem().getName(),
                    item.getQuantity(),
                    String.format("$%.2f", item.getMenuItem().getPrice()),
                    String.format("$%.2f", item.getSubtotal())
            });
        }
        updateTotal();
    }
    
    private void updateTotal() {
        totalLabel.setText(String.format("Total: $%.2f", currentOrder.getTotal()));
    }

    private void applyDiscount() {
        currentOrder.setDiscountRate(0.10);
        JOptionPane.showMessageDialog(this, "10% employee discount applied!");
        updateTotal();
    }
    
    private void clearOrder() {
        currentOrder = new Order(currentUser);
        updateOrderTable();
    }

    private void processCheckout() {
        if (currentOrder.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Order is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DatabaseManager.saveOrder(currentOrder);
        generateReceiptFile(currentOrder);
        JOptionPane.showMessageDialog(this, String.format("Checkout complete! Total: $%.2f. Receipt saved.", currentOrder.getTotal()), "Success", JOptionPane.INFORMATION_MESSAGE);
        clearOrder();
        refreshMenu();
    }
    
    private void generateReceiptFile(Order order) {
        new java.io.File("receipts").mkdirs();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String filename = "receipts/receipt_" + dtf.format(LocalDateTime.now()) + ".txt";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("======= RECEIPT =======\n");
            writer.write("Garments Cafeteria Inc.\n");
            writer.write("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Cashier: " + currentUser.getUsername() + "\n");
            writer.write("-----------------------\n");
            for (OrderItem item : order.getItems()) {
                writer.write(String.format("%-15s x%d  $%.2f\n", item.getMenuItem().getName(), item.getQuantity(), item.getSubtotal()));
            }
            writer.write("-----------------------\n");
            writer.write(String.format("Subtotal: $%.2f\n", order.getSubtotal()));
            if (order.getDiscountRate() > 0) {
                 writer.write(String.format("Discount (%.0f%%): -$%.2f\n", order.getDiscountRate() * 100, order.getDiscountAmount()));
            }
            writer.write(String.format("TOTAL: $%.2f\n", order.getTotal()));
            writer.write("=======================\n");
            writer.write("Thank you!\n");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save receipt file.", "File I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshMenu() {
        menuPanel.removeAll();
        populateMenuItems();
        menuPanel.revalidate();
        menuPanel.repaint();
    }
}