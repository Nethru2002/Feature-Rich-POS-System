package com.garmentscafeteria.db;

import com.garmentscafeteria.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:pos_database.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        // SQL statements to create tables
        String createUserTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT NOT NULL UNIQUE,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");";

        String createMenuTable = "CREATE TABLE IF NOT EXISTS menu_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "price REAL NOT NULL,"
                + "stock INTEGER NOT NULL"
                + ");";

        String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "order_date DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "total REAL NOT NULL,"
                + "FOREIGN KEY (user_id) REFERENCES users(id)"
                + ");";

        String createOrderDetailsTable = "CREATE TABLE IF NOT EXISTS order_details ("
                + "order_id INTEGER NOT NULL,"
                + "menu_item_id INTEGER NOT NULL,"
                + "quantity INTEGER NOT NULL,"
                + "subtotal REAL NOT NULL,"
                + "FOREIGN KEY (order_id) REFERENCES orders(id),"
                + "FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)"
                + ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUserTable);
            stmt.execute(createMenuTable);
            stmt.execute(createOrdersTable);
            stmt.execute(createOrderDetailsTable);

            // Add default data if tables are empty
            addDefaultData(conn);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addDefaultData(Connection conn) throws SQLException {
        // Add a default manager and cashier
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('manager', 'pass123', 'manager');");
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('cashier', 'pass123', 'cashier');");
        }
        // Add default menu items
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO menu_items (name, price, stock) VALUES ('Coffee', 2.50, 100);");
            stmt.execute("INSERT OR IGNORE INTO menu_items (name, price, stock) VALUES ('Tea', 2.00, 100);");
            stmt.execute("INSERT OR IGNORE INTO menu_items (name, price, stock) VALUES ('Sandwich', 5.50, 50);");
            stmt.execute("INSERT OR IGNORE INTO menu_items (name, price, stock) VALUES ('Salad', 6.00, 30);");
        }
    }

    public static User validateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<MenuItem> getMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE stock > 0";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new MenuItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    public static void saveOrder(Order order) {
        String insertOrderSQL = "INSERT INTO orders (user_id, total) VALUES (?, ?)";
        String insertOrderDetailsSQL = "INSERT INTO order_details (order_id, menu_item_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        String updateStockSQL = "UPDATE menu_items SET stock = stock - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into orders table
            long orderId;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, order.getCurrentUser().getId());
                pstmtOrder.setDouble(2, order.getTotal());
                pstmtOrder.executeUpdate();
                
                ResultSet generatedKeys = pstmtOrder.getGeneratedKeys();
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }

            // 2. Insert into order_details and update stock
            try (PreparedStatement pstmtDetails = conn.prepareStatement(insertOrderDetailsSQL);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSQL)) {
                
                for (OrderItem item : order.getItems()) {
                    // Add to order details
                    pstmtDetails.setLong(1, orderId);
                    pstmtDetails.setInt(2, item.getMenuItem().getId());
                    pstmtDetails.setInt(3, item.getQuantity());
                    pstmtDetails.setDouble(4, item.getSubtotal());
                    pstmtDetails.addBatch();
                    
                    // Update stock
                    pstmtStock.setInt(1, item.getQuantity());
                    pstmtStock.setInt(2, item.getMenuItem().getId());
                    pstmtStock.addBatch();
                }
                pstmtDetails.executeBatch();
                pstmtStock.executeBatch();
            }

            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getDailySalesReport() {
        StringBuilder report = new StringBuilder("--- Daily Sales Report ---\n\n");
        String sql = "SELECT strftime('%Y-%m-%d', order_date) as date, COUNT(id) as num_orders, SUM(total) as total_sales " +
                     "FROM orders " +
                     "WHERE date(order_date) = date('now') " +
                     "GROUP BY date;";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                report.append(String.format("Date: %s\n", rs.getString("date")));
                report.append(String.format("Total Orders: %d\n", rs.getInt("num_orders")));
                report.append(String.format("Total Revenue: $%.2f\n", rs.getDouble("total_sales")));
            } else {
                report.append("No sales recorded for today.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            report.append("Error generating report.");
        }
        return report.toString();
    }
}