package app;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Random;

@SuppressWarnings("serial")
public class atmsim extends JFrame {

    private Connection conn;
    private String dbUrl = "jdbc:mysql://localhost:3306/atm_database";
    private String dbUser = "root";
    private String dbPassword = ""; //Enter your Wampserver or MySQL server password if present

    private JTextField loginField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;

    private int loggedInAccountNumber;
    private String loggedInFullName;
    private double loggedInBalance;

    public atmsim() {
        setTitle("ATM System - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        connectToDatabase();

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));

        panel.add(new JLabel("Account No / Username:"));
        loginField = new JTextField();
        panel.add(loginField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        panel.add(loginButton);
        panel.add(registerButton);

        add(panel);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        setVisible(true);
    }

    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Database Connected");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void login() {
        String loginInput = loginField.getText().trim();
        String enteredPassword = String.valueOf(passwordField.getPassword());

        if (loginInput.isEmpty() || enteredPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String query;
            boolean isAccountNumber = loginInput.matches("\\d+");

            if (isAccountNumber) {
                query = "SELECT * FROM users WHERE account_number=? AND password=?";
            } else {
                query = "SELECT * FROM users WHERE username=? AND password=?";
            }

            PreparedStatement pst = conn.prepareStatement(query);

            if (isAccountNumber) {
                pst.setInt(1, Integer.parseInt(loginInput));
            } else {
                pst.setString(1, loginInput);
            }

            pst.setString(2, enteredPassword);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                loggedInAccountNumber = rs.getInt("account_number");
                loggedInFullName = rs.getString("full_name");
                loggedInBalance = rs.getDouble("balance");

                if (loggedInBalance <= 0) {
                    JOptionPane.showMessageDialog(this, "Inactive account! Please deposit first.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                showATMMenu();

            } else {
                JOptionPane.showMessageDialog(this, "Invalid login credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }

            rs.close();
            pst.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        JTextField fullNameField = new JTextField();
        JTextField usernameField = new JTextField();
        JTextField dobField = new JTextField("YYYY-MM-DD");
        String[] genders = {"Male", "Female", "Other"};
        JComboBox<String> genderBox = new JComboBox<>(genders);
        JPasswordField newPasswordField = new JPasswordField();
        JTextField depositField = new JTextField();

        JPanel regPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        regPanel.add(new JLabel("Full Name:"));
        regPanel.add(fullNameField);
        regPanel.add(new JLabel("Username:"));
        regPanel.add(usernameField);
        regPanel.add(new JLabel("Date of Birth:"));
        regPanel.add(dobField);
        regPanel.add(new JLabel("Gender:"));
        regPanel.add(genderBox);
        regPanel.add(new JLabel("New Password:"));
        regPanel.add(newPasswordField);
        regPanel.add(new JLabel("Initial Deposit:"));
        regPanel.add(depositField);

        int result = JOptionPane.showConfirmDialog(this, regPanel, "Register New Account", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String fullName = fullNameField.getText().trim();
            String username = usernameField.getText().trim();
            String dobStr = dobField.getText().trim();
            String gender = (String) genderBox.getSelectedItem();
            String password = String.valueOf(newPasswordField.getPassword());
            String depositStr = depositField.getText().trim();

            if (fullName.isEmpty() || username.isEmpty() || dobStr.isEmpty() || password.isEmpty() || depositStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String checkUserQuery = "SELECT * FROM users WHERE username=?";
                PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery);
                checkUserStmt.setString(1, username);
                ResultSet userRs = checkUserStmt.executeQuery();

                if (userRs.next()) {
                    JOptionPane.showMessageDialog(this, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                userRs.close();
                checkUserStmt.close();

                LocalDate dob = LocalDate.parse(dobStr);
                int age = Period.between(dob, LocalDate.now()).getYears();

                int accountNum = generateAccountNumber();
                double deposit = Double.parseDouble(depositStr);

                String insertQuery = "INSERT INTO users (account_number, username, full_name, dob, gender, age, password, balance) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(insertQuery);
                pst.setInt(1, accountNum);
                pst.setString(2, username);
                pst.setString(3, fullName);
                pst.setDate(4, Date.valueOf(dob));
                pst.setString(5, gender);
                pst.setInt(6, age);
                pst.setString(7, password);
                pst.setDouble(8, deposit);

                int inserted = pst.executeUpdate();

                if (inserted > 0) {
                    JOptionPane.showMessageDialog(this, "Account created!\nAccount Number: " + accountNum, "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed!", "Error", JOptionPane.ERROR_MESSAGE);
                }

                pst.close();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid input!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void showATMMenu() {
        JFrame menuFrame = new JFrame("Welcome " + loggedInFullName);
        menuFrame.setSize(400, 400);
        menuFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(8, 1, 10, 10));

        JButton checkBalanceBtn = new JButton("Check Balance");
        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton transferBtn = new JButton("Transfer");
        JButton payBtn = new JButton("Pay");
        JButton receiptBtn = new JButton("Print Receipt");
        JButton exitBtn = new JButton("Exit");

        panel.add(checkBalanceBtn);
        panel.add(depositBtn);
        panel.add(withdrawBtn);
        panel.add(transferBtn);
        panel.add(payBtn);
        panel.add(receiptBtn);
        panel.add(exitBtn);

        checkBalanceBtn.addActionListener(e -> checkBalance());
        depositBtn.addActionListener(e -> deposit());
        withdrawBtn.addActionListener(e -> withdraw());
        transferBtn.addActionListener(e -> transfer());
        payBtn.addActionListener(e -> pay());
        receiptBtn.addActionListener(e -> printReceipt());
        exitBtn.addActionListener(e -> menuFrame.dispose());

        menuFrame.add(panel);
        menuFrame.setVisible(true);
    }

    private void checkBalance() {
        JOptionPane.showMessageDialog(this, "Current Balance: $" + loggedInBalance, "Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deposit() {
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to deposit:");
        if (amountStr == null || amountStr.isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr);
            loggedInBalance += amount;
            updateBalanceInDatabase();
            JOptionPane.showMessageDialog(this, "Deposit successful. New balance: $" + loggedInBalance);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void withdraw() {
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to withdraw:");
        if (amountStr == null || amountStr.isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount > loggedInBalance) {
                JOptionPane.showMessageDialog(this, "Insufficient funds.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                loggedInBalance -= amount;
                updateBalanceInDatabase();
                JOptionPane.showMessageDialog(this, "Withdrawal successful. New balance: $" + loggedInBalance);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void transfer() {
        String targetAccStr = JOptionPane.showInputDialog(this, "Enter target account number:");
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to transfer:");
        if (targetAccStr == null || amountStr == null) return;

        try {
            int targetAcc = Integer.parseInt(targetAccStr);
            double amount = Double.parseDouble(amountStr);

            if (amount > loggedInBalance) {
                JOptionPane.showMessageDialog(this, "Insufficient funds.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String query = "SELECT balance FROM users WHERE account_number=?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, targetAcc);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                double targetBalance = rs.getDouble("balance");

                String updateSender = "UPDATE users SET balance=? WHERE account_number=?";
                String updateReceiver = "UPDATE users SET balance=? WHERE account_number=?";

                PreparedStatement pstSender = conn.prepareStatement(updateSender);
                pstSender.setDouble(1, loggedInBalance - amount);
                pstSender.setInt(2, loggedInAccountNumber);

                PreparedStatement pstReceiver = conn.prepareStatement(updateReceiver);
                pstReceiver.setDouble(1, targetBalance + amount);
                pstReceiver.setInt(2, targetAcc);

                pstSender.executeUpdate();
                pstReceiver.executeUpdate();

                loggedInBalance -= amount;

                JOptionPane.showMessageDialog(this, "Transferred $" + amount + " to account " + targetAcc);
            } else {
                JOptionPane.showMessageDialog(this, "Target account not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pay() {
        JOptionPane.showMessageDialog(this, "Pay function placeholder. Customize here!");
    }

    private void printReceipt() {
        JOptionPane.showMessageDialog(this, "Receipt\n------------------\nAccount No: " + loggedInAccountNumber +
                "\nName: " + loggedInFullName +
                "\nBalance: $" + loggedInBalance +
                "\nThank you for banking with us!", "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateBalanceInDatabase() {
        try {
            String query = "UPDATE users SET balance=? WHERE account_number=?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setDouble(1, loggedInBalance);
            pst.setInt(2, loggedInAccountNumber);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int generateAccountNumber() {
        Random rand = new Random();
        return 100000000 + rand.nextInt(900000000);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new atmsim());
    }
}
