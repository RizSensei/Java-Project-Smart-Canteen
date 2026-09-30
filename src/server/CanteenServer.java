package server;

import db.MenuDAO;
import db.OrderDAO;
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
                    // Escape ':' inside items so the broadcast parser stays happy
                    String safeItems = items.replace(":", ";");
                    broadcast("NEW_ORDER:" + orderId + ":" + name + ":" +
                            safeItems + ":" + String.format("%.2f", total));
                    return "ORDER_OK:" + orderId;
                }
                return "ERROR:failed to save order";

            case "GET_ORDERS":
                return "ORDERS:" + serializeOrders();

            case "MARK_READY":
                if (parts.length < 2)
                    return "ERROR:missing order id";
                int id = Integer.parseInt(parts[1]);
                boolean ok = OrderDAO.markReady(id);
                if (ok) {
                    broadcast("ORDER_UPDATED:" + id + ":READY");
                    return "OK:READY";
                }
                return "ERROR:update failed";

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

    private static void broadcast(String message) {
        System.out.println("  → broadcasting: " + message);
        for (PrintWriter w : staffClients) {
            w.println(message);
        }
    }
}