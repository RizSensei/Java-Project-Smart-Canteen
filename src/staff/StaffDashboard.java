package staff;

import model.Order;
import net.ClientConnection;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import billing.BillGenerator;

import java.awt.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StaffDashboard extends JFrame implements ClientConnection.MessageListener {

    private DefaultTableModel orderModel;
    private JTable orderTable;
    private JLabel statusLabel;
    private JLabel countLabel;
    private JLabel connLabel;

    private ClientConnection connection;
    private final List<Order> orders = new ArrayList<>();
    private final List<model.MenuItem> menuItems = new ArrayList<>();
    private DefaultListModel<model.MenuItem> menuListModel;
    private JList<model.MenuItem> menuList;
    private JDialog menuDialog;
    private JLabel menuMessageLabel;
    private JLabel itemNameErrorLabel;
    private JLabel itemPriceErrorLabel;
    private JTextField itemNameField;
    private JTextField itemPriceField;
    private JButton menuSubmitButton;
    private JButton menuCancelButton;
    private boolean menuMutationPending;
    private boolean addItemDialogOpen;

    public StaffDashboard() {
        setTitle("Smart Canteen - Staff");
        setSize(950, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setVisible(true);

        // Connect AFTER the UI is visible so messages can update the table
        connectToServer();
        refreshOrders(); // initial load
        refreshMenu();
    }

    // ---------- UI ----------
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY_DARK);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("👨‍🍳  Kitchen Dashboard");
        title.setFont(UITheme.H1);
        title.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);

        connLabel = new JLabel("● connecting...");
        connLabel.setFont(UITheme.BODY_B);
        connLabel.setForeground(Color.YELLOW);

        countLabel = new JLabel("0 active orders");
        countLabel.setFont(UITheme.BODY_B);
        countLabel.setForeground(Color.WHITE);

        right.add(connLabel);
        right.add(countLabel);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildCenter() {
        orderModel = new DefaultTableModel(
                new String[] { "Order ID", "Student", "Items", "Total (Rs.)", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        orderTable = new JTable(orderModel);
        orderTable.setFont(UITheme.BODY);
        orderTable.setRowHeight(80);
        orderTable.getTableHeader().setFont(UITheme.BODY_B);
        orderTable.getTableHeader().setBackground(UITheme.PRIMARY_DARK);
        orderTable.getTableHeader().setForeground(Color.WHITE);
        orderTable.setSelectionBackground(UITheme.PRIMARY);
        orderTable.setSelectionForeground(Color.WHITE);
        orderTable.setGridColor(UITheme.BORDER);
        orderTable.setShowVerticalLines(false);

        orderTable.getColumnModel().getColumn(2).setCellRenderer(new ItemsRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        orderTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        orderTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        orderTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Order ID
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(220); // Student
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(240); // Items
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Total
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(130); // Status

        JScrollPane scroll = new JScrollPane(orderTable);
        scroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG);
        footer.setBorder(new EmptyBorder(10, 20, 15, 20));

        JPanel orderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        orderButtons.setOpaque(false);
        JPanel menuButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        menuButtons.setOpaque(false);

        JButton refreshBtn = styledButton("🔄 Refresh", UITheme.PRIMARY_DARK);
        JButton readyBtn = styledButton("✅ Mark Ready", UITheme.PRIMARY);
        JButton billBtn = styledButton("Generate Bill", UITheme.PRIMARY_DARK);
        JButton addItemBtn = styledButton("Add Item", UITheme.PRIMARY);
        JButton removeItemBtn = styledButton("Remove Item", UITheme.DANGER);

        refreshBtn.addActionListener(e -> refreshOrders());
        addItemBtn.addActionListener(e -> showMenuManager(true));
        removeItemBtn.addActionListener(e -> showMenuManager(false));
        readyBtn.setEnabled(false);
        readyBtn.addActionListener(e -> markReady());
        billBtn.setEnabled(false);
        billBtn.addActionListener(e -> generateBill());

        // NEW: enable/disable based on selection
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            int row = orderTable.getSelectedRow();
            boolean isPending = false;
            boolean isPaid = false;
            if (row != -1) {
                int modelRow = orderTable.convertRowIndexToModel(row);
                String status = String.valueOf(orderModel.getValueAt(modelRow, 4));
                isPending = "PENDING".equals(status);
                isPaid = "PAID".equalsIgnoreCase(status.trim());
            }
            readyBtn.setEnabled(isPending);
            billBtn.setEnabled(row != -1 && isPaid);
        });

        orderButtons.add(refreshBtn);
        orderButtons.add(readyBtn);
        orderButtons.add(billBtn);
        menuButtons.add(addItemBtn);
        menuButtons.add(removeItemBtn);

        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(UITheme.SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);

        footer.add(orderButtons, BorderLayout.WEST);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(menuButtons, BorderLayout.EAST);
        return footer;
    }

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

    // ---------- SERVER ----------
    private void connectToServer() {
        try {
            connection = new ClientConnection(this);
            connection.connect("REGISTER_STAFF");
            SwingUtilities.invokeLater(() -> {
                connLabel.setText("● live");
                connLabel.setForeground(new Color(0xF8BBD0));
            });
        } catch (Exception ex) {
            connLabel.setText("● offline");
            connLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "Cannot reach server.\n" + ex.getMessage());
        }
    }

    /** Called from the socket thread — must marshal to EDT for Swing. */
    @Override
    public void onMessage(String message) {
        SwingUtilities.invokeLater(() -> handleServerMessage(message));
    }

    private void handleServerMessage(String message) {
        // Optional: log to terminal for debugging
        System.out.println("← " + message);

        if (message == null)
            return;

        if (message.startsWith("NEW_ORDER:")) {
            String[] p = message.split(":");
            if (p.length >= 5) {
                int id = Integer.parseInt(p[1]);
                String name = p[2];
                String items = p[3].replace(";", ":");
                double total = Double.parseDouble(p[4]);

                // If the order already exists in memory, update it; else add
                boolean updated = false;
                for (int i = 0; i < orders.size(); i++) {
                    if (orders.get(i).getId() == id) {
                        orders.set(i, new Order(id, name, items, total, "PENDING"));
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    orders.add(new Order(id, name, items, total, "PENDING"));
                    Toolkit.getDefaultToolkit().beep();
                    statusLabel.setText("⚡ New order #" + id + " from " + name);
                } else {
                    statusLabel.setText("✏ Order #" + id + " updated");
                }
                rebuildTable();
            }
        }

        else if (message.startsWith("ORDER_UPDATED:")) {
            // ORDER_UPDATED:id:STATUS
            String[] p = message.split(":");
            if (p.length >= 3) {
                int id = Integer.parseInt(p[1]);
                String status = p[2];
                for (Order o : orders) {
                    if (o.getId() == id) {
                        int idx = orders.indexOf(o);
                        orders.set(idx, new Order(o.getId(), o.getStudentName(),
                                o.getItems(), o.getTotal(), status));
                        break;
                    }
                }
                rebuildTable();
                statusLabel.setText("Order #" + id + " → " + status);
            }
        } else if (message.startsWith("ORDERS:")) {
            orders.clear();
            orders.addAll(parseOrders(message));
            rebuildTable();
            statusLabel.setText("Loaded " + orders.size() + " orders.");
        } else if (message.startsWith("MENU:")) {
            updateMenuItems(parseMenu(message));
        } else if ("ITEM_ADDED".equals(message)) {
            menuMutationPending = false;
            setMenuMessage("Item added successfully.", new Color(0x2E7D32));
            if (itemNameField != null) {
                itemNameField.setText("");
                itemPriceField.setText("");
                itemNameErrorLabel.setText(" ");
                itemPriceErrorLabel.setText(" ");
            }
            enableMenuActions();
            refreshMenu();
            statusLabel.setText("Menu item added.");
        } else if ("ITEM_EXISTS".equals(message)) {
            menuMutationPending = false;
            setMenuMessage("An item with that name already exists.", UITheme.DANGER);
            enableMenuActions();
        } else if ("ITEM_REMOVED".equals(message)) {
            menuMutationPending = false;
            setMenuMessage("Item removed successfully.", new Color(0x2E7D32));
            enableMenuActions();
            refreshMenu();
            statusLabel.setText("Menu item removed.");
        } else if (message.startsWith("ERROR:")) {
            String error = message.substring(6);
            statusLabel.setText("Server error: " + error);
            if (menuMutationPending || menuDialog != null && menuDialog.isVisible()) {
                menuMutationPending = false;
                setMenuMessage(error, UITheme.DANGER);
                enableMenuActions();
            }
        }
        // Ignore OK:REGISTERED and OK:READY — those are acks
    }

    private void refreshOrders() {
        if (connection == null)
            return;
        connection.send("GET_ORDERS");
        statusLabel.setText("Refreshing orders; paid and cancelled orders will be hidden.");
    }

    private void refreshMenu() {
        if (connection != null) {
            connection.send("GET_MENU");
        }
    }

    private void showMenuManager(boolean adding) {
        if (menuDialog != null && menuDialog.isDisplayable()) {
            menuDialog.toFront();
            return;
        }
        if (connection == null) {
            JOptionPane.showMessageDialog(this, "Not connected to the server.",
                    "Menu Unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        addItemDialogOpen = adding;
        menuDialog = new JDialog(this, adding ? "Add Item" : "Remove Item", false);
        menuDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        menuDialog.setLayout(new BorderLayout(8, 8));

        menuListModel = new DefaultListModel<>();
        menuList = new JList<>(menuListModel);
        menuList.setFont(UITheme.BODY);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setSelectionBackground(UITheme.PRIMARY);
        menuList.setSelectionForeground(Color.WHITE);
        menuList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focused);
                if (value instanceof model.MenuItem) {
                    label.setText((index + 1) + ".  " + value);
                }
                return label;
            }
        });
        JScrollPane itemScroll = new JScrollPane(menuList);
        itemScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                "Current Menu", 0, 0, UITheme.BODY_B, UITheme.TEXT));
        menuDialog.add(itemScroll, BorderLayout.CENTER);

        if (adding) {
            JPanel fields = new JPanel(new GridBagLayout());
            fields.setBackground(UITheme.BG);
            fields.setBorder(new EmptyBorder(12, 16, 4, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(3, 3, 3, 3);
            itemNameField = new JTextField(22);
            itemPriceField = new JTextField(22);
            itemNameErrorLabel = menuErrorLabel();
            itemPriceErrorLabel = menuErrorLabel();
            addMenuField(fields, gbc, 0, "Item name", itemNameField, itemNameErrorLabel);
            addMenuField(fields, gbc, 3, "Price (Rs.)", itemPriceField, itemPriceErrorLabel);
            menuDialog.add(fields, BorderLayout.NORTH);
        } else {
            itemNameField = null;
            itemPriceField = null;
            itemNameErrorLabel = null;
            itemPriceErrorLabel = null;
            menuList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && menuSubmitButton != null) {
                    menuSubmitButton.setEnabled(menuList.getSelectedValue() != null);
                }
            });
        }

        menuMessageLabel = new JLabel(" ");
        menuMessageLabel.setFont(UITheme.SMALL);
        menuMessageLabel.setForeground(UITheme.TEXT_MUTED);
        menuSubmitButton = styledButton(adding ? "Add Item" : "Remove Item",
                adding ? UITheme.PRIMARY : UITheme.DANGER);
        menuSubmitButton.setEnabled(adding || menuList.getSelectedValue() != null);
        menuCancelButton = new JButton("Cancel");
        menuCancelButton.setFont(UITheme.BODY);

        JPanel actions = new JPanel(new BorderLayout(8, 4));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(5, 12, 12, 12));
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtons.setOpaque(false);
        actionButtons.add(menuCancelButton);
        actionButtons.add(menuSubmitButton);
        actions.add(menuMessageLabel, BorderLayout.CENTER);
        actions.add(actionButtons, BorderLayout.SOUTH);
        menuDialog.add(actions, BorderLayout.SOUTH);

        menuCancelButton.addActionListener(e -> menuDialog.dispose());
        menuSubmitButton.addActionListener(e -> {
            if (addItemDialogOpen) {
                submitNewMenuItem();
            } else {
                removeSelectedMenuItem();
            }
        });

        menuDialog.setSize(480, adding ? 540 : 440);
        menuDialog.setMinimumSize(new Dimension(400, 380));
        menuDialog.setLocationRelativeTo(this);
        menuDialog.setVisible(true);
        refreshMenu();
    }

    private void submitNewMenuItem() {
        String name = itemNameField.getText().trim();
        String priceText = itemPriceField.getText().trim();
        itemNameErrorLabel.setText(" ");
        itemPriceErrorLabel.setText(" ");

        boolean valid = true;
        if (name.isEmpty()) {
            itemNameErrorLabel.setText("Item name cannot be empty.");
            valid = false;
        } else if (!name.matches("[A-Za-z0-9][A-Za-z0-9 '&().-]{0,49}")) {
            itemNameErrorLabel.setText("Use up to 50 letters, numbers, spaces, or basic punctuation.");
            valid = false;
        }

        BigDecimal price = null;
        if (priceText.isEmpty()) {
            itemPriceErrorLabel.setText("Price cannot be empty.");
            valid = false;
        } else if (!priceText.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            itemPriceErrorLabel.setText("Enter a positive amount with up to 2 decimal places.");
            valid = false;
        } else {
            price = new BigDecimal(priceText);
            if (price.signum() <= 0 || price.compareTo(new BigDecimal("9999.99")) > 0) {
                itemPriceErrorLabel.setText("Price must be greater than 0 and no more than 9999.99.");
                valid = false;
            }
        }
        if (!valid) {
            return;
        }
        if (connection == null) {
            setMenuMessage("Not connected to the server.", UITheme.DANGER);
            return;
        }

        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
        menuMutationPending = true;
        menuSubmitButton.setEnabled(false);
        menuCancelButton.setEnabled(false);
        setMenuMessage("Saving item...", UITheme.TEXT_MUTED);
        connection.send("ADD_ITEM:" + encodedName + ":" + price.stripTrailingZeros().toPlainString());
    }

    private void removeSelectedMenuItem() {
        model.MenuItem selected = menuList.getSelectedValue();
        if (selected == null) {
            setMenuMessage("Select a menu item first.", UITheme.DANGER);
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(menuDialog,
                "Permanently remove \"" + selected.getName() + "\" from the menu?",
                "Confirm Item Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        if (connection == null) {
            setMenuMessage("Not connected to the server.", UITheme.DANGER);
            return;
        }

        menuMutationPending = true;
        menuSubmitButton.setEnabled(false);
        menuCancelButton.setEnabled(false);
        setMenuMessage("Removing item...", UITheme.TEXT_MUTED);
        connection.send("REMOVE_ITEM:" + selected.getId());
    }

    private void addMenuField(JPanel panel, GridBagConstraints gbc, int row, String labelText,
            JTextField field, JLabel errorLabel) {
        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.BODY_B);
        label.setForeground(UITheme.TEXT);
        gbc.gridy = row;
        panel.add(label, gbc);
        field.setFont(UITheme.BODY);
        gbc.gridy = row + 1;
        panel.add(field, gbc);
        gbc.gridy = row + 2;
        panel.add(errorLabel, gbc);
    }

    private JLabel menuErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(UITheme.SMALL);
        label.setForeground(UITheme.DANGER);
        return label;
    }

    private void updateMenuItems(List<model.MenuItem> items) {
        menuItems.clear();
        menuItems.addAll(items);
        if (menuListModel != null) {
            menuListModel.clear();
            for (model.MenuItem item : menuItems) {
                menuListModel.addElement(item);
            }
        }
    }

    private List<model.MenuItem> parseMenu(String response) {
        List<model.MenuItem> items = new ArrayList<>();
        String body = response.substring(5);
        if (body.isEmpty()) {
            return items;
        }
        for (String entry : body.split(";")) {
            String[] fields = entry.split("~", 3);
            if (fields.length == 3) {
                try {
                    items.add(new model.MenuItem(
                            Integer.parseInt(fields[0]),
                            fields[1],
                            Double.parseDouble(fields[2])));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping invalid menu entry: " + entry);
                }
            }
        }
        return items;
    }

    private void setMenuMessage(String message, Color color) {
        if (menuMessageLabel != null && menuDialog != null && menuDialog.isDisplayable()) {
            menuMessageLabel.setForeground(color);
            menuMessageLabel.setText(message);
        }
    }

    private void enableMenuActions() {
        if (menuSubmitButton != null && menuDialog != null && menuDialog.isDisplayable()) {
            menuSubmitButton.setEnabled(addItemDialogOpen || menuList.getSelectedValue() != null);
            menuCancelButton.setEnabled(true);
        }
    }

    private void rebuildTable() {
        orderTable.clearSelection();
        orderModel.setRowCount(0);
        for (Order o : orders) {
            orderModel.addRow(new Object[] {
                    o.getId(),
                    o.getStudentName(),
                    o.getItems(),
                    String.format("%.2f", o.getTotal()),
                    o.getStatus()
            });
        }
        countLabel.setText(orders.size() + " orders");
    }

    private List<Order> parseOrders(String response) {
        List<Order> list = new ArrayList<>();
        String body = response.substring(7); // strip "ORDERS:"
        if (body.isEmpty())
            return list;

        for (String entry : body.split(";")) {
            String[] p = entry.split("~");
            if (p.length == 5) {
                list.add(new Order(
                        Integer.parseInt(p[0]),
                        p[1],
                        p[2].replace(",", ", "), // restore commas if needed
                        Double.parseDouble(p[3]),
                        p[4]));
            }
        }
        return list;
    }

    private void markReady() {
        int row = orderTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }
        int orderId = (int) orderModel.getValueAt(row, 0);
        String status = (String) orderModel.getValueAt(row, 3);

        if ("READY".equals(status)) {
            JOptionPane.showMessageDialog(this, "Order #" + orderId + " is already READY.");
            return;
        }
        if (connection == null) {
            JOptionPane.showMessageDialog(this, "Not connected to server.");
            return;
        }
        // Send and let the broadcast update the table
        connection.send("MARK_READY:" + orderId);
        statusLabel.setText("Marking #" + orderId + " as READY...");
    }

    private void generateBill() {
        Order selectedOrder = getSelectedOrder();
        if (selectedOrder == null) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }
        if (selectedOrder.getStatus() == null
                || !"PAID".equalsIgnoreCase(selectedOrder.getStatus().trim())) {
            JOptionPane.showMessageDialog(this,
                    "Only paid orders can have a bill generated.",
                    "Bill Generation Not Allowed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            java.io.File bill = BillGenerator.generate(selectedOrder);
            statusLabel.setText("Generated " + bill.getName());
            if (connection == null) {
                statusLabel.setText("Bill generated, but student is not notified (offline).");
                return;
            }
            String encodedPath = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(bill.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
            connection.send("BILL_GENERATED:" + selectedOrder.getId() + ":" + encodedPath);
            statusLabel.setText("Generated bill and notified " + selectedOrder.getStudentName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not generate the bill.\n" + ex.getMessage(),
                    "Bill Generation Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Order getSelectedOrder() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }
        int modelRow = orderTable.convertRowIndexToModel(selectedRow);
        int orderId = ((Number) orderModel.getValueAt(modelRow, 0)).intValue();
        for (Order order : orders) {
            if (order.getId() == orderId) {
                return order;
            }
        }
        return null;
    }

    // ---------- Custom status renderer ----------
    private static class StatusRenderer extends DefaultTableCellRenderer {
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
            } else if ("PAID".equals(status)) {
                label.setBackground(UITheme.PAID_BG);
                label.setForeground(UITheme.PAID_FG);
                label.setText("● PAID");
            } else if ("READY".equals(status)) {
                label.setBackground(UITheme.READY_BG);
                label.setForeground(UITheme.READY_FG);
                label.setText("● READY");
            } else if ("PENDING".equals(status)) {
                label.setBackground(UITheme.PENDING_BG);
                label.setForeground(UITheme.PENDING_FG);
                label.setText("● PENDING");
            } else if ("CANCELLED".equals(status)) {
                label.setBackground(new Color(0xFFEBEE));
                label.setForeground(UITheme.DANGER);
                label.setText("● CANCELLED");
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(UITheme.TEXT);
            }
            return label;
        }
    }

    // ---------- Custom items renderer (HTML multi-line) ----------
    private static class ItemsRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);

            String raw = value == null ? "" : value.toString();
            label.setText(formatItemsAsHtml(raw));
            label.setVerticalAlignment(SwingConstants.TOP);
            label.setBorder(new EmptyBorder(6, 8, 6, 8));
            label.setFont(UITheme.SMALL);
            return label;
        }

        private static String formatItemsAsHtml(String raw) {
            if (raw == null || raw.isEmpty()) {
                return "<html><i style='color:#999'>—</i></html>";
            }

            StringBuilder html = new StringBuilder(
                    "<html><table cellspacing='0' cellpadding='1'>");

            String[] parts = raw.split("\\s*,\\s*");
            for (String part : parts) {
                String left = part.split("\\s*=\\s*")[0].trim();
                html.append("<tr>")
                        .append("<td><b>").append(escape(left)).append("</b></td>")
                        .append("</tr>");
            }

            html.append("</table></html>");
            return html.toString();
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StaffDashboard::new);
    }
}