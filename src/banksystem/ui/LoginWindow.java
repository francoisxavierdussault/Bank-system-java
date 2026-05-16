package banksystem.ui;

import banksystem.dto.CustomerDTO;
import banksystem.dto.ManagerDTO;
import banksystem.service.LoginService;
import banksystem.service.LoginService.LoginResult;
import banksystem.service.LoginService.Role;
import banksystem.ui.client1.BankManager;
import banksystem.ui.client2.BankCustomer;
import banksystem.util.BankLogger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Point d'entrée unique de l'application BankSystem.
 * Authentifie l'utilisateur et ouvre BankManager ou BankCustomer selon son rôle.
 *
 * Règles de sécurité :
 *  - Le compte est verrouillé après 3 échecs (enregistré en base Oracle).
 *  - Première connexion : mot de passe par défaut = ID numérique (ex. "1").
 *  - Les sessions sont journalisées via BankLogger.
 */
public class LoginWindow extends JFrame {

    private static final long        serialVersionUID = 1L;
    private static final BankLogger  logger           = BankLogger.getInstance();
    private static final Color       BRAND_BLUE       = new Color(0, 70, 127);
    private static final Color       BRAND_GREEN      = new Color(0, 100, 60);

    private final LoginService loginService = new LoginService();

    private JTextField        txtId;
    private JPasswordField    txtPassword;
    private JRadioButton      rbManager, rbCustomer;
    private JButton           btnLogin;
    private JLabel            lblMessage;
    private JLabel            lblAttempts;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new LoginWindow().setVisible(true));
    }

    public LoginWindow() {
        initialize();
    }

    private void initialize() {
        setTitle("BankSystem — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // --- En-tête ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BRAND_BLUE);
        header.setBorder(new EmptyBorder(24, 30, 24, 30));

        JLabel lblBank = new JLabel("🏦  BankSystem", SwingConstants.CENTER);
        lblBank.setFont(new Font("Tahoma", Font.BOLD, 28));
        lblBank.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Secure Banking Portal", SwingConstants.CENTER);
        lblSub.setFont(new Font("Tahoma", Font.ITALIC, 13));
        lblSub.setForeground(new Color(180, 210, 240));

        header.add(lblBank, BorderLayout.CENTER);
        header.add(lblSub,  BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // --- Formulaire ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(24, 40, 10, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 8, 4);

        // Sélecteur de rôle
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        rolePanel.setBackground(Color.WHITE);
        rbManager  = new JRadioButton("Manager");
        rbCustomer = new JRadioButton("Customer");
        rbManager.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbCustomer.setFont(new Font("Tahoma", Font.BOLD, 13));
        rbManager.setBackground(Color.WHITE);
        rbCustomer.setBackground(Color.WHITE);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbManager); bg.add(rbCustomer);
        rbManager.setSelected(true);
        rolePanel.add(rbManager); rolePanel.add(rbCustomer);
        JPanel roleBorder = new JPanel(new BorderLayout());
        roleBorder.setBorder(new TitledBorder("Role"));
        roleBorder.setBackground(Color.WHITE);
        roleBorder.add(rolePanel);
        form.add(roleBorder, gbc);

        // Identifiant
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        JLabel lblId = new JLabel("ID :");
        lblId.setFont(new Font("Tahoma", Font.BOLD, 13));
        form.add(lblId, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtId = new JTextField();
        txtId.setFont(new Font("Tahoma", Font.PLAIN, 13));
        txtId.setPreferredSize(new Dimension(180, 32));
        form.add(txtId, gbc);

        // Mot de passe
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblPwd = new JLabel("Password :");
        lblPwd.setFont(new Font("Tahoma", Font.BOLD, 13));
        form.add(lblPwd, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Tahoma", Font.PLAIN, 13));
        txtPassword.setPreferredSize(new Dimension(180, 32));
        form.add(txtPassword, gbc);

        // Indication première connexion
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel lblHint = new JLabel("First login? Your default password is your ID number.");
        lblHint.setFont(new Font("Tahoma", Font.ITALIC, 10));
        lblHint.setForeground(Color.GRAY);
        form.add(lblHint, gbc);

        // Bouton de connexion
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = new Insets(16, 4, 4, 4);
        btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnLogin.setBackground(BRAND_BLUE);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setPreferredSize(new Dimension(0, 40));
        form.add(btnLogin, gbc);

        // Étiquette de message
        gbc.gridx = 0; gbc.gridy = 5; gbc.insets = new Insets(6, 4, 0, 4);
        lblMessage = new JLabel(" ", SwingConstants.CENTER);
        lblMessage.setFont(new Font("Tahoma", Font.BOLD, 12));
        form.add(lblMessage, gbc);

        // Étiquette tentatives restantes
        gbc.gridy = 6; gbc.insets = new Insets(2, 4, 4, 4);
        lblAttempts = new JLabel(" ", SwingConstants.CENTER);
        lblAttempts.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblAttempts.setForeground(Color.GRAY);
        form.add(lblAttempts, gbc);

        root.add(form, BorderLayout.CENTER);

        // --- Pied de page ---
        JLabel lblFooter = new JLabel("© 2026 BankSystem — All rights reserved", SwingConstants.CENTER);
        lblFooter.setFont(new Font("Tahoma", Font.ITALIC, 10));
        lblFooter.setForeground(Color.GRAY);
        lblFooter.setBorder(new EmptyBorder(6, 0, 10, 0));
        root.add(lblFooter, BorderLayout.SOUTH);

        // --- Écouteurs d'événements ---
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin()); // Touche Entrée
        txtId.addActionListener(e -> txtPassword.requestFocus());
    }

    private void doLogin() {
        String idText  = txtId.getText().trim();
        String pwd     = new String(txtPassword.getPassword());

        if (idText.isEmpty()) {
            showMessage("Please enter your ID.", Color.RED); return;
        }
        if (pwd.isEmpty()) {
            showMessage("Please enter your password.", Color.RED); return;
        }

        int id;
        try { id = Integer.parseInt(idText); }
        catch (NumberFormatException e) {
            showMessage("ID must be a number.", Color.RED); return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Exécution en arrière-plan pour ne pas bloquer l'interface lors de l'appel BD
        new Thread(() -> {
            LoginResult result = rbManager.isSelected()
                ? loginService.loginManager(id, pwd)
                : loginService.loginCustomer(id, pwd);

            SwingUtilities.invokeLater(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                handleResult(result);
            });
        }).start();
    }

    private void handleResult(LoginResult result) {
        if (!result.success) {
            showMessage(result.message, Color.RED);
            if (result.attemptsLeft > 0) {
                lblAttempts.setText("Attempts remaining: " + result.attemptsLeft);
            } else {
                lblAttempts.setText("Account locked — contact your administrator.");
                btnLogin.setEnabled(false);
            }
            txtPassword.setText("");
            return;
        }

        // Connexion réussie
        showMessage("Login successful! Opening...", new Color(0, 128, 0));
        logger.info("LoginWindow", "Opening " + result.role + " UI");

        dispose(); // ferme la fenêtre de connexion

        if (result.role == Role.MANAGER) {
            ManagerDTO mgr = result.manager;
            EventQueue.invokeLater(() -> new BankManager(mgr).setVisible(true));
        } else {
            CustomerDTO cust = result.customer;
            EventQueue.invokeLater(() -> new BankCustomer(cust).setVisible(true));
        }
    }

    private void showMessage(String msg, Color color) {
        lblMessage.setText(msg);
        lblMessage.setForeground(color);
    }
}
