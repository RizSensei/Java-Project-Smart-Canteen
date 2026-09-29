package db;

import model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // Insert a new order, return its generated ID (or -1 on failure)
    public static int placeOrder(String studentName, String items, double total) {
        String sql = "INSERT INTO orders (student_name, total, status) VALUES (?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, studentName);
            ps.setDouble(2, total);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("placeOrder error: " + e.getMessage());
        }
        return -1;
    }

    // Fetch all orders (for staff dashboard)
    public static List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, student_name, total, status FROM orders ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(new Order(
                    rs.getInt("id"),
                    rs.getString("student_name"),
                    "",  // items placeholder — we'll improve later
                    rs.getDouble("total"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("getAllOrders error: " + e.getMessage());
        }
        return orders;
    }

    // Update status to READY
    public static boolean markReady(int orderId) {
        String sql = "UPDATE orders SET status = 'READY' WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("markReady error: " + e.getMessage());
        }
        return false;
    }

    // Test
    public static void main(String[] args) {
        int id = placeOrder("Ram", "Burger x2, Coke x1", 350.00);
        System.out.println("Inserted order ID: " + id);

        System.out.println("\nAll orders:");
        for (Order o : getAllOrders()) {
            System.out.println(o);
        }

        System.out.println("\nMarking order #" + id + " as READY...");
        markReady(id);

        System.out.println("\nAfter update:");
        for (Order o : getAllOrders()) {
            System.out.println(o);
        }
    }
}