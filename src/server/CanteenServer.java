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
                if (parts.length < 2)
                    return "ERROR:missing name";
                String loginName = parts[1].trim();
                if (UserDAO.exists(loginName)) {
                    return "LOGIN_OK:" + loginName;
                }
                return "LOGIN_FAIL";

            case "GET_MENU":
                return "MENU:" + serializeMenu();

            case "ORDER":
                if (parts.length < 4)
                    return "ERROR:bad order format";
                String name = parts[1];
                String items = parts[2];
                double total = Double.parseDouble(parts[3]);

                int orderId = OrderDAO.placeOrder(name, items, total);
                if (orderId > 0) {
                    String safeItems = items.replace(":", ";");
                    broadcastToStaff("NEW_ORDER:" + orderId + ":" + name + ":" +
                            safeItems + ":" + String.format("%.2f", total));
                    return "ORDER_OK:" + orderId;
                }
                return "ERROR:failed to save order";

            case "GET_ORDERS":
                return "ORDERS:" + serializeOrders();

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

    private static String serializeMenu() {
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