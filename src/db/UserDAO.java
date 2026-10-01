package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /**
     * Returns true only if a user with this exact name AND password exists.
     */
    public static boolean validate(String name, String password) {
        String sql = "SELECT id FROM users " +
                "WHERE name = ? AND password = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("UserDAO.validate error: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Rijan  → " + validate("Rijan 001", "admin"));
        System.out.println("Shrena → " + validate("Shrena", "bebo"));
        System.out.println("Bob  → " + validate("Bob", "admin"));
    }
}