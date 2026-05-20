package banksystem.ui.client2;

import banksystem.business.*;
import banksystem.dao.AccountDAO;
import banksystem.dao.LineOfCreditAccountDAO;
import banksystem.dao.TransactionDAO;
import banksystem.data.DataCollection;
import banksystem.dto.AccountDTO;
import banksystem.dto.CustomerDTO;
import banksystem.dto.LineOfCreditAccountDTO;
import banksystem.enums.EnumAccountStatus;
import banksystem.service.CustomerService;
import banksystem.util.BankLogger;
import banksystem.util.SessionManager;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Interface client — fenêtre principale après connexion réussie en tant que client.
 *
 * <p>Cette classe assemble les trois onglets WindowBuilder-compatibles :
 * {@link TransactionTabPanel}, {@link MovementsTabPanel} et {@link HistoryTabPanel}.
 * Elle gère le chargement des données Oracle, la session (timeout 300 s) et le logout.</p>
 *
 * <p><b>Pour modifier l'UI d'un onglet :</b> ouvrir le fichier du panneau correspondant
 * dans Eclipse et basculer sur l'onglet « Design » (WindowBuilder).</p>
 */
public class BankCustomer extends JFrame {

    private static final long     serialVersionUID = 1L;
    private static final BankLogger logger          = BankLogger.getInstance();
    private static final int      SESSION_TIMEOUT_S = 300;

    private final CustomerDTO     loggedInCustomer;
    private final CustomerService customerService = new CustomerService();
    private final AccountDAO      accountDAO      = new AccountDAO();
    private final TransactionDAO  transactionDAO  = new TransactionDAO();

    private SessionManager session;

    // -----------------------------------------------------------------------
    // Panneaux des onglets
    // -----------------------------------------------------------------------

    private TransactionTabPanel transactionTab;
    private MovementsTabPanel   movementsTab;
    private HistoryTabPanel     historyTab;

    // Barre de statut partagée (passée à TransactionTabPanel)
    private JLabel lblStatusBar;

    // -----------------------------------------------------------------------
    // Constructeur
    // -----------------------------------------------------------------------

    /**
     * Construit et affiche la fenêtre principale du client.
     * Charge les comptes Oracle dans {@link banksystem.data.DataCollection}, initialise les trois
     * panneaux d'onglets et démarre la session avec timeout automatique.
     *
     * @param loggedInCustomer DTO du client authentifié (fourni par LoginWindow)
     */
    public BankCustomer(CustomerDTO loggedInCustomer) {
        this.loggedInCustomer = loggedInCustomer;

        // 1. Chargement des données Oracle → DataCollection
        loadCustomerData();

        // 2. Construction de l'interface
        String name = loggedInCustomer.getFirstName() + " " + loggedInCustomer.getLastName();
        setTitle("BankCustomer — " + name + "  (ID: " + loggedInCustomer.getCustomerId() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(150, 100, 740, 640);

        JPanel contentPane = new JPanel(new BorderLayout(0, 8));
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(contentPane);

        // Barre de titre
        contentPane.add(buildTitleBar(name), BorderLayout.NORTH);

        // Barre de statut (créée avant les panneaux qui en ont besoin)
        lblStatusBar = new JLabel("  Welcome, " + loggedInCustomer.getFirstName() + "!");
        lblStatusBar.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblStatusBar.setForeground(Color.DARK_GRAY);
        contentPane.add(lblStatusBar, BorderLayout.SOUTH);

        // Création des panneaux d'onglets
        transactionTab = new TransactionTabPanel(accountDAO, customerService, transactionDAO,
            loggedInCustomer, lblStatusBar);
        movementsTab   = new MovementsTabPanel(transactionDAO, loggedInCustomer);
        historyTab     = new HistoryTabPanel(transactionDAO, loggedInCustomer);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Tahoma", Font.BOLD, 13));
        tabs.addTab("  Transactions  ", transactionTab);
        tabs.addTab("  Mouvements    ", movementsTab);
        tabs.addTab("  History       ", historyTab);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) movementsTab.refresh();
            if (tabs.getSelectedIndex() == 2) historyTab.refresh();
        });
        contentPane.add(tabs, BorderLayout.CENTER);

        // 3. Remplissage de la combo compte (après que les panneaux sont créés)
        transactionTab.loadAccountCombo();

        // 4. Démarrage de la session
        startSession();
    }

    // -----------------------------------------------------------------------
    // Barre de titre
    // -----------------------------------------------------------------------

    private JPanel buildTitleBar(String name) {
        JPanel titlePanel = new JPanel(new BorderLayout());

        JLabel lblTitle = new JLabel("My Bank Accounts", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 100, 0));

        JLabel lblUser = new JLabel(
            "👤  " + name + "  (ID: " + loggedInCustomer.getCustomerId() + ")",
            SwingConstants.RIGHT);
        lblUser.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblUser.setForeground(Color.DARK_GRAY);

        JButton btnLogout = new JButton("🚪 Logout");
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
    // Chargement des données
    // -----------------------------------------------------------------------

    /**
     * Charge les comptes du client connecté depuis Oracle dans DataCollection.
     * Si aucun compte n'existe en base, crée un compte épargne par défaut.
     */
    private void loadCustomerData() {
        int cid = loggedInCustomer.getCustomerId();
        try {
            if (DataCollection.findCustomer(cid) == null) {
                Customer c = new Customer(cid,
                    loggedInCustomer.getFirstName(), loggedInCustomer.getLastName(),
                    loggedInCustomer.getEmail(), loggedInCustomer.getPhone());
                DataCollection.addCustomer(c);
            }

            java.util.List<AccountDTO> dbAccounts = accountDAO.selectByCustomer(cid);

            if (!dbAccounts.isEmpty()) {
                for (AccountDTO dto : dbAccounts) {
                    if (DataCollection.findAccount(dto.getAccountId()) != null) {
                        Account existing = DataCollection.findAccount(dto.getAccountId());
                        try { existing.setBalance(dto.getBalance()); } catch (Exception ignored) {}
                        existing.setStatus(EnumAccountStatus.valueOf(dto.getStatus()));
                        existing.setDailyWithdrawalLimit(dto.getDailyLimit());
                        continue;
                    }
                    Account acc = buildAccountFromDTO(dto);
                    if (acc != null) DataCollection.addAccount(acc);
                }
                logger.info("BankCustomer", "Loaded " + dbAccounts.size()
                    + " account(s) from Oracle for Customer ID=" + cid);
            } else {
                if (DataCollection.getAccountsByCustomer(cid).isEmpty()) {
                    long newId = 1000L + cid;
                    SavingsAccount acc = new SavingsAccount(newId, (long) cid, 1000.0, 0.02);
                    DataCollection.addAccount(acc);
                    AccountDTO dto = new AccountDTO(newId, cid, "SAVINGS", 1000.0, "ACTIVE", 5000.0, new java.util.Date());
                    accountDAO.insert(dto);
                    logger.info("BankCustomer", "Created default savings account #" + newId + " for Customer ID=" + cid);
                }
            }
        } catch (Exception e) {
            logger.error("BankCustomer", "loadCustomerData error: " + e.getMessage());
        }
    }

    /** Construit la sous-classe Account appropriée à partir d'un AccountDTO. */
    private Account buildAccountFromDTO(AccountDTO dto) {
        try {
            Account acc;
            switch (dto.getAccountType()) {
                case "CHECKING":
                    acc = new CheckingAccount(dto.getAccountId(), (long) dto.getCustomerId(), dto.getBalance(), 3);
                    break;
                case "CREDIT":
                    acc = new CreditAccount(dto.getAccountId(), (long) dto.getCustomerId(), dto.getBalance(), 10000.0);
                    break;
                case "CURRENCY":
                    acc = new CurrencyAccount(dto.getAccountId(), (long) dto.getCustomerId(), dto.getBalance());
                    break;
                case "LINE_OF_CREDIT": {
                    LineOfCreditAccountDAO locDao = new LineOfCreditAccountDAO();
                    LineOfCreditAccountDTO locDto = locDao.selectById(dto.getAccountId());
                    double creditLimit = (locDto != null) ? locDto.getCreditLimit() : 25000.0;
                    acc = new LineOfCreditAccount(dto.getAccountId(), (long) dto.getCustomerId(),
                        dto.getBalance(), creditLimit);
                    break;
                }
                default:
                    acc = new SavingsAccount(dto.getAccountId(), (long) dto.getCustomerId(), dto.getBalance(), 0.02);
                    break;
            }
            acc.setStatus(EnumAccountStatus.valueOf(dto.getStatus()));
            acc.setDailyWithdrawalLimit(dto.getDailyLimit());
            return acc;
        } catch (Exception e) {
            logger.error("BankCustomer", "buildAccountFromDTO error #" + dto.getAccountId() + ": " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------

    private void startSession() {
        logger.info("BankCustomer", "Session opened by Customer ID="
            + loggedInCustomer.getCustomerId()
            + " (" + loggedInCustomer.getFirstName() + " " + loggedInCustomer.getLastName() + ")");
        session = new SessionManager("Customer", SESSION_TIMEOUT_S, this::onSessionTimeout);
        session.attach(this, SESSION_TIMEOUT_S, null);
    }

    private void onSessionTimeout() {
        JOptionPane.showMessageDialog(this,
            "Your session has expired due to inactivity.\nYou will be returned to the login screen.",
            "Session Timeout", JOptionPane.WARNING_MESSAGE);
        logger.warn("BankCustomer", "Session timed out for Customer ID=" + loggedInCustomer.getCustomerId());
        logout();
    }

    private void logout() {
        session.end();
        logger.info("BankCustomer", "Customer ID=" + loggedInCustomer.getCustomerId()
            + " (" + loggedInCustomer.getFirstName() + " " + loggedInCustomer.getLastName() + ") logged out");
        dispose();
        EventQueue.invokeLater(() -> new banksystem.ui.LoginWindow().setVisible(true));
    }
}
