package db;

import model.MenuItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {

    // Fetch all available menu items
    public static List<MenuItem> getAllItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT id, item_name, price FROM menu WHERE available = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(new MenuItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getDouble("price")
                ));
            }
        } catch (SQLException e) {
            System.out.println("MenuDAO error: " + e.getMessage());
        }
        return items;
    }

    // Test method
    public static void main(String[] args) {
        List<MenuItem> items = getAllItems();
        for (MenuItem item : items) {
            System.out.println(item);
        }
    }
}