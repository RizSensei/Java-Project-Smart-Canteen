package server;

import db.MenuDAO;
import db.OrderDAO;
import db.UserDAO;
import model.MenuItem;
import model.Order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CanteenServer {

    public static final int PORT = 6000;

    // Thread-safe list of connected staff clients (for broadcast)
    private static final List<PrintWriter> staffClients = new CopyOnWriteArrayList<>();
    private static final List<PrintWriter> studentClients = new CopyOnWriteArrayList<>();

    private static final java.util.Map<PrintWriter, String> studentRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("🍔 Canteen Server started on port " + PORT);
        System.out.println("   Waiting for clients...\n");

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("▶ New connection from " + client.getInetAddress());

            // Each client handled in its own thread
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("← " + line);
                String response = processMessage(line, out);
                if (response != null) {
                    out.println(response);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            // Remove this client from staff list if it was a staff client
            // (we can't know which PrintWriter belongs to which socket easily,
            // so we clean up lazily — see broadcast below)
        }
    }

    private static String processMessage(String msg, PrintWriter out) {
        // Format: COMMAND:arg1:arg2...
        String[] parts = msg.split(":", 4);
        String cmd = parts[0];

        switch (cmd) {
            case "REGISTER_STAFF":
                staffClients.add(out);
                System.out.println("  → staff registered (" + staffClients.size() + " total)");
                return "OK:REGISTERED";

            case "REGISTER_STUDENT":
                // Format: REGISTER_STUDENT:Name
                if (parts.length < 2)
                    return "ERROR:missing name";
                String studentName = parts[1];
                studentClients.add(out);
                studentRegistry.put(out, studentName); // see note below
                System.out.println("  → student registered: " + studentName +
                        " (" + studentClients.size() + " students)");
                return "OK:STUDENT_REGISTERED";

            case "LOGIN":
                if (parts.length < 3)
                    return "ERROR:missing name or password";
                String loginName = parts[1].trim();
                String loginPass;
                try {
                    loginPass = parts.length >= 4 && "B64".equals(parts[2])
                            ? new String(Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8)
                            : parts[2];
                } catch (IllegalArgumentException e) {
                    return "ERROR:invalid login request";
                }

                if (UserDAO.validate(loginName, loginPass)) {
                    try {
                        UserDAO.recordSuccessfulLogin(loginName);
                    } catch (SQLException e) {
                        System.out.println("Could not record successful login for "
                                + loginName + ": " + e.getMessage());
                    }
                    return "LOGIN_OK:" + loginName;
                }
                return "LOGIN_FAIL";

            case "CREATE_STUDENT":
                if (parts.length < 4)
                    return "ERROR:username, Gmail address, and password are required";
                String newUsername = parts[1].trim();
                if (!UserDAO.isValidUsername(newUsername))
                    return "ERROR:Username must be 3-30 characters using letters, numbers, dot, underscore, or hyphen.";
                String newEmail;
                try {
                    newEmail = UserDAO.normalizeGmail(
                            new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException e) {
                    return "ERROR:Enter a valid Gmail address.";
                }
                if (newEmail == null)
                    return "ERROR:Enter a valid Gmail address.";
                try {
                    char[] newPassword = new String(
                            Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8).toCharArray();
                    if (!UserDAO.isValidPassword(newPassword)) {
                        java.util.Arrays.fill(newPassword, '\0');
                        return "ERROR:Password must be 8-128 characters and include a letter and a number.";
                    }
                    return UserDAO.createAccount(newUsername, newEmail, newPassword)
                            ? "ACCOUNT_CREATED"
                            : "ACCOUNT_EXISTS";
                } catch (IllegalArgumentException e) {
                    return "ERROR:invalid account request";
                } catch (IllegalStateException e) {
                    System.out.println("Could not hash student password: " + e.getMessage());
                    return "ERROR:Could not securely create the student account.";
                } catch (SQLException e) {
                    System.out.println("Student account creation failed: " + e.getMessage());
                    return "ERROR:Could not save the student account. Check the users table schema.";
                }

            case "GET_MENU":
                try {
                    return "MENU:" + serializeMenu();
                } catch (SQLException e) {
                    System.out.println("Could not load menu: " + e.getMessage());
                    return "ERROR:Could not load the menu from the database.";
                }

            case "ADD_ITEM": {
                if (!staffClients.contains(out))
                    return "ERROR:only kitchen staff can manage menu items";
                if (parts.length < 3)
                    return "ERROR:missing item name or price";
                try {
                    String itemName = new String(
                            Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    BigDecimal price = new BigDecimal(parts[2]);
                    if (!MenuDAO.addItem(itemName, price))
                        return "ITEM_EXISTS";
                    broadcastToStudents("MENU_UPDATED");
                    return "ITEM_ADDED";
                } catch (IllegalArgumentException e) {
                    return "ERROR:Enter a valid item name and positive price.";
                } catch (SQLException e) {
                    System.out.println("Could not add menu item: " + e.getMessage());
                    return "ERROR:Could not save the menu item. Check the menu table schema.";
                }
            }

            case "REMOVE_ITEM": {
                if (!staffClients.contains(out))
                    return "ERROR:only kitchen staff can manage menu items";
                if (parts.length < 2)
                    return "ERROR:missing menu item id";
                try {
                    int itemId = Integer.parseInt(parts[1]);
                    if (itemId <= 0)
                        return "ERROR:invalid menu item id";
                    if (!MenuDAO.removeItem(itemId))
                        return "ERROR:menu item not found";
                    broadcastToStudents("MENU_UPDATED");
                    return "ITEM_REMOVED";
                } catch (NumberFormatException e) {
                    return "ERROR:invalid menu item id";
                } catch (SQLException e) {
                    System.out.println("Could not remove menu item: " + e.getMessage());
                    return "ERROR:Could not delete the menu item from the database.";
                }
            }

            case "ORDER": {
                if (parts.length < 4)
                    return "ERROR:bad order format";
                String name = parts[1];
                String items = parts[2];
                // We ignore the client-supplied total and recompute server-side
                int orderId = OrderDAO.placeOrMergeOrder(name, items, 0);

                if (orderId > 0) {
                    // Fetch the current state to broadcast accurate items + total
                    Order o = OrderDAO.getOrderById(orderId);
                    if (o != null) {
                        String safeItems = o.getItems().replace(":", ";");
                        broadcastToStaff("NEW_ORDER:" + orderId + ":" + name + ":" +
                                safeItems + ":" + String.format("%.2f", o.getTotal()));
                    }
                    return "ORDER_OK:" + orderId;
                }
                return "ERROR:failed to save order";
            }

            case "GET_ORDERS":
                return "ORDERS:" + serializeOrders();

            case "BILL_GENERATED": {
                if (!staffClients.contains(out))
                    return "ERROR:only staff can send bill notifications";
                if (parts.length < 3)
                    return "ERROR:missing order id or bill path";

                int billOrderId = Integer.parseInt(parts[1]);
                Order billOrder = OrderDAO.getOrderById(billOrderId);
                if (billOrder == null || !"PAID".equalsIgnoreCase(billOrder.getStatus()))
                    return "ERROR:bill notifications require a paid order";

                String notification = "BILL_GENERATED:" + billOrderId + ":" + parts[2];
                boolean delivered = false;
                for (java.util.Map.Entry<PrintWriter, String> student : studentRegistry.entrySet()) {
                    if (billOrder.getStudentName().equals(student.getValue())) {
                        student.getKey().println(notification);
                        delivered = true;
                    }
                }
                return delivered ? "OK:BILL_NOTIFICATION_SENT:" + billOrderId
                        : "ERROR:student is not connected";
            }

            case "MARK_READY": {
                if (parts.length < 2)
                    return "ERROR:missing order id";
                int id = Integer.parseInt(parts[1]);
                boolean ok = OrderDAO.markReady(id);
                if (ok) {
                    String update = "ORDER_UPDATED:" + id + ":READY";
                    broadcastToStaff(update);
                    broadcastToStudents(update);
                    return "OK:READY";
                }
                return "ERROR:order not pending";
            }

            case "MARK_PAID": {
                if (parts.length < 2)
                    return "ERROR:missing order id";
                int paidId = Integer.parseInt(parts[1]);
                boolean paid = OrderDAO.markPaid(paidId);
                if (paid) {
                    String update = "ORDER_UPDATED:" + paidId + ":PAID";
                    broadcastToStaff(update);
                    broadcastToStudents(update);
                    return "OK:PAID";
                }
                return "ERROR:order not ready or not found";
            }

            case "CANCEL_ORDER": {
                if (parts.length < 2)
                    return "ERROR:missing order id";
                int cancelId = Integer.parseInt(parts[1]);
                boolean ok = OrderDAO.cancelOrder(cancelId);
                if (ok) {
                    String update = "ORDER_UPDATED:" + cancelId + ":CANCELLED";
                    broadcastToStaff(update);
                    broadcastToStudents(update);
                    return "OK:CANCELLED";
                }
                return "ERROR:order not pending";
            }

            case "GET_MY_ORDERS":
                if (parts.length < 2)
                    return "ERROR:missing name";
                String queryName = parts[1];
                return "MY_ORDERS:" + serializeOrdersFor(queryName);

            case "QUIT":
                return "BYE";

            default:
                return "ERROR:unknown command";
        }
    }

    private static String serializeMenu() throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (MenuItem m : MenuDAO.getAllItems()) {
            sb.append(m.getId()).append("~")
                    .append(m.getName()).append("~")
                    .append(m.getPrice()).append(";");
        }
        return sb.toString();
    }

    private static String serializeOrders() {
        StringBuilder sb = new StringBuilder();
        for (Order o : OrderDAO.getAllOrders()) {
            sb.append(o.getId()).append("~")
                    .append(o.getStudentName()).append("~")
                    .append(o.getItems().replace("~", "-").replace(";", ","))
                    .append("~")
                    .append(o.getTotal()).append("~")
                    .append(o.getStatus()).append(";");
        }
        return sb.toString();
    }

    private static String serializeOrdersFor(String studentName) {
        StringBuilder sb = new StringBuilder();
        for (Order o : OrderDAO.getOrdersForStudent(studentName)) {
            sb.append(o.getId()).append("~")
                    .append(o.getStudentName()).append("~")
                    .append(o.getItems().replace("~", "-").replace(";", ","))
                    .append("~")
                    .append(o.getTotal()).append("~")
                    .append(o.getStatus()).append(";");
        }
        return sb.toString();
    }

    private static void broadcastToStaff(String message) {
        System.out.println("  → staff broadcast: " + message);
        for (PrintWriter w : staffClients) {
            w.println(message);
        }
    }

    private static void broadcastToStudents(String message) {
        System.out.println("  → student broadcast: " + message);
        for (PrintWriter w : studentClients) {
            w.println(message);
        }
    }
}