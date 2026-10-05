package db;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserDAO {

    private static final String HASH_PREFIX = "PBKDF2$";
    private static final int HASH_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static volatile boolean accountSchemaReady;
    private static volatile boolean loginHistorySchemaReady;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Returns true only if a user with this exact name and password exists.
     * Legacy plaintext rows remain valid for backwards compatibility.
     */
    public static boolean validate(String name, String password) {
        String sql = "SELECT password FROM users WHERE name = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedPassword = rs.getString(1);
                if (storedPassword == null) {
                    return false;
                }
                if (storedPassword.startsWith(HASH_PREFIX)) {
                    return verifyPassword(password.toCharArray(), storedPassword);
                }
                return MessageDigest.isEqual(
                        password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("UserDAO.validate error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a new student login. Returns false if the username already exists.
     */
    public static boolean createAccount(String name, String email, char[] password) throws SQLException {
        String normalizedEmail = normalizeGmail(email);
        if (!isValidUsername(name) || normalizedEmail == null
                || !isValidPassword(password)) {
            throw new IllegalArgumentException("Invalid username, Gmail address, or password.");
        }

        try {
            ensureAccountSchema();
            String encodedPassword = hashPassword(password);
            String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
            try (Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, normalizedEmail);
                ps.setString(3, encodedPassword);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
                    return false;
                }
                throw e;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static boolean isValidUsername(String name) {
        return name != null && name.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,29}");
    }

    public static boolean isValidPassword(char[] password) {
        if (password == null || password.length < 8 || password.length > 128) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char ch : password) {
            hasLetter |= Character.isLetter(ch);
            hasDigit |= Character.isDigit(ch);
        }
        return hasLetter && hasDigit;
    }

    public static String normalizeGmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 254 || !normalized.endsWith("@gmail.com")) {
            return null;
        }
        String localPart = normalized.substring(0, normalized.length() - "@gmail.com".length());
        if (localPart.length() < 6 || localPart.length() > 30
                || !localPart.matches("[a-z0-9](?:[a-z0-9.]*[a-z0-9])?")
                || localPart.contains("..")) {
            return null;
        }
        return normalized;
    }

    public static void recordSuccessfulLogin(String username) throws SQLException {
        ensureLoginHistorySchema();
        String sql = "INSERT INTO student_login_history (username) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    private static synchronized void ensureLoginHistorySchema() throws SQLException {
        if (loginHistorySchemaReady) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS student_login_history ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                + "username VARCHAR(50) NOT NULL, "
                + "login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_student_login_history_username (username), "
                + "INDEX idx_student_login_history_time (login_time))";
        try (Connection conn = DBConnection.getConnection();
                Statement statement = conn.createStatement()) {
            statement.execute(sql);
            loginHistorySchemaReady = true;
        }
    }

    private static synchronized void ensureAccountSchema() throws SQLException {
        if (accountSchemaReady) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            int passwordColumnSize = 0;
            boolean hasEmailColumn = false;
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet columns = metadata.getColumns(conn.getCatalog(), null, "users", null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    if ("password".equalsIgnoreCase(columnName)) {
                        passwordColumnSize = columns.getInt("COLUMN_SIZE");
                    } else if ("email".equalsIgnoreCase(columnName)) {
                        hasEmailColumn = true;
                    }
                }
            }
            if (passwordColumnSize == 0) {
                throw new SQLException("The users table does not have a password column.");
            }
            if (passwordColumnSize < 100) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL");
                }
            }
            if (!hasEmailColumn) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("ALTER TABLE users ADD COLUMN email VARCHAR(254) NULL");
                }
            }
            if (!hasUniqueSingleColumnIndex(metadata, conn.getCatalog(), "users", "name")) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("CREATE UNIQUE INDEX uq_users_name ON users (name)");
                }
            }
            if (!hasUniqueSingleColumnIndex(metadata, conn.getCatalog(), "users", "email")) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("CREATE UNIQUE INDEX uq_users_email ON users (email)");
                }
            }
            accountSchemaReady = true;
        }
    }

    private static boolean hasUniqueSingleColumnIndex(
            DatabaseMetaData metadata, String catalog, String table, String column) throws SQLException {
        Map<String, Boolean> uniqueIndexes = new HashMap<>();
        Map<String, Integer> indexColumnCounts = new HashMap<>();
        Map<String, Boolean> matchingColumns = new HashMap<>();
        try (ResultSet indexes = metadata.getIndexInfo(catalog, null, table, false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String indexColumn = indexes.getString("COLUMN_NAME");
                if (indexName == null || indexColumn == null) {
                    continue;
                }
                uniqueIndexes.put(indexName, !indexes.getBoolean("NON_UNIQUE"));
                indexColumnCounts.merge(indexName, 1, Integer::sum);
                if (indexColumn.equalsIgnoreCase(column)) {
                    matchingColumns.put(indexName, true);
                }
            }
        }
        for (Map.Entry<String, Boolean> entry : uniqueIndexes.entrySet()) {
            String indexName = entry.getKey();
            if (entry.getValue()
                    && indexColumnCounts.getOrDefault(indexName, 0) == 1
                    && Boolean.TRUE.equals(matchingColumns.get(indexName))) {
                return true;
            }
        }
        return false;
    }

    private static String hashPassword(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password, salt, HASH_ITERATIONS);
        return HASH_PREFIX + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(char[] password, String encoded) {
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !"PBKDF2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 1 || iterations > 1_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = deriveKey(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash the account password.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
