package client;

import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class LoginScreen extends JFrame {

    private JTextField nameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;
    private JButton loginBtn;
    private JButton addLoginBtn;

    public LoginScreen() {
        setTitle("Smart Canteen - Login");
        setSize(420, 410);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);

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
        gbc.gridwidth = 2;

        // Heading
        JLabel prompt = new JLabel("Enter your username and password:");
        prompt.setFont(UITheme.BODY_B);
        prompt.setForeground(UITheme.TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        wrapper.add(prompt, gbc);

        // Name label + field
        JLabel nameLbl = new JLabel("Username");
        nameLbl.setFont(UITheme.SMALL);
        nameLbl.setForeground(UITheme.TEXT_MUTED);
        gbc.gridx = 0;
        gbc.gridy = 1;
        wrapper.add(nameLbl, gbc);

        nameField = new JTextField(18);
        nameField.setFont(UITheme.BODY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        gbc.gridx = 0;
        gbc.gridy = 2;
        wrapper.add(nameField, gbc);

        // Password label + field
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UITheme.SMALL);
        passLbl.setForeground(UITheme.TEXT_MUTED);
        gbc.gridx = 0;
        gbc.gridy = 3;
        wrapper.add(passLbl, gbc);

        passwordField = new JPasswordField(18);
        passwordField.setFont(UITheme.BODY);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        gbc.gridx = 0;
        gbc.gridy = 4;
        wrapper.add(passwordField, gbc);

        // Login button
        loginBtn = new JButton("Login");
        loginBtn.setFont(UITheme.BODY_B);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(UITheme.PRIMARY);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(new EmptyBorder(8, 24, 8, 24));
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setOpaque(true);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        wrapper.add(loginBtn, gbc);

        // Message label
        messageLabel = new JLabel(" ");
        messageLabel.setFont(UITheme.SMALL);
        messageLabel.setForeground(UITheme.DANGER);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(messageLabel, gbc);

        addLoginBtn = new JButton("Add New Login");
        addLoginBtn.setFont(UITheme.BODY_B);
        addLoginBtn.setForeground(UITheme.PRIMARY_DARK);
        addLoginBtn.setFocusPainted(false);
        addLoginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addLoginBtn.addActionListener(e -> showCreateAccountDialog());
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        wrapper.add(addLoginBtn, gbc);

        // Enter key on either field triggers login
        nameField.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        loginBtn.addActionListener(e -> attemptLogin());

        return wrapper;
    }

    private void attemptLogin() {
        String name = nameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty()) {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("Please enter your username.");
            return;
        }
        if (password.isEmpty()) {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("Please enter your password.");
            return;
        }

        messageLabel.setForeground(UITheme.TEXT_MUTED);
        messageLabel.setText("Checking...");
        loginBtn.setEnabled(false);

        // Network I/O off the EDT
        new Thread(() -> {
            String response;
            try {
                String encodedPassword = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(password.getBytes(StandardCharsets.UTF_8));
                response = ServerConnection.send("LOGIN:" + name + ":B64:" + encodedPassword);
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
            messageLabel.setText("Invalid name or password.");
            passwordField.setText("");
            nameField.selectAll();
            nameField.requestFocus();
        } else {
            messageLabel.setForeground(UITheme.DANGER);
            messageLabel.setText("Login failed: " + response);
        }
    }

    private void showCreateAccountDialog() {
        JDialog dialog = new JDialog(this, "Add New Login", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG);
        form.setBorder(new EmptyBorder(18, 22, 18, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);

        JTextField usernameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField newPasswordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        JLabel usernameError = fieldErrorLabel();
        JLabel emailError = fieldErrorLabel();
        JLabel passwordError = fieldErrorLabel();
        JLabel confirmError = fieldErrorLabel();
        JLabel formMessage = new JLabel(" ");
        formMessage.setFont(UITheme.SMALL);
        formMessage.setForeground(UITheme.DANGER);

        addFormField(form, gbc, 0, "Username", usernameField, usernameError);
        addFormField(form, gbc, 3, "Gmail address", emailField, emailError);
        addFormField(form, gbc, 6, "Password", newPasswordField, passwordError);
        addFormField(form, gbc, 9, "Confirm Password", confirmPasswordField, confirmError);

        gbc.gridy = 12;
        form.add(formMessage, gbc);

        JButton createButton = styledButton("Create Account");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UITheme.BODY);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(createButton);
        gbc.gridy = 13;
        form.add(actions, gbc);

        cancelButton.addActionListener(e -> dialog.dispose());
        createButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            char[] passwordChars = newPasswordField.getPassword();
            char[] confirmation = confirmPasswordField.getPassword();

            usernameError.setText(" ");
            emailError.setText(" ");
            passwordError.setText(" ");
            confirmError.setText(" ");
            formMessage.setText(" ");
            boolean valid = true;
            if (username.isEmpty()) {
                usernameError.setText("Username cannot be empty.");
                valid = false;
            } else if (!username.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,29}")) {
                usernameError.setText("Use 3-30 letters, numbers, dots, underscores, or hyphens.");
                valid = false;
            }
            if (email.isEmpty()) {
                emailError.setText("Gmail address cannot be empty.");
                valid = false;
            } else if (!email.matches("(?i)[a-z0-9](?:[a-z0-9.]*[a-z0-9])?@gmail\\.com")
                    || email.length() > 254
                    || email.substring(0, email.indexOf('@')).length() < 6
                    || email.substring(0, email.indexOf('@')).length() > 30
                    || email.substring(0, email.indexOf('@')).contains("..")) {
                emailError.setText("Enter a valid Gmail address (for example, name@gmail.com).");
                valid = false;
            }
            if (!db.UserDAO.isValidPassword(passwordChars)) {
                if (passwordChars.length == 0) {
                    passwordError.setText("Password cannot be empty.");
                } else {
                    passwordError.setText("Use 8-128 characters with at least one letter and one number.");
                }
                valid = false;
            }
            if (confirmation.length == 0) {
                confirmError.setText("Please confirm your password.");
                valid = false;
            } else if (!Arrays.equals(passwordChars, confirmation)) {
                confirmError.setText("Passwords do not match.");
                valid = false;
            }
            if (!valid) {
                Arrays.fill(passwordChars, '\0');
                Arrays.fill(confirmation, '\0');
                return;
            }

            String password = new String(passwordChars);
            Arrays.fill(passwordChars, '\0');
            Arrays.fill(confirmation, '\0');
            String encodedPassword = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(password.getBytes(StandardCharsets.UTF_8));
            String encodedEmail = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(email.toLowerCase(java.util.Locale.ROOT)
                            .getBytes(StandardCharsets.UTF_8));
            createButton.setEnabled(false);
            cancelButton.setEnabled(false);
            formMessage.setForeground(UITheme.TEXT_MUTED);
            formMessage.setText("Creating account...");

            new Thread(() -> {
                String response;
                try {
                    response = ServerConnection.send(
                            "CREATE_STUDENT:" + username + ":" + encodedEmail + ":" + encodedPassword);
                } catch (Exception ex) {
                    response = "ERROR:" + ex.getMessage();
                }
                String result = response;
                SwingUtilities.invokeLater(() -> {
                    if ("ACCOUNT_CREATED".equals(result)) {
                        nameField.setText(username);
                        passwordField.setText("");
                        messageLabel.setForeground(new Color(0x2E7D32));
                        messageLabel.setText("Account created. You can now log in.");
                        dialog.dispose();
                    } else {
                        createButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        if ("ACCOUNT_EXISTS".equals(result)) {
                            usernameError.setText("Username or Gmail address is already registered.");
                        } else {
                            formMessage.setForeground(UITheme.DANGER);
                            formMessage.setText(result != null && result.startsWith("ERROR:")
                                    ? result.substring(6)
                                    : "Could not create the account. Please try again.");
                        }
                    }
                });
            }, "create-student-account").start();
        });

        dialog.setContentPane(form);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(460, 390));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addFormField(JPanel form, GridBagConstraints gbc, int row,
            String labelText, JComponent field, JLabel error) {
        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.BODY_B);
        label.setForeground(UITheme.TEXT);
        gbc.gridy = row;
        form.add(label, gbc);
        field.setFont(UITheme.BODY);
        gbc.gridy = row + 1;
        form.add(field, gbc);
        gbc.gridy = row + 2;
        form.add(error, gbc);
    }

    private JLabel fieldErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(UITheme.SMALL);
        label.setForeground(UITheme.DANGER);
        return label;
    }

    private JButton styledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UITheme.BODY_B);
        button.setForeground(Color.WHITE);
        button.setBackground(UITheme.PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}