package db;

import model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // ---- Insert a new order, return its generated ID (or -1 on failure)
    public static int placeOrder(String studentName, String items, double total) {
        String sql = "INSERT INTO orders (student_name, items, total, status) " +
                "VALUES (?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, studentName);
            ps.setString(2, items);
            ps.setDouble(3, total);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);

        } catch (SQLException e) {
            System.out.println("placeOrder error: " + e.getMessage());
        }
        return -1;
    }

    // ---- Fetch all orders (for staff dashboard)
    public static List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, student_name, items, total, status " +
                "FROM orders ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(new Order(
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getString("items"),
                        rs.getDouble("total"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            System.out.println("getAllOrders error: " + e.getMessage());
        }
        return orders;
    }

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

    public static List<Order> getOrdersForStudent(String studentName) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, student_name, items, total, status " +
                "FROM orders WHERE student_name = ? ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new Order(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getString("items"),
                            rs.getDouble("total"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("getOrdersForStudent error: " + e.getMessage());
        }
        return orders;
    }

    public static boolean markPaid(int orderId) {
        String sql = "UPDATE orders SET status = 'PAID' " +
                "WHERE id = ? AND status = 'READY'";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("markPaid error: " + e.getMessage());
        }
        return false;
    }
}