package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // ⚠️ Change these to match your setup
    private static final String URL = "jdbc:mysql://localhost:3306/canteen_db";
    private static final String USER = "root";
    private static final String PASSWORD = "bebo";  // your MySQL root password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Quick test
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connected to canteen_db successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}