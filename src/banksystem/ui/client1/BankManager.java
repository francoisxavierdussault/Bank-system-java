package banksystem.ui.client1;

import banksystem.dao.AccountDAO;
import banksystem.dto.ManagerDTO;
import banksystem.service.CustomerService;
import banksystem.service.ManagerService;
import banksystem.util.BankLogger;
import banksystem.util.SessionManager;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Interface gestionnaire — fenêtre principale après connexion réussie en tant que manager.
 *
 * <p>Cette classe assemble les quatre onglets WindowBuilder-compatibles :
 * {@link ManagerTabPanel}, {@link CustomerTabPanel}, {@link AccountTabPanel} et
 * {@link LogTabPanel}. Elle gère également la session (timeout 600 s) et le logout.</p>
 *
 * <p><b>Pour modifier l'UI d'un onglet :</b> ouvrir le fichier du panneau correspondant
 * dans Eclipse et basculer sur l'onglet « Design » (WindowBuilder).</p>
 */
public class BankManager extends JFrame {

    private static final long     serialVersionUID = 1L;
    private static final BankLogger logger          = BankLogger.getInstance();
    private static final int      SESSION_TIMEOUT_S = 600;

    private final ManagerDTO      loggedInManager;
    private final ManagerService  managerService  = new ManagerService();
    private final CustomerService customerService = new CustomerService();
    private final AccountDAO      accountDAO      = new AccountDAO();

    private SessionManager session;

    // -----------------------------------------------------------------------
    // Panneaux des onglets (instanciés dans le constructeur)
    // -----------------------------------------------------------------------

    private ManagerTabPanel  managerTab;
    private CustomerTabPanel customerTab;
    private AccountTabPanel  accountTab;
    private LogTabPanel      logTab;

    // -----------------------------------------------------------------------
    // Constructeur
    // -----------------------------------------------------------------------

    /**
     * Construit et affiche la fenêtre principale du gestionnaire.
     * Initialise les quatre panneaux d'onglets, charge les données initiales et démarre la session.
     *
     * @param loggedInManager DTO du gestionnaire authentifié (fourni par LoginWindow)
     */
    public BankManager(ManagerDTO loggedInManager) {
        this.loggedInManager = loggedInManager;

        setTitle("Fortis Bank — Logged in as: " + loggedInManager.getManagerName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 80, 900, 680);

        JPanel contentPane = new JPanel(new BorderLayout(0, 6));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // --- Barre de titre ---
        contentPane.add(buildTitleBar(), BorderLayout.NORTH);

        // --- Création des panneaux d'onglets ---
        managerTab  = new ManagerTabPanel(managerService, customerService, loggedInManager);
        customerTab = new CustomerTabPanel(customerService, managerService);
        accountTab  = new AccountTabPanel(accountDAO, customerService, loggedInManager);
        logTab      = new LogTabPanel();

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Tahoma", Font.BOLD, 13));
        tabbedPane.addTab("  Manager   ", managerTab);
        tabbedPane.addTab("  Customer  ", customerTab);
        tabbedPane.addTab("  Accounts  ", accountTab);
        tabbedPane.addTab("  Log Viewer", logTab);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 1) customerTab.loadManagerCombo();
            if (idx == 2) accountTab.refresh();
            if (idx == 3) logTab.refresh();
        });

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // --- Chargement initial ---
        managerTab.mgrSelectAll();
        customerTab.custSelectAll();
        startSession();
    }

    // -----------------------------------------------------------------------
    // Barre de titre
    // -----------------------------------------------------------------------

    private JPanel buildTitleBar() {
        JPanel titlePanel = new JPanel(new BorderLayout());

        JLabel lblTitle = new JLabel("Fortis Bank Manager - Database Management", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 70, 127));

        JLabel lblUser = new JLabel(
            "👤  " + loggedInManager.getManagerName() + "  (ID: " + loggedInManager.getManagerId() + ")",
            SwingConstants.RIGHT);
        lblUser.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblUser.setForeground(Color.DARK_GRAY);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Tahoma", Font.BOLD, 11));
        btnLogout.setBackground(new Color(180, 40, 40));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setOpaque(true);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) logout();
        });

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(lblUser);
        eastPanel.add(btnLogout);

        titlePanel.add(lblTitle,  BorderLayout.CENTER);
        titlePanel.add(eastPanel, BorderLayout.EAST);
        return titlePanel;
    }

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------

    private void startSession() {
        logger.info("BankManager", "Session opened by Manager ID="
            + loggedInManager.getManagerId() + " (" + loggedInManager.getManagerName() + ")");
        session = new SessionManager("Manager", SESSION_TIMEOUT_S, this::onSessionTimeout);
        session.attach(this, SESSION_TIMEOUT_S, null);
    }

    private void onSessionTimeout() {
        JOptionPane.showMessageDialog(this,
            "Your session has expired due to inactivity.\nYou will be returned to the login screen.",
            "Session Timeout", JOptionPane.WARNING_MESSAGE);
        logger.warn("BankManager", "Session timed out for Manager ID=" + loggedInManager.getManagerId());
        logout();
    }

    private void logout() {
        session.end();
        logger.info("BankManager", "Manager ID=" + loggedInManager.getManagerId()
            + " (" + loggedInManager.getManagerName() + ") logged out");
        dispose();
        EventQueue.invokeLater(() -> new banksystem.ui.LoginWindow().setVisible(true));
    }
}
