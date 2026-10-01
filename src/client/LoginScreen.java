package client;

import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginScreen extends JFrame {

    private JTextField nameField;
    private JLabel messageLabel;
    private JButton loginBtn;

    public LoginScreen() {
        setTitle("Smart Canteen - Login");
        setSize(420, 280);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(),   BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY);
        header.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("🍔  Smart Canteen");
        title.setFont(UITheme.H1);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel prompt = new JLabel("Enter your name to continue:");
        prompt.setFont(UITheme.BODY_B);
        prompt.setForeground(UITheme.TEXT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        wrapper.add(prompt, gbc);

        nameField = new JTextField(18);
        nameField.setFont(UITheme.BODY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        wrapper.add(nameField, gbc);

        loginBtn = new JButton("Login");
        loginBtn.setFont(UITheme.BODY_B);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(UITheme.PRIMARY);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(new EmptyBorder(8, 24, 8, 24));
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setOpaque(true);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        wrapper.add(loginBtn, gbc);

        messageLabel = new JLabel(" ");
        messageLabel.setFont(UITheme.SMALL);
        messageLabel.setForeground(UITheme.DANGER);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(messageLabel, gbc);

        // Enter key triggers login
        nameField.addActionListener(e -> attemptLogin());
        loginBtn.addActionListener(e -> attemptLogin());

        return wrapper;
    }

    private void attemptLogin() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            messageLabel.setText("Please enter your name.");
            return;
        }

        messageLabel.setForeground(UITheme.TEXT_MUTED);
        messageLabel.setText("Checking...");
        loginBtn.setEnabled(false);

        // Run network I/O off the EDT so the UI doesn't freeze
        new Thread(() -> {
            String response;
            try {
                response = ServerConnection.send("LOGIN:" + name);
            } catch (Exception ex) {
                response = "ERROR:" + ex.getMessage();
            }
            final String result = response;

            SwingUtilities.invokeLater(() -> {
                loginBtn.setEnabled(true);
                handleLoginResponse(name, result);
            });
        }).start();
    }

    private void handleLoginResponse(String name, String response) {
        if (response == null) {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("Server not reachable.");
            return;
        }

        if (response.startsWith("LOGIN_OK:")) {
            // Open dashboard, close this window
            new StudentClient(name);
            dispose();
        } else if (response.equals("LOGIN_FAIL")) {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("User not found. Try again!");
            nameField.selectAll();
            nameField.requestFocus();
        } else {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("Login failed: " + response);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}