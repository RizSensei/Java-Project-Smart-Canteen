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

    /** Returns the most recent PENDING order for the given student, or null. */
    public static Order getPendingOrderForStudent(String studentName) {
        String sql = "SELECT id, student_name, items, total, status " +
                "FROM orders WHERE student_name = ? AND status = 'PENDING' " +
                "ORDER BY id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Order(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getString("items"),
                            rs.getDouble("total"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("getPendingOrderForStudent error: " + e.getMessage());
        }
        return null;
    }

    /** Update items and total on an existing order. */
    public static boolean updateOrderItemsAndTotal(int orderId,
            String newItems,
            double newTotal) {
        String sql = "UPDATE orders SET items = ?, total = ? " +
                "WHERE id = ? AND status = 'PENDING'";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newItems);
            ps.setDouble(2, newTotal);
            ps.setInt(3, orderId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("updateOrderItemsAndTotal error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Places an order. If the student already has a PENDING order,
     * merges the new items into it and returns the same order ID.
     * Otherwise creates a new order.
     *
     * Returns the order ID, or -1 on failure.
     */
    public static int placeOrMergeOrder(String studentName,
            String newItems,
            double newTotal) {

        // Build a price lookup once, reuse for both branches
        java.util.Map<String, Double> priceLookup = new java.util.HashMap<>();
        try {
            for (model.MenuItem m : MenuDAO.getAllItems()) {
                priceLookup.put(m.getName(), m.getPrice());
            }
        } catch (SQLException e) {
            System.out.println("Could not load menu prices for order: " + e.getMessage());
            return -1;
        }

        java.util.Map<String, Integer> newOnes = ItemMerger.parseQuantities(newItems);
        if (newOnes.isEmpty()) {
            return -1;
        }
        double newItemsTotal = 0.0;
        for (java.util.Map.Entry<String, Integer> item : newOnes.entrySet()) {
            Double price = priceLookup.get(item.getKey());
            if (price == null || item.getValue() <= 0) {
                return -1;
            }
            newItemsTotal += price * item.getValue();
        }

        Order pending = getPendingOrderForStudent(studentName);

        // ---- No existing PENDING order: insert fresh with correct total ----
        if (pending == null) {
            String cleanItems = ItemMerger.rebuild(newOnes,
                    priceLookup::get);
            return placeOrder(studentName, cleanItems, newItemsTotal);
        }

        // ---- Merge into existing PENDING order ----
        java.util.Map<String, Integer> merged = ItemMerger.parseQuantities(pending.getItems());
        for (java.util.Map.Entry<String, Integer> e : newOnes.entrySet()) {
            merged.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        java.util.Map<String, Double> mergedPrices =
                ItemMerger.parseUnitPrices(pending.getItems());
        mergedPrices.putAll(priceLookup);
        String mergedItems = ItemMerger.rebuild(merged,
                name -> mergedPrices.getOrDefault(name, 0.0));

        boolean ok = updateOrderItemsAndTotal(pending.getId(),
                mergedItems, pending.getTotal() + newItemsTotal);
        return ok ? pending.getId() : -1;
    }

    public static boolean markReady(int orderId) {
        String sql = "UPDATE orders SET status = 'READY' WHERE id = ? AND status = 'PENDING'";
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

    public static boolean cancelOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' " +
                "WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("cancelOrder error: " + e.getMessage());
        }
        return false;
    }

    public static Order getOrderById(int orderId) {
        String sql = "SELECT id, student_name, items, total, status " +
                "FROM orders WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Order(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getString("items"),
                            rs.getDouble("total"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("getOrderById error: " + e.getMessage());
        }
        return null;
    }
}