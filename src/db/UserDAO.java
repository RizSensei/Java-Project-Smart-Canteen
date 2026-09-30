package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /** Returns true if a user with this exact name exists. */
    public static boolean exists(String name) {
        String sql = "SELECT id FROM users WHERE name = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("UserDAO.exists error: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Rijan  → " + exists("Rijan"));
        System.out.println("Shrena → " + exists("Shrena"));
        System.out.println("Bob  → " + exists("Bob"));
    }
}