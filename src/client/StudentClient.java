package client;

import model.MenuItem;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentClient extends JFrame {

    private JTextField nameField;
    private JTable menuTable;
    private DefaultTableModel menuModel;
    private JSpinner qtySpinner;
    private JTextArea cartArea;
    private JLabel totalLabel;
    private JLabel statusLabel;

    private List<MenuItem> menuItems;
    private double cartTotal = 0.0;
    private StringBuilder cartContents = new StringBuilder();

    public StudentClient() {
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

        JLabel nameLbl = new JLabel("Your Name:");
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setFont(UITheme.BODY_B);

        nameField = new JTextField(15);
        nameField.setFont(UITheme.BODY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1),
                new EmptyBorder(5, 8, 5, 8)));

        namePanel.add(nameLbl);
        namePanel.add(nameField);
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

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuScroll, cartPanel);
        split.setDividerLocation(600);
        split.setBorder(null);
        return split;
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
        double lineTotal = item.getPrice() * qty;

        cartContents.append(String.format("%-15s x%-3d  Rs. %8.2f%n",
                item.getName(), qty, lineTotal));
        cartTotal += lineTotal;
        refreshCart();
    }

    private void refreshCart() {
        cartArea.setText(cartContents.toString());
        totalLabel.setText(String.format("Total: Rs. %.2f", cartTotal));
    }

    private void clearCart() {
        cartContents.setLength(0);
        cartTotal = 0.0;
        refreshCart();
        statusLabel.setText("Cart cleared.");
    }

    private void placeOrder() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your name.");
            return;
        }
        if (cartTotal == 0.0) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.");
            return;
        }

        // Build "Burger x2, Coke x1" for the socket message
        String items = cartContents.toString().trim().replace("\n", ", ")
                .replaceAll("\\s+", " ");
        // Careful: ':' is our protocol separator, so remove any ':' from items
        items = items.replace(":", " ");

        // Format: ORDER:name:items:total
        String cmd = "ORDER:" + name + ":" + items + ":" + cartTotal;

        try {
            String response = ServerConnection.send(cmd);
            if (response != null && response.startsWith("ORDER_OK:")) {
                int orderId = Integer.parseInt(response.substring(9));
                JOptionPane.showMessageDialog(this,
                        "Order placed! Order ID: " + orderId);
                clearCart();
                nameField.setText("");
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StudentClient::new);
    }
}