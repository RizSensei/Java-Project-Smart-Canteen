package db;

import model.MenuItem;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuDAO {

    // Fetch all available menu items
    public static List<MenuItem> getAllItems() throws SQLException {
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
        }
        return items;
    }

    public static boolean addItem(String name, BigDecimal price) throws SQLException {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9 '&().-]{0,49}")
                || price == null || price.signum() <= 0
                || price.compareTo(new BigDecimal("9999.99")) > 0
                || price.scale() > 2) {
            throw new IllegalArgumentException("Invalid menu item name or price.");
        }

        ensureUniqueNameIndex();
        String sql = "INSERT INTO menu (item_name, price, available) VALUES (?, ?, TRUE)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setBigDecimal(2, price);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
                return false;
            }
            throw e;
        }
    }

    public static boolean removeItem(int id) throws SQLException {
        String sql = "DELETE FROM menu WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static volatile boolean uniqueNameIndexReady;

    private static synchronized void ensureUniqueNameIndex() throws SQLException {
        if (uniqueNameIndexReady) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            boolean hasUniqueNameIndex = false;
            boolean hasNameColumn = false;
            try (ResultSet columns = metadata.getColumns(conn.getCatalog(), null, "menu", null)) {
                while (columns.next()) {
                    if ("item_name".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        hasNameColumn = true;
                        break;
                    }
                }
            }
            if (!hasNameColumn) {
                throw new SQLException("The menu table does not have an item_name column.");
            }
            Map<String, Boolean> uniqueIndexes = new HashMap<>();
            Map<String, Integer> indexColumnCounts = new HashMap<>();
            Map<String, Boolean> matchingColumns = new HashMap<>();
            try (ResultSet indexes = metadata.getIndexInfo(
                    conn.getCatalog(), null, "menu", false, false)) {
                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    String indexColumn = indexes.getString("COLUMN_NAME");
                    if (indexName != null && indexColumn != null) {
                        uniqueIndexes.put(indexName, !indexes.getBoolean("NON_UNIQUE"));
                        indexColumnCounts.merge(indexName, 1, Integer::sum);
                        if ("item_name".equalsIgnoreCase(indexColumn)) {
                            matchingColumns.put(indexName, true);
                        }
                    }
                }
            }
            for (Map.Entry<String, Boolean> index : uniqueIndexes.entrySet()) {
                if (index.getValue()
                        && indexColumnCounts.getOrDefault(index.getKey(), 0) == 1
                        && Boolean.TRUE.equals(matchingColumns.get(index.getKey()))) {
                    hasUniqueNameIndex = true;
                    break;
                }
            }
            if (!hasUniqueNameIndex) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("CREATE UNIQUE INDEX uq_menu_item_name ON menu (item_name)");
                }
            }
            uniqueNameIndexReady = true;
        }
    }

    // Test method
    public static void main(String[] args) {
        try {
            List<MenuItem> items = getAllItems();
            for (MenuItem item : items) {
                System.out.println(item);
            }
        } catch (SQLException e) {
            System.out.println("MenuDAO error: " + e.getMessage());
        }
    }
}