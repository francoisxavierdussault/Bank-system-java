package banksystem.ui.client2;

import banksystem.business.Account;
import banksystem.business.CheckingAccount;
import banksystem.business.CreditAccount;
import banksystem.business.CurrencyAccount;
import banksystem.business.SavingsAccount;
import banksystem.business.Customer;
import banksystem.business.Transaction;
import banksystem.dao.AccountDAO;
import banksystem.dao.TransactionDAO;
import banksystem.data.DataCollection;
import banksystem.dto.AccountDTO;
import banksystem.dto.CustomerDTO;
import banksystem.dto.TransactionDTO;
import banksystem.enums.EnumAccountStatus;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.service.CustomerService;
import banksystem.util.BankLogger;
import banksystem.util.CsvExporter;
import banksystem.util.PdfExporter;
import banksystem.util.SessionManager;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/**
 * Interface client — transactions et historique des comptes.
 * Ouverte par LoginWindow après une connexion client réussie.
 * N'affiche que les comptes appartenant au client connecté.
 */
public class BankCustomer extends JFrame {

    private static final long    serialVersionUID = 1L;
    private static final BankLogger logger         = BankLogger.getInstance();
    private static final int     SESSION_TIMEOUT_S = 300;

    private final CustomerDTO    loggedInCustomer;
    CustomerService customerService = new CustomerService();
    private final AccountDAO     accountDAO     = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private SessionManager session;

    // sélection du compte
    private JComboBox<String> comboAccount;

    // affichage des informations
    private JLabel lblBalance;
    private JLabel lblAccStatus;
    private JLabel lblDailyLimit;

    // champs de transaction
    private JTextField        txtAmount;
    private JComboBox<String> comboTransferTarget;

    // mouvements (3 dernières transactions par compte)
    private JTable            movTable;
    private DefaultTableModel movModel;

    // historique des transactions
    private JTable            txTable;
    private DefaultTableModel txModel;

    // barre d'état
    private JLabel lblStatusBar;

    // -------------------------------------------------------------------------

    /**
     * Constructeur appelé par LoginWindow après une connexion client réussie.
     *
     * @param loggedInCustomer DTO du client authentifié
     */
    public BankCustomer(CustomerDTO loggedInCustomer) {
        this.loggedInCustomer = loggedInCustomer;
        loadCustomerData();
        initialize();
        loadAccountCombo();
        startSession();
    }

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

    /**
     * Charge les comptes du client connecté depuis Oracle dans DataCollection.
     * Si aucun compte n'existe en base, crée un compte épargne par défaut.
     */
    private void loadCustomerData() {
        int cid = loggedInCustomer.getCustomerId();
        try {
            // Enregistrement du client dans DataCollection si absent
            if (DataCollection.findCustomer(cid) == null) {
                Customer c = new Customer(cid,
                    loggedInCustomer.getFirstName(), loggedInCustomer.getLastName(),
                    loggedInCustomer.getEmail(), loggedInCustomer.getPhone());
                DataCollection.addCustomer(c);
            }

            // Chargement des comptes depuis Oracle
            java.util.List<AccountDTO> dbAccounts = accountDAO.selectByCustomer(cid);

            if (!dbAccounts.isEmpty()) {
                for (AccountDTO dto : dbAccounts) {
                    // Ignoré si déjà chargé (évite les doublons au rechargement)
                    if (DataCollection.findAccount(dto.getAccountId()) != null) {
                        // Mise à jour du solde et du statut depuis la BD
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
                // Aucun compte Oracle — création d'un compte épargne par défaut
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
                default: // SAVINGS (épargne par défaut)
                    acc = new SavingsAccount(dto.getAccountId(), (long) dto.getCustomerId(), dto.getBalance(), 0.02);
                    break;
            }
            acc.setStatus(EnumAccountStatus.valueOf(dto.getStatus()));
            acc.setDailyWithdrawalLimit(dto.getDailyLimit());
            return acc;
        } catch (Exception e) {
            logger.error("BankCustomer", "buildAccountFromDTO error for #" + dto.getAccountId() + ": " + e.getMessage());
            return null;
        }
    }

    private void initialize() {
        String name = loggedInCustomer.getFirstName() + " " + loggedInCustomer.getLastName();
        setTitle("BankCustomer — " + name + "  (ID: " + loggedInCustomer.getCustomerId() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(150, 100, 740, 640);

        JPanel contentPane = new JPanel(new BorderLayout(0, 8));
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(contentPane);

        // Titre et infos utilisateur
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("My Bank Accounts", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 100, 0));
        JLabel lblUser = new JLabel("👤  " + name + "  (ID: " + loggedInCustomer.getCustomerId() + ")",
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
        contentPane.add(titlePanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Tahoma", Font.BOLD, 13));
        tabs.addTab("  Transactions  ", buildTransactionTab());
        tabs.addTab("  Mouvements    ", buildMovementsTab());
        tabs.addTab("  History       ", buildHistoryTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) refreshMovements();
            if (tabs.getSelectedIndex() == 2) refreshHistory();
        });
        contentPane.add(tabs, BorderLayout.CENTER);

        lblStatusBar = new JLabel("  Welcome, " + loggedInCustomer.getFirstName() + "!");
        lblStatusBar.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblStatusBar.setForeground(Color.DARK_GRAY);
        contentPane.add(lblStatusBar, BorderLayout.SOUTH);
    }

    // =========================================================================
    //   Onglet transactions
    // =========================================================================

    private JPanel buildTransactionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel selPanel = new JPanel(new GridBagLayout());
        selPanel.setBorder(new TitledBorder("Account Selection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; selPanel.add(new JLabel("Account :"), gbc);
        gbc.gridx = 1; comboAccount = new JComboBox<>();
        comboAccount.setPrototypeDisplayValue("ACC-9999 (SavingsAccount)      ");
        selPanel.add(comboAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1; selPanel.add(new JLabel("Balance :"), gbc);
        gbc.gridx = 1; lblBalance = new JLabel("$0.00");
        lblBalance.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblBalance.setForeground(new Color(0, 100, 0));
        selPanel.add(lblBalance, gbc);

        gbc.gridx = 0; gbc.gridy = 2; selPanel.add(new JLabel("Status :"), gbc);
        gbc.gridx = 1; lblAccStatus = new JLabel("—");
        lblAccStatus.setFont(new Font("Tahoma", Font.BOLD, 12));
        selPanel.add(lblAccStatus, gbc);

        gbc.gridx = 0; gbc.gridy = 3; selPanel.add(new JLabel("Daily limit left :"), gbc);
        gbc.gridx = 1; lblDailyLimit = new JLabel("—");
        lblDailyLimit.setFont(new Font("Tahoma", Font.PLAIN, 12));
        selPanel.add(lblDailyLimit, gbc);

        panel.add(selPanel, BorderLayout.NORTH);

        JPanel txPanel = new JPanel(new GridBagLayout());
        txPanel.setBorder(new TitledBorder("Do a Transaction"));
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets  = new Insets(10, 16, 10, 16);
        g2.anchor  = GridBagConstraints.WEST;
        g2.fill    = GridBagConstraints.HORIZONTAL;

        // --- Étiquette montant (col 0) ---
        g2.gridx = 0; g2.gridy = 0; g2.weightx = 0;
        JLabel lblAmount = new JLabel("Amount ($) :");
        lblAmount.setFont(new Font("Tahoma", Font.BOLD, 14));
        txPanel.add(lblAmount, g2);

        // --- Champ montant (col 1) ---
        g2.gridx = 1; g2.weightx = 1.0;
        txtAmount = new JTextField();
        txtAmount.setFont(new Font("Tahoma", Font.PLAIN, 16));
        txtAmount.setPreferredSize(new Dimension(260, 38));
        txtAmount.setMinimumSize  (new Dimension(200, 38));
        txtAmount.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 100, 180), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        txtAmount.setForeground(Color.GRAY);
        txtAmount.setText("Enter amount...");
        txtAmount.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtAmount.getText().equals("Enter amount...")) {
                    txtAmount.setText("");
                    txtAmount.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtAmount.getText().trim().isEmpty()) {
                    txtAmount.setForeground(Color.GRAY);
                    txtAmount.setText("Enter amount...");
                }
            }
        });
        txPanel.add(txtAmount, g2);

        // --- Étiquette virement (col 0) ---
        g2.gridx = 0; g2.gridy = 1; g2.weightx = 0;
        JLabel lblTransfer = new JLabel("Transfer to :");
        lblTransfer.setFont(new Font("Tahoma", Font.BOLD, 14));
        txPanel.add(lblTransfer, g2);

        // --- Liste déroulante virement (col 1) ---
        g2.gridx = 1; g2.weightx = 1.0;
        comboTransferTarget = new JComboBox<>();
        comboTransferTarget.setFont(new Font("Tahoma", Font.PLAIN, 13));
        comboTransferTarget.setPreferredSize(new Dimension(260, 36));
        comboTransferTarget.setPrototypeDisplayValue("ACC-9999 (SavingsAccount) — Alice Tremblay   ");
        comboTransferTarget.setToolTipText("Select the destination account for your transfer");
        txPanel.add(comboTransferTarget, g2);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton btnDeposit       = styleBtn(new JButton("Deposit"),          new Color(40, 167, 69));
        JButton btnWithdraw      = styleBtn(new JButton("Withdraw"),         new Color(220, 53, 69));
        JButton btnTransfer      = styleBtn(new JButton("Transfer"),         new Color(70, 130, 180));
        JButton btnApplyInterest = styleBtn(new JButton("Apply Interest"),   new Color(130, 80, 160));
        JButton btnFreezeAccount = styleBtn(new JButton("Freeze/Unfreeze"),  new Color(160, 100, 0));
        JButton btnExportPdf     = styleBtn(new JButton("Export PDF"),       new Color(80, 80, 80));
        btnRow.add(btnDeposit); btnRow.add(btnWithdraw); btnRow.add(btnTransfer);
        btnRow.add(btnApplyInterest); btnRow.add(btnFreezeAccount); btnRow.add(btnExportPdf);

        g2.gridx = 0; g2.gridy = 2; g2.gridwidth = 2; g2.weightx = 1.0;
        g2.anchor = GridBagConstraints.CENTER; g2.fill = GridBagConstraints.NONE;
        txPanel.add(btnRow, g2);
        panel.add(txPanel, BorderLayout.CENTER);

        comboAccount.addActionListener(e -> refreshAccountInfo());
        btnDeposit.addActionListener(e -> doDeposit());
        btnWithdraw.addActionListener(e -> doWithdraw());
        btnTransfer.addActionListener(e -> doTransfer());
        btnApplyInterest.addActionListener(e -> doApplyInterest());
        btnFreezeAccount.addActionListener(e -> doToggleFreeze());
        btnExportPdf.addActionListener(e -> exportTransactionsPdf());

        return panel;
    }

    // =========================================================================
    //   Mouvements tab — 3 dernières transactions par compte (DB)
    // =========================================================================

    private JPanel buildMovementsTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        movModel = new DefaultTableModel(
            new String[]{"Compte", "Type", "Description", "Montant",
                         "Solde avant", "Solde après", "Intérêt", "Pénalité", "Frais", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movTable = new JTable(movModel);
        movTable.setRowHeight(24);
        movTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 11));
        movTable.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(movTable);
        scroll.setBorder(new TitledBorder(
            "Mouvements de compte — 3 dernières transactions par compte"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRefresh = new JButton("Rafraîchir");
        JButton btnCsv     = styleBtn(new JButton("Export CSV (.txt)"), new Color(0, 100, 80));
        JButton btnPdf     = styleBtn(new JButton("Export PDF"),        new Color(80, 80, 80));
        bottom.add(btnRefresh); bottom.add(btnCsv); bottom.add(btnPdf);
        panel.add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshMovements());
        btnCsv.addActionListener(e -> exportMovementsCsv());
        btnPdf.addActionListener(e -> exportMovementsPdf());

        return panel;
    }

    private void refreshMovements() {
        movModel.setRowCount(0);
        int cid = loggedInCustomer.getCustomerId();
        for (Account acc : DataCollection.getAccountsByCustomer(cid)) {
            List<TransactionDTO> recent = transactionDAO.selectRecentByAccount(acc.getAccountNumber());
            for (TransactionDTO tx : recent) {
                movModel.addRow(new Object[]{
                    "ACC-" + tx.getAccountId(),
                    tx.getTransactionType(),
                    tx.getDescription(),
                    String.format("$%.2f", tx.getAmount()),
                    String.format("$%.2f", tx.getBalanceBefore()),
                    String.format("$%.2f", tx.getBalanceAfter()),
                    tx.getInterestGain()  > 0 ? String.format("+$%.2f", tx.getInterestGain())  : "-",
                    tx.getPenaltyAmount() > 0 ? String.format("-$%.2f", tx.getPenaltyAmount()) : "-",
                    tx.getFeeApplied()    > 0 ? String.format("-$%.2f", tx.getFeeApplied())    : "-",
                    tx.getTransactionDate()
                });
            }
        }
    }

    private void exportMovementsCsv() {
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < movModel.getRowCount(); r++) {
            String[] row = new String[movModel.getColumnCount()];
            for (int c = 0; c < row.length; c++) row[c] = movModel.getValueAt(r, c).toString();
            rows.add(row);
        }
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun mouvement à exporter."); return; }
        String[] headers = {"Compte","Type","Description","Montant",
                            "Solde avant","Solde après","Intérêt","Pénalité","Frais","Date"};
        String name = loggedInCustomer.getFirstName() + "_" + loggedInCustomer.getLastName();
        try {
            String path = CsvExporter.export("Mouvements_" + name, headers, rows, "reports");
            JOptionPane.showMessageDialog(this, "Fichier CSV (.txt) sauvegardé :\n" + path,
                "Export réussi", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            logger.error("BankCustomer", "CSV export error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur d'export : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportMovementsPdf() {
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < movModel.getRowCount(); r++) {
            String[] row = new String[movModel.getColumnCount()];
            for (int c = 0; c < row.length; c++) row[c] = movModel.getValueAt(r, c).toString();
            rows.add(row);
        }
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun mouvement à exporter."); return; }
        String name = loggedInCustomer.getFirstName() + "_" + loggedInCustomer.getLastName();
        runPdfExport("Mouvements_" + name,
            new String[]{"Compte","Type","Description","Montant",
                         "Solde avant","Solde après","Intérêt","Pénalité","Frais","Date"}, rows);
    }

    // =========================================================================
    //   Onglet historique
    // =========================================================================

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        txModel = new DefaultTableModel(
            new String[]{"Account", "Type", "Description", "Amount", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        txTable = new JTable(txModel);
        txTable.setRowHeight(24);
        txTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        txTable.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(txTable);
        scroll.setBorder(new TitledBorder("Transaction History — My Accounts"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRefresh   = new JButton("Refresh");
        JButton btnExportCsv = styleBtn(new JButton("Export CSV (.txt)"), new Color(0, 100, 80));
        JButton btnExportPdf = styleBtn(new JButton("Export PDF"),        new Color(80, 80, 80));
        bottom.add(btnRefresh); bottom.add(btnExportCsv); bottom.add(btnExportPdf);
        panel.add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshHistory());
        btnExportCsv.addActionListener(e -> exportHistoryCsv());
        btnExportPdf.addActionListener(e -> exportHistoryPdf());

        return panel;
    }

    // =========================================================================
    //   Chargement des listes et rafraîchissement
    // =========================================================================

    private void loadAccountCombo() {
        comboAccount.removeAllItems();
        comboTransferTarget.removeAllItems();
        int cid = loggedInCustomer.getCustomerId();

        // Mes comptes — depuis DataCollection (déjà chargés depuis Oracle)
        for (Account acc : DataCollection.getAccountsByCustomer(cid)) {
            comboAccount.addItem("ACC-" + acc.getAccountNumber()
                + " (" + acc.getClass().getSimpleName() + ")");
        }

        // Destinations de virement — tous les comptes Oracle sauf les miens
        List<AccountDTO> allAccounts = accountDAO.selectAll();
        for (AccountDTO dto : allAccounts) {
            if (dto.getCustomerId() == cid) continue; // ignorer mes propres comptes
            if (!dto.getStatus().equals("ACTIVE"))    continue; // ignorer les comptes gelés/fermés

            // Recherche du nom du propriétaire du compte
            CustomerDTO owner = customerService.searchCustomer(dto.getCustomerId());
            String ownerName = (owner != null)
                ? owner.getFirstName() + " " + owner.getLastName()
                : "Customer #" + dto.getCustomerId();

            comboTransferTarget.addItem(
                "ACC-" + dto.getAccountId()
                + " (" + dto.getAccountType() + ")"
                + " — " + ownerName);
        }

        if (comboTransferTarget.getItemCount() == 0) {
            comboTransferTarget.addItem("— No other accounts available —");
        }

        refreshAccountInfo();
    }

    private void refreshAccountInfo() {
        Account acc = getSelectedAccount();
        if (acc == null) {
            lblBalance.setText("$0.00"); lblAccStatus.setText("—"); lblDailyLimit.setText("—"); return;
        }
        lblBalance.setText(String.format("$%.2f", acc.getBalance()));

        EnumAccountStatus st = acc.getStatus();
        lblAccStatus.setText(st.name());
        lblAccStatus.setForeground(st == EnumAccountStatus.ACTIVE  ? new Color(0, 128, 0)
                                 : st == EnumAccountStatus.FROZEN ? new Color(200, 130, 0)
                                 :                                   Color.RED);

        double remaining = acc.getDailyWithdrawalLimit() - acc.getDailyWithdrawnToday();
        lblDailyLimit.setText(String.format("$%.2f  (limit $%.2f / day)", remaining, acc.getDailyWithdrawalLimit()));
        lblDailyLimit.setForeground(remaining < 500 ? new Color(180, 80, 0) : Color.DARK_GRAY);
    }

    private void refreshHistory() {
        txModel.setRowCount(0);
        int cid = loggedInCustomer.getCustomerId();
        List<TransactionDTO> all = transactionDAO.selectAllByCustomer(cid);
        for (TransactionDTO tx : all) {
            txModel.addRow(new Object[]{
                "ACC-" + tx.getAccountId(),
                tx.getTransactionType(),
                tx.getDescription(),
                String.format("$%.2f", tx.getAmount()),
                tx.getTransactionDate()
            });
        }
    }

    // =========================================================================
    //   Opérations de transaction
    // =========================================================================

    private void doDeposit() {
        Account acc = getSelectedAccount();
        if (acc == null) { showError("Please select an account."); return; }
        double amount = parseAmount(); if (amount <= 0) return;
        try {
            double balBefore = acc.getBalance();
            acc.deposit(amount);
            double balAfter = acc.getBalance();
            accountDAO.updateBalance(acc.getAccountNumber(), balAfter);
            recordTx(acc, "DEPOSIT", "Dépôt", amount, balBefore, balAfter, 0, 0, 0);
            refreshAccountInfo();
            setStatusBar("Deposited $" + String.format("%.2f", amount) + " successfully.", new Color(0, 128, 0));
        } catch (InsufficientFundException ex) {
            showError(ex.getMessage());
            logger.warn("BankCustomer", "Deposit blocked: " + ex.getMessage());
        } catch (NotInRangeException ex) {
            showError(ex.getMessage());
        }
    }

    private void doWithdraw() {
        Account acc = getSelectedAccount();
        if (acc == null) { showError("Please select an account."); return; }
        double amount = parseAmount(); if (amount <= 0) return;
        try {
            double balBefore = acc.getBalance();
            acc.withdraw(amount);
            double balAfter = acc.getBalance();
            accountDAO.updateBalance(acc.getAccountNumber(), balAfter);
            double fee = balBefore - balAfter - amount; // frais éventuels (compte chèque hors limite)
            if (fee < 0.001) fee = 0;
            recordTx(acc, "WITHDRAWAL", "Retrait", amount, balBefore, balAfter, 0, 0, fee);
            refreshAccountInfo();
            setStatusBar("Withdrew $" + String.format("%.2f", amount) + " successfully.", new Color(0, 128, 0));
        } catch (InsufficientFundException ex) {
            showError(ex.getMessage());
            logger.warn("BankCustomer", "Withdrawal blocked: " + ex.getMessage());
        } catch (NotInRangeException ex) {
            showError(ex.getMessage());
        }
    }

    private void doTransfer() {
        Account source = getSelectedAccount();
        if (source == null) { showError("Please select an account to transfer FROM."); return; }

        String targetItem = (String) comboTransferTarget.getSelectedItem();
        if (targetItem == null || targetItem.startsWith("—")) {
            showError("Please select a valid destination account."); return;
        }

        Account target = getSelectedTransferTarget();
        if (target == null) { showError("Destination account could not be loaded. Please try again."); return; }
        if (source.getAccountNumber() == target.getAccountNumber()) {
            showError("Source and destination accounts must be different."); return;
        }

        double amount = parseAmount(); if (amount <= 0) return;

        // Recherche du nom du destinataire pour confirmation
        CustomerDTO recipientDTO = customerService.searchCustomer(target.getCustomerNumber().intValue());
        String recipientName = (recipientDTO != null)
            ? recipientDTO.getFirstName() + " " + recipientDTO.getLastName()
            : "Customer #" + target.getCustomerNumber();

        // Boîte de dialogue de confirmation avec les détails
        String confirmMsg = String.format(
            "<html><b>Confirm Transfer</b><br><br>"
            + "From  :  <b>ACC-%d</b>  (balance: $%.2f)<br>"
            + "To     :  <b>ACC-%d</b>  (%s)<br>"
            + "Recipient : <b>%s</b><br><br>"
            + "Amount : <b>$%.2f</b><br><br>"
            + "Are you sure?</html>",
            source.getAccountNumber(), source.getBalance(),
            target.getAccountNumber(), target.getClass().getSimpleName(),
            recipientName, amount);

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg,
            "Confirm Transfer", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            double srcBefore = source.getBalance();
            double tgtBefore = target.getBalance();
            source.transfert(amount, target);
            double srcAfter = source.getBalance();
            double tgtAfter = target.getBalance();
            accountDAO.updateBalance(source.getAccountNumber(), srcAfter); // persistance source
            accountDAO.updateBalance(target.getAccountNumber(), tgtAfter); // persistance cible
            double fee = srcBefore - srcAfter - amount;
            if (fee < 0.001) fee = 0;
            recordTx(source, "TRANSFER", "Virement vers ACC-" + target.getAccountNumber(), amount, srcBefore, srcAfter, 0, 0, fee);
            recordTx(target, "TRANSFER", "Virement reçu de ACC-" + source.getAccountNumber(), amount, tgtBefore, tgtAfter, 0, 0, 0);
            refreshAccountInfo();
            logger.info("BankCustomer", "Transfer $" + String.format("%.2f", amount)
                + " from ACC-" + source.getAccountNumber()
                + " to ACC-" + target.getAccountNumber() + " (" + recipientName + ")");
            setStatusBar(String.format("✓ Transferred $%.2f to %s (ACC-%d) successfully.",
                amount, recipientName, target.getAccountNumber()), new Color(0, 128, 0));
        } catch (InsufficientFundException ex) {
            showError(ex.getMessage());
            logger.warn("BankCustomer", "Transfer blocked: " + ex.getMessage());
        } catch (NotInRangeException ex) {
            showError(ex.getMessage());
        }
    }

    private void doApplyInterest() {
        Account acc = getSelectedAccount();
        if (acc == null) { showError("Please select an account."); return; }
        if (!(acc instanceof SavingsAccount)) { showError("Interest can only be applied to Savings accounts."); return; }
        if (JOptionPane.showConfirmDialog(this, "Apply interest to ACC-" + acc.getAccountNumber() + " ?",
            "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        double balBefore = acc.getBalance();
        SavingsAccount sav = (SavingsAccount) acc;
        sav.applyInterest();
        double balAfter = acc.getBalance();
        double interest = balAfter - balBefore;
        accountDAO.updateBalance(acc.getAccountNumber(), balAfter);
        recordTx(acc, "INTEREST", "Intérêts appliqués", interest, balBefore, balAfter, interest, 0, 0);
        refreshAccountInfo();
        setStatusBar("Interest applied to ACC-" + acc.getAccountNumber() + ".", new Color(100, 0, 150));
    }

    private void doToggleFreeze() {
        Account acc = getSelectedAccount();
        if (acc == null) { showError("Please select an account."); return; }
        if (acc.getStatus() == EnumAccountStatus.CLOSED) { showError("Account is permanently closed."); return; }
        boolean isFrozen = acc.getStatus() == EnumAccountStatus.FROZEN;
        String action = isFrozen ? "unfreeze" : "freeze";
        if (JOptionPane.showConfirmDialog(this, "Do you want to " + action
            + " ACC-" + acc.getAccountNumber() + " ?", "Confirm",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        EnumAccountStatus newStatus = isFrozen ? EnumAccountStatus.ACTIVE : EnumAccountStatus.FROZEN;
        acc.setStatus(newStatus);
        accountDAO.updateStatus(acc.getAccountNumber(), newStatus.name()); // persist to Oracle
        logger.info("BankCustomer", "Account#" + acc.getAccountNumber() + " status -> " + acc.getStatus());
        refreshAccountInfo();
        setStatusBar("ACC-" + acc.getAccountNumber() + " is now " + acc.getStatus() + ".",
            isFrozen ? new Color(0, 128, 0) : new Color(180, 80, 0));
    }

    // =========================================================================
    //   Exportation PDF
    // =========================================================================

    private void exportTransactionsPdf() {
        Account acc = getSelectedAccount();
        if (acc == null) { showError("Please select an account first."); return; }
        List<String[]> rows = new ArrayList<>();
        for (Transaction tx : acc.getTransactions()) {
            rows.add(new String[]{
                tx.getTransactionType().name(), tx.getDescription(),
                String.format("$%.2f", tx.getAmount()), tx.getTransactionDate().toString()
            });
        }
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "No transactions to export."); return; }
        runPdfExport("Transactions_ACC-" + acc.getAccountNumber(),
            new String[]{"Type", "Description", "Amount", "Date"}, rows);
    }

    private static final String[] HISTORY_HEADERS = {
        "Compte", "Type", "Description", "Montant",
        "Solde avant", "Solde après", "Intérêt", "Pénalité", "Frais", "Date"
    };

    private List<String[]> buildHistoryRows() {
        List<String[]> rows = new ArrayList<>();
        List<TransactionDTO> all = transactionDAO.selectAllByCustomer(loggedInCustomer.getCustomerId());
        for (TransactionDTO tx : all) {
            rows.add(new String[]{
                "ACC-" + tx.getAccountId(),
                tx.getTransactionType(),
                tx.getDescription() != null ? tx.getDescription() : "",
                String.format("$%.2f", tx.getAmount()),
                String.format("$%.2f", tx.getBalanceBefore()),
                String.format("$%.2f", tx.getBalanceAfter()),
                tx.getInterestGain()  > 0 ? String.format("+$%.2f", tx.getInterestGain())  : "-",
                tx.getPenaltyAmount() > 0 ? String.format("-$%.2f", tx.getPenaltyAmount()) : "-",
                tx.getFeeApplied()    > 0 ? String.format("-$%.2f", tx.getFeeApplied())    : "-",
                tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : ""
            });
        }
        return rows;
    }

    private void exportHistoryCsv() {
        List<String[]> rows = buildHistoryRows();
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun historique à exporter."); return; }
        String name = loggedInCustomer.getFirstName() + "_" + loggedInCustomer.getLastName();
        try {
            String path = CsvExporter.export("History_" + name, HISTORY_HEADERS, rows, "reports");
            JOptionPane.showMessageDialog(this, "CSV (.txt) sauvegardé :\n" + path,
                "Export réussi", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            logger.error("BankCustomer", "Erreur export CSV historique : " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Erreur d'export : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportHistoryPdf() {
        List<String[]> rows = buildHistoryRows();
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun historique à exporter."); return; }
        String name = loggedInCustomer.getFirstName() + "_" + loggedInCustomer.getLastName();
        runPdfExport("History_" + name, HISTORY_HEADERS, rows);
    }

    private void runPdfExport(String title, String[] headers, List<String[]> rows) {
        try {
            String path = PdfExporter.export(title, headers, rows, "reports");
            JOptionPane.showMessageDialog(this, "PDF saved to:\n" + path, "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("BankCustomer", "PDF export failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "PDF export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    //   Utilitaires
    // =========================================================================

    private double parseAmount() {
        String text = txtAmount.getText().trim();
        if (text.isEmpty() || text.equals("Enter amount...")) { showError("Please enter an amount."); return -1; }
        try {
            double amount = Double.parseDouble(text);
            if (amount <= 0) { showError("Amount must be greater than zero."); return -1; }
            return amount;
        } catch (NumberFormatException e) { showError("Amount must be a valid number."); return -1; }
    }

    private Account getSelectedAccount() {
        String item = (String) comboAccount.getSelectedItem();
        if (item == null) return null;
        long accNum = Long.parseLong(item.replace("ACC-", "").split(" ")[0]);
        return DataCollection.findAccount(accNum);
    }

    /**
     * Returns the Account for the selected transfer target.
     * If it isn't loaded in DataCollection yet, fetches it from Oracle first.
     */
    private Account getSelectedTransferTarget() {
        String item = (String) comboTransferTarget.getSelectedItem();
        if (item == null || item.startsWith("—")) return null;
        try {
            long accNum = Long.parseLong(item.replace("ACC-", "").split(" ")[0]);

            // Chercher dans DataCollection d'abord
            Account acc = DataCollection.findAccount(accNum);
            if (acc != null) return acc;

            // Pas encore chargé — récupération depuis Oracle
            AccountDTO dto = accountDAO.selectById(accNum);
            if (dto == null) return null;

            acc = buildAccountFromDTO(dto);
            if (acc != null) DataCollection.addAccount(acc);
            return acc;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Insère une transaction dans la table Transaction pour qu'elle apparaisse dans l'onglet Mouvements.
     */
    private void recordTx(Account acc, String type, String description,
                          double amount, double balBefore, double balAfter,
                          double interestGain, double penaltyAmount, double feeApplied) {
        try {
            TransactionDTO tx = new TransactionDTO();
            tx.setAccountId(acc.getAccountNumber());
            tx.setCustomerId(loggedInCustomer.getCustomerId());
            tx.setTransactionType(type);
            tx.setDescription(description);
            tx.setAmount(amount);
            tx.setBalanceBefore(balBefore);
            tx.setBalanceAfter(balAfter);
            tx.setInterestGain(interestGain);
            tx.setPenaltyAmount(penaltyAmount);
            tx.setFeeApplied(feeApplied);
            tx.setTransactionDate(new java.util.Date());
            transactionDAO.insert(tx);
        } catch (Exception e) {
            logger.error("BankCustomer", "Erreur enregistrement transaction : " + e.getMessage());
        }
    }

    private void setStatusBar(String msg, Color color) { lblStatusBar.setText("  " + msg); lblStatusBar.setForeground(color); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JButton styleBtn(JButton btn, Color color) {
        btn.setBackground(color); btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Tahoma", Font.BOLD, 12));
        btn.setOpaque(true); btn.setBorderPainted(false);
        return btn;
    }
}
