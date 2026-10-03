package client;

import model.MenuItem;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class StudentClient extends JFrame implements net.ClientConnection.MessageListener {

    private JTable menuTable;
    private DefaultTableModel menuModel;
    private JSpinner qtySpinner;
    private JTextArea cartArea;
    private JLabel totalLabel;
    private JLabel statusLabel;
    private JButton payBtn;
    private JButton cancelBtn;
    private final String loggedInName;
    private List<MenuItem> menuItems;
    private DefaultTableModel myOrdersModel;
    private JTable myOrdersTable;
    private net.ClientConnection connection;

    private java.util.LinkedHashMap<String, Integer> cartQty = new java.util.LinkedHashMap<>();
    private java.util.LinkedHashMap<String, Double> cartPrice = new java.util.LinkedHashMap<>();
    private double cartTotal = 0.0;

    public StudentClient(String loggedInName) {
        this.loggedInName = loggedInName;
        setTitle("Smart Canteen - Student");
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);

        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadMenu();
        loadMyOrders();
        connectLiveUpdates();
        setVisible(true);
    }

    // ---------- HEADER ----------
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("🍔  Smart Canteen");
        title.setFont(UITheme.H1);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        namePanel.setOpaque(false);

        JLabel welcomeLbl = new JLabel("👤  " + loggedInName);
        welcomeLbl.setForeground(Color.WHITE);
        welcomeLbl.setFont(UITheme.BODY_B);

        JButton signOutBtn = new JButton("Sign Out");
        signOutBtn.setFont(UITheme.BODY_B);
        signOutBtn.setForeground(UITheme.PRIMARY_DARK);
        signOutBtn.setBackground(Color.WHITE);
        signOutBtn.setFocusPainted(false);
        signOutBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        signOutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signOutBtn.setOpaque(true);
        signOutBtn.addActionListener(e -> signOut());

        namePanel.add(welcomeLbl);
        namePanel.add(signOutBtn);
        header.add(namePanel, BorderLayout.EAST);

        return header;
    }

    // ---------- CENTER ----------
    private JSplitPane buildCenter() {
        // Menu table
        menuModel = new DefaultTableModel(new String[] { "ID", "Item", "Price (Rs.)" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        menuTable = new JTable(menuModel);
        styleTable(menuTable);

        // Right-align price column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        menuTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        JScrollPane menuScroll = new JScrollPane(menuTable);
        menuScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                "Menu", 0, 0, UITheme.BODY_B, UITheme.TEXT));

        // Cart panel
        JPanel cartPanel = new JPanel(new BorderLayout(5, 5));
        cartPanel.setBackground(UITheme.BG);
        cartPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        cartArea = new JTextArea();
        cartArea.setEditable(false);
        cartArea.setFont(UITheme.MONO);
        cartArea.setBackground(UITheme.CARD);
        cartArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane cartScroll = new JScrollPane(cartArea);
        cartScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                "Your Cart", 0, 0, UITheme.BODY_B, UITheme.TEXT));

        totalLabel = new JLabel("Total: Rs. 0.00");
        totalLabel.setFont(UITheme.H2);
        totalLabel.setForeground(UITheme.PRIMARY_DARK);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        cartPanel.add(cartScroll, BorderLayout.CENTER);
        cartPanel.add(totalLabel, BorderLayout.SOUTH);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuScroll, cartPanel);
        topSplit.setDividerLocation(600);
        topSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                topSplit, buildMyOrdersPanel());
        mainSplit.setDividerLocation(360);
        mainSplit.setBorder(null);
        mainSplit.setResizeWeight(0.7);

        return mainSplit;
    }

    private JPanel buildMyOrdersPanel() {
        myOrdersModel = new DefaultTableModel(
                new String[] { "ID", "Items", "Total (Rs.)", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        myOrdersTable = new JTable(myOrdersModel);
        myOrdersTable.setFont(UITheme.BODY);
        myOrdersTable.setRowHeight(28);
        myOrdersTable.getTableHeader().setFont(UITheme.BODY_B);
        myOrdersTable.getTableHeader().setBackground(UITheme.PRIMARY_DARK);
        myOrdersTable.getTableHeader().setForeground(Color.WHITE);
        myOrdersTable.setSelectionBackground(UITheme.PRIMARY);
        myOrdersTable.setSelectionForeground(Color.WHITE);
        myOrdersTable.setGridColor(UITheme.BORDER);
        myOrdersTable.setShowVerticalLines(false);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        myOrdersTable.getColumnModel().getColumn(2).setCellRenderer(right);
        myOrdersTable.getColumnModel().getColumn(3)
                .setCellRenderer(new StudentStatusRenderer());

        myOrdersTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        myOrdersTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        myOrdersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        myOrdersTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        // ---- Button bar ----
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonBar.setOpaque(false);

        JButton refreshMineBtn = new JButton("🔄 Refresh");
        refreshMineBtn.setFont(UITheme.BODY);
        refreshMineBtn.addActionListener(e -> loadMyOrders());

        payBtn = new JButton("💵 Pay");
        payBtn.setFont(UITheme.BODY_B);
        payBtn.setForeground(Color.WHITE);
        payBtn.setBackground(UITheme.ACCENT);
        payBtn.setFocusPainted(false);
        payBtn.setBorder(new EmptyBorder(6, 16, 6, 16));
        payBtn.setOpaque(true);
        payBtn.setEnabled(false); // disabled until a READY row is selected
        payBtn.addActionListener(e -> paySelectedOrder());

        cancelBtn = new JButton("❌ Cancel Order");
        cancelBtn.setFont(UITheme.BODY_B);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(UITheme.DANGER);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(new EmptyBorder(6, 16, 6, 16));
        cancelBtn.setOpaque(true);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> cancelSelectedOrder());

        buttonBar.add(refreshMineBtn);
        buttonBar.add(payBtn);
        buttonBar.add(cancelBtn);

        // ---- Table + button bar in a titled wrapper ----
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);

        JScrollPane scroll = new JScrollPane(myOrdersTable);
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(buttonBar, BorderLayout.SOUTH);

        // Enable/disable Pay on selection change
        myOrdersTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            updatePayButtonState();
        });

        return wrapper;
    }

    // ---------- FOOTER ----------
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG);
        footer.setBorder(new EmptyBorder(10, 20, 15, 20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);

        JLabel qtyLbl = new JLabel("Quantity:");
        qtyLbl.setFont(UITheme.BODY);

        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        qtySpinner.setFont(UITheme.BODY);
        qtySpinner.setPreferredSize(new Dimension(60, 32));

        JButton addBtn = styledButton("➕ Add to Cart", UITheme.PRIMARY);
        JButton clearBtn = styledButton("🗑  Clear Cart", UITheme.DANGER);
        JButton placeBtn = styledButton("✅ Place Order", UITheme.ACCENT);

        addBtn.addActionListener(e -> addToCart());
        clearBtn.addActionListener(e -> clearCart());
        placeBtn.addActionListener(e -> placeOrder());

        buttons.add(qtyLbl);
        buttons.add(qtySpinner);
        buttons.add(Box.createHorizontalStrut(15));
        buttons.add(addBtn);
        buttons.add(clearBtn);
        buttons.add(placeBtn);

        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(UITheme.SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        footer.add(buttons, BorderLayout.WEST);
        footer.add(statusLabel, BorderLayout.EAST);

        return footer;
    }

    // ---------- HELPERS ----------
    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.BODY_B);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFont(UITheme.BODY);
        table.setRowHeight(28);
        table.getTableHeader().setFont(UITheme.BODY_B);
        table.getTableHeader().setBackground(UITheme.PRIMARY_DARK);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(UITheme.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(UITheme.BORDER);
        table.setShowVerticalLines(false);
    }

    // ---------- LOGIC ----------
    private void loadMenu() {
        try {
            String response = ServerConnection.send("GET_MENU"); // "MENU:id~name~price;..."
            menuItems = parseMenu(response);

            menuModel.setRowCount(0);
            for (MenuItem item : menuItems) {
                menuModel.addRow(new Object[] {
                        item.getId(), item.getName(),
                        String.format("%.2f", item.getPrice())
                });
            }
            statusLabel.setText("Loaded " + menuItems.size() + " items from server.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot reach server. Is CanteenServer running?\n" + ex.getMessage());
            statusLabel.setText("Server offline.");
        }
    }

    private java.util.List<MenuItem> parseMenu(String response) {
        java.util.List<MenuItem> items = new java.util.ArrayList<>();
        if (response == null || !response.startsWith("MENU:"))
            return items;

        String body = response.substring(5);
        if (body.isEmpty())
            return items;

        for (String entry : body.split(";")) {
            String[] parts = entry.split("~");
            if (parts.length == 3) {
                items.add(new MenuItem(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        Double.parseDouble(parts[2])));
            }
        }
        return items;
    }

    private void addToCart() {
        int row = menuTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item from the menu first.");
            return;
        }

        MenuItem item = menuItems.get(row);
        int qty = (int) qtySpinner.getValue();

        String name = item.getName();

        // If item already in cart, increment; otherwise add fresh
        cartQty.merge(name, qty, Integer::sum);
        cartPrice.put(name, item.getPrice());

        refreshCart();
    }

    private void refreshCart() {
        StringBuilder sb = new StringBuilder();
        cartTotal = 0.0;

        for (String name : cartQty.keySet()) {
            int qty = cartQty.get(name);
            double price = cartPrice.get(name);
            double lineTotal = price * qty;
            cartTotal += lineTotal;

            sb.append(String.format("%-15s x%-3d  Rs. %8.2f%n",
                    name, qty, lineTotal));
        }

        cartArea.setText(sb.toString());
        totalLabel.setText(String.format("Total: Rs. %.2f", cartTotal));
    }

    private void clearCart() {
        cartQty.clear();
        cartPrice.clear();
        cartTotal = 0.0;
        refreshCart();
        statusLabel.setText("Cart cleared.");
    }

    private void placeOrder() {
        if (cartTotal == 0.0) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.");
            return;
        }

        StringBuilder itemsSb = new StringBuilder();
        for (String itemName : cartQty.keySet()) {
            int qty = cartQty.get(itemName);
            double price = cartPrice.get(itemName);
            double lineTotal = price * qty;

            if (itemsSb.length() > 0)
                itemsSb.append(", ");
            itemsSb.append(itemName)
                    .append(" x").append(qty)
                    .append(" = Rs. ").append(String.format("%.2f", lineTotal));
        }
        String items = itemsSb.toString().replace(":", " ");

        // Use the logged-in user's name — no name field anymore
        String cmd = "ORDER:" + loggedInName + ":" + items + ":" + cartTotal;

        try {
            String response = ServerConnection.send(cmd);
            if (response != null && response.startsWith("ORDER_OK:")) {
                int orderId = Integer.parseInt(response.substring(9));
                JOptionPane.showMessageDialog(this,
                        "Order placed! Order ID: " + orderId);
                clearCart();
                loadMyOrders();
                // No nameField to clear anymore
                statusLabel.setText("Order #" + orderId + " sent to kitchen.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Server rejected order: " + response);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Server not reachable. Is CanteenServer running?\n" + ex.getMessage());
        }
    }

    private void signOut() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Sign out?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION)
            return;

        new LoginScreen();
        dispose();
    }

    private void loadMyOrders() {
        if (myOrdersModel == null)
            return;
        try {
            String response = ServerConnection.send("GET_MY_ORDERS:" + loggedInName);
            myOrdersModel.setRowCount(0);

            if (response != null && response.startsWith("MY_ORDERS:")) {
                String body = response.substring(10);
                if (!body.isEmpty()) {
                    for (String entry : body.split(";")) {
                        String[] p = entry.split("~");
                        if (p.length == 5) {
                            myOrdersModel.addRow(new Object[] {
                                    Integer.parseInt(p[0]),
                                    p[2], // items
                                    String.format("%.2f", Double.parseDouble(p[3])),
                                    p[4] // status
                            });
                        }
                    }
                }
            }
        } catch (Exception ex) {
            statusLabel.setText("Could not load your orders: " + ex.getMessage());
        }

        // Reset selection + refresh Pay button state
        myOrdersTable.clearSelection();
        updatePayButtonState();
    }

    private void connectLiveUpdates() {
        try {
            connection = new net.ClientConnection(this);
            connection.connect("REGISTER_STUDENT:" + loggedInName);
            statusLabel.setText("Connected — live updates on.");
        } catch (Exception ex) {
            statusLabel.setText("Live updates offline: " + ex.getMessage());
        }
    }

    @Override
    public void onMessage(String message) {
        SwingUtilities.invokeLater(() -> handleLiveMessage(message));
    }

    private void handleLiveMessage(String message) {
        if (message == null)
            return;
        System.out.println("← student recv: " + message);

        if (message.startsWith("ORDER_UPDATED:")) {
            // ORDER_UPDATED:id:STATUS
            String[] p = message.split(":");
            if (p.length >= 3) {
                int updatedId = Integer.parseInt(p[1]);
                String newStatus = p[2];

                // Find the row in myOrdersModel and update its status
                for (int i = 0; i < myOrdersModel.getRowCount(); i++) {
                    int rowId = (int) myOrdersModel.getValueAt(i, 0);
                    if (rowId == updatedId) {
                        myOrdersModel.setValueAt(newStatus, i, 3);
                        statusLabel.setText("Order #" + updatedId + " → " + newStatus);
                        break;
                    }
                }
                updatePayButtonState();
            }
        } else if (message.startsWith("BILL_GENERATED:")) {
            showGeneratedBill(message);
        }
    }

    private void showGeneratedBill(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length != 3) {
            return;
        }

        try {
            int orderId = Integer.parseInt(parts[1]);
            String path = new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
            File bill = new File(path);
            Object[] options = { "View Bill", "Close" };
            int choice = JOptionPane.showOptionDialog(this,
                    "Your bill for Order #" + orderId
                            + " has been generated! Would you like to view it now?",
                    "Bill Generated", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                if (!bill.isFile()) {
                    JOptionPane.showMessageDialog(this,
                            "The bill file is not available on this device.\n" + bill.getAbsolutePath(),
                            "Bill Not Available", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!Desktop.isDesktopSupported()
                        || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    JOptionPane.showMessageDialog(this,
                            "This system cannot open the bill automatically.\n"
                                    + bill.getAbsolutePath(),
                            "Cannot Open Bill", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Desktop.getDesktop().open(bill.getAbsoluteFile());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open the bill.\n" + ex.getMessage(),
                    "Cannot Open Bill", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePayButtonState() {
        int row = myOrdersTable.getSelectedRow();
        boolean isReady = false;
        boolean isPending = false;

        if (row != -1) {
            String status = (String) myOrdersModel.getValueAt(row, 3);
            isReady = "READY".equals(status);
            isPending = "PENDING".equals(status);
        }

        payBtn.setEnabled(isReady);
        if (cancelBtn != null)
            cancelBtn.setEnabled(isPending);
    }

    private void paySelectedOrder() {
        int row = myOrdersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }

        int orderId = (int) myOrdersModel.getValueAt(row, 0);
        String status = (String) myOrdersModel.getValueAt(row, 3);

        if (!"READY".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only READY orders can be paid.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Pay for order #" + orderId + "?",
                "Confirm Payment", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        try {
            String response = ServerConnection.send("MARK_PAID:" + orderId);
            if (response != null && response.startsWith("OK:PAID")) {
                myOrdersModel.setValueAt("PAID", row, 3);
                statusLabel.setText("Order #" + orderId + " paid. Thank you!");
                updatePayButtonState();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not pay: " + response);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Server not reachable.\n" + ex.getMessage());
        }
    }

    private void cancelSelectedOrder() {
        int row = myOrdersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }

        int orderId = (int) myOrdersModel.getValueAt(row, 0);
        String status = (String) myOrdersModel.getValueAt(row, 3);

        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only PENDING orders can be cancelled.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel order #" + orderId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        try {
            String response = ServerConnection.send("CANCEL_ORDER:" + orderId);
            if (response != null && response.startsWith("OK:CANCELLED")) {
                myOrdersModel.setValueAt("CANCELLED", row, 3);
                statusLabel.setText("Order #" + orderId + " cancelled.");
                updatePayButtonState();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not cancel: " + response);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Server not reachable.\n" + ex.getMessage());
        }
    }

    @Override
    public void dispose() {
        if (connection != null)
            connection.close();
        super.dispose();
    }

    private static class StudentStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(UITheme.BODY_B);
            label.setOpaque(true);

            String status = value == null ? "" : value.toString();
            if (isSelected) {
                label.setBackground(UITheme.PRIMARY);
                label.setForeground(Color.WHITE);
            } else if ("READY".equals(status)) {
                label.setBackground(UITheme.READY_BG);
                label.setForeground(UITheme.READY_FG);
                label.setText("● READY");
            } else if ("PENDING".equals(status)) {
                label.setBackground(UITheme.PENDING_BG);
                label.setForeground(UITheme.PENDING_FG);
                label.setText("● PENDING");
            } else if ("PAID".equals(status)) {
                label.setBackground(UITheme.PAID_BG);
                label.setForeground(UITheme.PAID_FG);
                label.setText("● PAID");
            } else if ("CANCELLED".equals(status)) {
                label.setBackground(new Color(0xFFEBEE)); // light red
                label.setForeground(UITheme.DANGER); // red text
                label.setText("● CANCELLED");
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(UITheme.TEXT);
            }
            return label;
        }
    }

}