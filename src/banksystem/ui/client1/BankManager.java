package banksystem.ui.client1;

import banksystem.dao.AccountDAO;
import banksystem.dto.AccountDTO;
import banksystem.dto.CustomerDTO;
import banksystem.dto.ManagerDTO;
import banksystem.service.CustomerService;
import banksystem.service.ManagerService;
import banksystem.util.BankLogger;
import banksystem.util.PdfExporter;
import banksystem.util.SessionManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/**
 * Interface gestionnaire — gestion de la base de données des gestionnaires et clients.
 * Ouverte automatiquement par LoginWindow après une connexion réussie en tant que gestionnaire.
 */
public class BankManager extends JFrame {

    private static final long    serialVersionUID  = 1L;
    private static final BankLogger logger          = BankLogger.getInstance();
    private static final int     SESSION_TIMEOUT_S  = 600;

    private final ManagerDTO    loggedInManager;
    ManagerService  managerService  = new ManagerService();
    CustomerService customerService = new CustomerService();
    AccountDAO      accountDAO      = new AccountDAO();

    private SessionManager session;

    // --- Onglet gestionnaire ---
    private JTextField        txtMgrId, txtMgrName, txtMgrEmail, txtMgrPhone;
    private JLabel            lblMgrIdErr, lblMgrNameErr;
    private JTable            mgrTable;
    private DefaultTableModel mgrModel;
    private JLabel            lblMgrStatus;

    // --- Onglet client ---
    private JTextField        txtCustId, txtCustFirstName, txtCustLastName, txtCustEmail, txtCustPhone;
    private JTextField        txtCustSearch;
    private JComboBox<String> comboManager;
    private JLabel            lblCustIdErr, lblCustFirstNameErr, lblCustLastNameErr;
    private JTable            custTable;
    private DefaultTableModel custModel;
    private JLabel            lblCustStatus;

    // --- Onglet comptes ---
    private JTextField        txtAccId, txtAccBalance, txtAccDailyLimit;
    private JComboBox<String> comboAccCustomer, comboAccType;
    private JTable            accTable;
    private DefaultTableModel accModel;
    private JLabel            lblAccStatus;

    // --- Onglet journal ---
    private JTextArea logTextArea;

    // -------------------------------------------------------------------------

    /**
     * Constructeur appelé par LoginWindow après une connexion gestionnaire réussie.
     *
     * @param loggedInManager DTO du gestionnaire authentifié
     */
    public BankManager(ManagerDTO loggedInManager) {
        this.loggedInManager = loggedInManager;
        initialize();
        mgrSelectAll();
        custSelectAll();
        startSession();
    }

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

    private void initialize() {
        setTitle("BankManager — Logged in as: " + loggedInManager.getManagerName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 80, 900, 680);

        JPanel contentPane = new JPanel(new BorderLayout(0, 6));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // Barre de titre avec les informations de connexion
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Bank Manager - Database Management", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 70, 127));
        JLabel lblUser = new JLabel("👤  " + loggedInManager.getManagerName()
                + "  (ID: " + loggedInManager.getManagerId() + ")", SwingConstants.RIGHT);
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

        titlePanel.add(lblTitle,   BorderLayout.CENTER);
        titlePanel.add(eastPanel,  BorderLayout.EAST);
        contentPane.add(titlePanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Tahoma", Font.BOLD, 13));
        tabbedPane.addTab("  Manager   ", buildManagerTab());
        tabbedPane.addTab("  Customer  ", buildCustomerTab());
        tabbedPane.addTab("  Accounts  ", buildAccountsTab());
        tabbedPane.addTab("  Log Viewer", buildLogViewerTab());

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 1) loadManagerCombo();
            if (idx == 2) accSelectAll();
            if (idx == 3) refreshLogViewer();
        });

        contentPane.add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    //   Onglet gestionnaire
    // =========================================================================

    private JPanel buildManagerTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Manager Data"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 2, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Manager ID :"), gbc);
        gbc.gridx = 1; txtMgrId = new JTextField(10); formPanel.add(txtMgrId, gbc);
        gbc.gridx = 2; lblMgrIdErr = errLabel(); formPanel.add(lblMgrIdErr, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Manager Name :"), gbc);
        gbc.gridx = 1; txtMgrName = new JTextField(25); formPanel.add(txtMgrName, gbc);
        gbc.gridx = 2; lblMgrNameErr = errLabel(); formPanel.add(lblMgrNameErr, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Email :"), gbc);
        gbc.gridx = 1; txtMgrEmail = new JTextField(25); formPanel.add(txtMgrEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Phone :"), gbc);
        gbc.gridx = 1; txtMgrPhone = new JTextField(15); formPanel.add(txtMgrPhone, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton btnSelectAll = styleBtn(new JButton("Select All"), new Color(70, 130, 180));
        JButton btnInsert    = styleBtn(new JButton("Insert"),     new Color(40, 167, 69));
        JButton btnUpdate    = styleBtn(new JButton("Update"),     new Color(255, 140, 0));
        JButton btnDelete    = styleBtn(new JButton("Delete"),     new Color(220, 53, 69));
        JButton btnSearch    = styleBtn(new JButton("Search"),     new Color(100, 100, 200));
        JButton btnExportPdf = styleBtn(new JButton("Export PDF"), new Color(80, 80, 80));
        JButton btnClear     = new JButton("Clear");
        btnPanel.add(btnSelectAll); btnPanel.add(btnInsert); btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);    btnPanel.add(btnSearch);
        btnPanel.add(btnExportPdf); btnPanel.add(btnClear);

        // Seul le gestionnaire ID=1 (admin) peut créer de nouveaux gestionnaires
        boolean isAdmin = loggedInManager.getManagerId() == 1;
        btnInsert.setEnabled(isAdmin);
        btnInsert.setToolTipText(isAdmin
            ? "Insert a new manager"
            : "Only the admin manager (ID=1) can create new managers");

        mgrModel = new DefaultTableModel(
            new String[]{"Manager ID", "Manager Name", "Email", "Phone"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        mgrTable = new JTable(mgrModel);
        mgrTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mgrTable.setRowHeight(24);
        mgrTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        mgrTable.setFont(new Font("Tahoma", Font.PLAIN, 12));

        mgrTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && mgrTable.getSelectedRow() >= 0) {
                int row = mgrTable.getSelectedRow();
                txtMgrId.setText(mgrModel.getValueAt(row, 0).toString());
                txtMgrName.setText(mgrModel.getValueAt(row, 1).toString());
                txtMgrEmail.setText(mgrModel.getValueAt(row, 2).toString());
                txtMgrPhone.setText(mgrModel.getValueAt(row, 3).toString());
                clearInlineErrors(lblMgrIdErr, lblMgrNameErr);
            }
        });

        JScrollPane scroll = new JScrollPane(mgrTable);
        scroll.setBorder(new TitledBorder("Manager List"));

        lblMgrStatus = new JLabel("  Ready");
        lblMgrStatus.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblMgrStatus.setForeground(Color.DARK_GRAY);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(btnPanel,     BorderLayout.NORTH);
        center.add(scroll,       BorderLayout.CENTER);
        center.add(lblMgrStatus, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        btnSelectAll.addActionListener(e -> mgrSelectAll());
        btnInsert.addActionListener(e -> mgrInsert());
        btnUpdate.addActionListener(e -> mgrUpdate());
        btnDelete.addActionListener(e -> mgrDelete());
        btnSearch.addActionListener(e -> mgrSearch());
        btnClear.addActionListener(e -> mgrClear());
        btnExportPdf.addActionListener(e -> exportManagersPdf());

        return panel;
    }

    // =========================================================================
    //   Onglet client
    // =========================================================================

    private JPanel buildCustomerTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Customer Data"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 2, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Customer ID :"), gbc);
        gbc.gridx = 1; txtCustId = new JTextField(10); formPanel.add(txtCustId, gbc);
        gbc.gridx = 2; lblCustIdErr = errLabel(); formPanel.add(lblCustIdErr, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("First Name :"), gbc);
        gbc.gridx = 1; txtCustFirstName = new JTextField(20); formPanel.add(txtCustFirstName, gbc);
        gbc.gridx = 2; lblCustFirstNameErr = errLabel(); formPanel.add(lblCustFirstNameErr, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Last Name :"), gbc);
        gbc.gridx = 1; txtCustLastName = new JTextField(20); formPanel.add(txtCustLastName, gbc);
        gbc.gridx = 2; lblCustLastNameErr = errLabel(); formPanel.add(lblCustLastNameErr, gbc);

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Email :"), gbc);
        gbc.gridx = 1; txtCustEmail = new JTextField(25); formPanel.add(txtCustEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Phone :"), gbc);
        gbc.gridx = 1; txtCustPhone = new JTextField(15); formPanel.add(txtCustPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Manager :"), gbc);
        gbc.gridx = 1; comboManager = new JComboBox<>();
        comboManager.setPrototypeDisplayValue("99 - Manager Name Placeholder   ");
        formPanel.add(comboManager, gbc);

        gbc.gridx = 0; gbc.gridy = 6; formPanel.add(new JLabel("Search name :"), gbc);
        gbc.gridx = 1;
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        txtCustSearch = new JTextField(16);
        JButton btnSearchName = styleBtn(new JButton("Search by Name"), new Color(100, 100, 200));
        searchRow.add(txtCustSearch); searchRow.add(btnSearchName);
        formPanel.add(searchRow, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton btnSelectAll = styleBtn(new JButton("Select All"), new Color(70, 130, 180));
        JButton btnInsert    = styleBtn(new JButton("Insert"),     new Color(40, 167, 69));
        JButton btnUpdate    = styleBtn(new JButton("Update"),     new Color(255, 140, 0));
        JButton btnDelete    = styleBtn(new JButton("Delete"),     new Color(220, 53, 69));
        JButton btnSearchId  = styleBtn(new JButton("Search by ID"), new Color(100, 100, 200));
        JButton btnExportPdf = styleBtn(new JButton("Export PDF"),   new Color(80, 80, 80));
        JButton btnClear     = new JButton("Clear");
        btnPanel.add(btnSelectAll); btnPanel.add(btnInsert); btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);    btnPanel.add(btnSearchId);
        btnPanel.add(btnExportPdf); btnPanel.add(btnClear);

        custModel = new DefaultTableModel(
            new String[]{"Customer ID", "First Name", "Last Name", "Email", "Phone", "Manager ID"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        custTable = new JTable(custModel);
        custTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        custTable.setRowHeight(24);
        custTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        custTable.setFont(new Font("Tahoma", Font.PLAIN, 12));

        custTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && custTable.getSelectedRow() >= 0) {
                int row = custTable.getSelectedRow();
                txtCustId.setText(custModel.getValueAt(row, 0).toString());
                txtCustFirstName.setText(custModel.getValueAt(row, 1).toString());
                txtCustLastName.setText(custModel.getValueAt(row, 2).toString());
                txtCustEmail.setText(custModel.getValueAt(row, 3).toString());
                txtCustPhone.setText(custModel.getValueAt(row, 4).toString());
                String mgrId = custModel.getValueAt(row, 5).toString();
                for (int i = 0; i < comboManager.getItemCount(); i++) {
                    if (comboManager.getItemAt(i).startsWith(mgrId + " ")) {
                        comboManager.setSelectedIndex(i); break;
                    }
                }
                clearInlineErrors(lblCustIdErr, lblCustFirstNameErr, lblCustLastNameErr);
            }
        });

        JScrollPane scroll = new JScrollPane(custTable);
        scroll.setBorder(new TitledBorder("Customer List"));

        lblCustStatus = new JLabel("  Ready");
        lblCustStatus.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblCustStatus.setForeground(Color.DARK_GRAY);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(btnPanel,      BorderLayout.NORTH);
        center.add(scroll,        BorderLayout.CENTER);
        center.add(lblCustStatus, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        btnSelectAll.addActionListener(e -> custSelectAll());
        btnInsert.addActionListener(e -> custInsert());
        btnUpdate.addActionListener(e -> custUpdate());
        btnDelete.addActionListener(e -> custDelete());
        btnSearchId.addActionListener(e -> custSearch());
        btnSearchName.addActionListener(e -> custSearchByName());
        btnClear.addActionListener(e -> custClear());
        btnExportPdf.addActionListener(e -> exportCustomersPdf());

        return panel;
    }

    // =========================================================================
    //   Onglet journal
    // =========================================================================

    private JPanel buildLogViewerTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logTextArea.setBackground(new Color(30, 30, 30));
        logTextArea.setForeground(new Color(180, 255, 180));

        JScrollPane scroll = new JScrollPane(logTextArea);
        scroll.setBorder(new TitledBorder("Today's Log — " + todayLabel()));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRefresh = styleBtn(new JButton("Refresh"),    new Color(70, 130, 180));
        JButton btnExport  = styleBtn(new JButton("Export PDF"), new Color(80, 80, 80));
        JButton btnClear   = new JButton("Clear View");
        bottom.add(btnRefresh); bottom.add(btnExport); bottom.add(btnClear);
        panel.add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshLogViewer());
        btnClear.addActionListener(e -> logTextArea.setText(""));
        btnExport.addActionListener(e -> exportLogPdf());

        return panel;
    }

    private void refreshLogViewer() {
        String today    = todayLabel();
        String filePath = "logs" + File.separator + "log_" + today + ".txt";
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            logTextArea.setText(content);
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        } catch (IOException e) {
            logTextArea.setText("No log file found for today (" + filePath + ")");
        }
    }

    private static String todayLabel() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    // =========================================================================
    //   Exportation PDF
    // =========================================================================

    private void exportManagersPdf() {
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < mgrModel.getRowCount(); r++) {
            rows.add(new String[]{
                mgrModel.getValueAt(r, 0).toString(),
                mgrModel.getValueAt(r, 1).toString(),
                mgrModel.getValueAt(r, 2).toString(),
                mgrModel.getValueAt(r, 3).toString()
            });
        }
        runPdfExport("Manager_List",
            new String[]{"Manager ID", "Manager Name", "Email", "Phone"},
            rows, lblMgrStatus);
    }

    private void exportCustomersPdf() {
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < custModel.getRowCount(); r++) {
            rows.add(new String[]{
                custModel.getValueAt(r, 0).toString(),
                custModel.getValueAt(r, 1).toString(),
                custModel.getValueAt(r, 2).toString(),
                custModel.getValueAt(r, 3).toString(),
                custModel.getValueAt(r, 4).toString(),
                custModel.getValueAt(r, 5).toString()
            });
        }
        runPdfExport("Customer_List",
            new String[]{"ID", "First Name", "Last Name", "Email", "Phone", "Manager ID"},
            rows, lblCustStatus);
    }

    private void exportLogPdf() {
        String text = logTextArea.getText();
        if (text.isBlank()) { JOptionPane.showMessageDialog(this, "Log is empty."); return; }

        // Analyse chaque ligne : Horodatage | Niveau | Source | Message
        // Format : [2026-05-14 12:32:48] [INFO] [CustomerService] message
        String[] headers = {"Timestamp", "Level", "Source", "Message"};
        float[]  widths  = {130f, 45f, 110f, 210f}; // total = 495pt (fits A4 with 40pt margins)
        List<String[]> rows = new ArrayList<>();

        for (String line : text.split("\n")) {
            if (line.trim().isEmpty()) continue;
            rows.add(parseLogLine(line));
        }
        try {
            String path = PdfExporter.export("Log_" + todayLabel(), headers, rows, "reports", widths);
            JOptionPane.showMessageDialog(this, "PDF saved to:\n" + path,
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("BankManager", "Log PDF export failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "PDF export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Analyse une ligne de journal de la forme :
     *   [2026-05-14 12:32:48] [INFO] [CustomerService] message
     * Retourne String[]{horodatage, niveau, source, message}
     */
    private String[] parseLogLine(String line) {
        try {
            int ts1 = line.indexOf('[') + 1;
            int ts2 = line.indexOf(']', ts1);
            int lv1 = line.indexOf('[', ts2) + 1;
            int lv2 = line.indexOf(']', lv1);
            int sr1 = line.indexOf('[', lv2) + 1;
            int sr2 = line.indexOf(']', sr1);
            String timestamp = line.substring(ts1, ts2).trim();
            String level     = line.substring(lv1, lv2).trim();
            String source    = line.substring(sr1, sr2).trim();
            String message   = line.substring(sr2 + 1).trim();
            return new String[]{timestamp, level, source, message};
        } catch (Exception e) {
            return new String[]{line, "", "", ""};
        }
    }

    private void runPdfExport(String title, String[] headers, List<String[]> rows, JLabel statusLabel) {
        try {
            String path = PdfExporter.export(title, headers, rows, "reports");
            String msg  = "PDF exported: " + path;
            if (statusLabel != null) setStatus(statusLabel, msg, new Color(0, 100, 0));
            JOptionPane.showMessageDialog(this, "PDF saved to:\n" + path, "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            logger.error("BankManager", "PDF export failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "PDF export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    //   CRUD gestionnaire
    // =========================================================================

    private void mgrSelectAll() {
        mgrModel.setRowCount(0);
        for (ManagerDTO dto : managerService.getAllManagers()) {
            mgrModel.addRow(new Object[]{ dto.getManagerId(), dto.getManagerName(), dto.getEmail(), dto.getPhone() });
        }
        setStatus(lblMgrStatus, "Loaded " + mgrModel.getRowCount() + " manager(s)", Color.DARK_GRAY);
    }

    private void mgrSearch() {
        String idText = txtMgrId.getText().trim();
        if (idText.isEmpty()) { setInlineError(lblMgrIdErr, "Required"); return; }
        clearInlineErrors(lblMgrIdErr);
        try {
            int id = Integer.parseInt(idText);
            ManagerDTO dto = managerService.searchManager(id);
            if (dto != null) {
                txtMgrName.setText(dto.getManagerName());
                txtMgrEmail.setText(dto.getEmail());
                txtMgrPhone.setText(dto.getPhone());
                setStatus(lblMgrStatus, "Manager " + id + " found.", new Color(0, 128, 0));
            } else {
                setStatus(lblMgrStatus, "No manager found with ID = " + id, Color.ORANGE);
            }
        } catch (NumberFormatException ex) { setInlineError(lblMgrIdErr, "Must be a number"); }
    }

    private void mgrInsert() {
        if (loggedInManager.getManagerId() != 1) {
            JOptionPane.showMessageDialog(this,
                "Access denied — only the admin manager (ID=1) can create new managers.",
                "Insufficient Privileges", JOptionPane.WARNING_MESSAGE);
            logger.warn("BankManager", "Unauthorized insert attempt by Manager ID="
                    + loggedInManager.getManagerId());
            return;
        }
        if (!validateMgrInline()) return;
        ManagerDTO dto = new ManagerDTO(Integer.parseInt(txtMgrId.getText().trim()),
            txtMgrName.getText().trim(), txtMgrEmail.getText().trim(), txtMgrPhone.getText().trim());
        int rows = managerService.addManager(dto);
        if (rows > 0) { setStatus(lblMgrStatus, "Manager " + dto.getManagerId() + " inserted.", new Color(0, 128, 0)); mgrSelectAll(); mgrClear(); }
        else setStatus(lblMgrStatus, "Insert failed — check log viewer.", Color.RED);
    }

    private void mgrUpdate() {
        if (!validateMgrInline()) return;
        ManagerDTO dto = new ManagerDTO(Integer.parseInt(txtMgrId.getText().trim()),
            txtMgrName.getText().trim(), txtMgrEmail.getText().trim(), txtMgrPhone.getText().trim());
        int rows = managerService.updateManager(dto);
        if (rows > 0) { setStatus(lblMgrStatus, rows + " row(s) updated.", new Color(0, 128, 0)); mgrSelectAll(); }
        else setStatus(lblMgrStatus, "Update failed — check log viewer.", Color.RED);
    }

    private void mgrDelete() {
        if (txtMgrId.getText().isBlank()) { setInlineError(lblMgrIdErr, "Required"); return; }
        int managerId;
        try { managerId = Integer.parseInt(txtMgrId.getText().trim()); }
        catch (NumberFormatException e) { setInlineError(lblMgrIdErr, "Must be a number"); return; }
        boolean hasCustomers = customerService.getAllCustomers().stream().anyMatch(c -> c.getManagerId() == managerId);
        if (hasCustomers) {
            setStatus(lblMgrStatus, "Delete blocked — manager still has customers assigned.", Color.ORANGE);
            JOptionPane.showMessageDialog(this, "Cannot delete Manager ID = " + managerId + ".\nThis manager still has customers assigned.", "Delete Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete Manager ID = " + managerId + " ?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        int rows = managerService.removeManager(managerId);
        if (rows > 0) { setStatus(lblMgrStatus, rows + " row(s) deleted.", new Color(0, 128, 0)); mgrSelectAll(); mgrClear(); }
        else setStatus(lblMgrStatus, "Delete failed — check log viewer.", Color.RED);
    }

    private void mgrClear() {
        txtMgrId.setText(""); txtMgrName.setText(""); txtMgrEmail.setText(""); txtMgrPhone.setText("");
        mgrTable.clearSelection(); clearInlineErrors(lblMgrIdErr, lblMgrNameErr);
        setStatus(lblMgrStatus, "Ready", Color.DARK_GRAY);
    }

    private boolean validateMgrInline() {
        boolean ok = true;
        clearInlineErrors(lblMgrIdErr, lblMgrNameErr);
        if (txtMgrId.getText().isBlank())   { setInlineError(lblMgrIdErr,   "Required"); ok = false; }
        else { try { Integer.parseInt(txtMgrId.getText().trim()); } catch (NumberFormatException e) { setInlineError(lblMgrIdErr, "Must be a number"); ok = false; } }
        if (txtMgrName.getText().isBlank()) { setInlineError(lblMgrNameErr, "Required"); ok = false; }
        return ok;
    }

    // =========================================================================
    //   CRUD client
    // =========================================================================

    private void loadManagerCombo() {
        comboManager.removeAllItems();
        for (ManagerDTO dto : managerService.getAllManagers())
            comboManager.addItem(dto.getManagerId() + " - " + dto.getManagerName());
    }

    private void custSelectAll() {
        loadManagerCombo();
        custModel.setRowCount(0);
        for (CustomerDTO dto : customerService.getAllCustomers()) {
            custModel.addRow(new Object[]{ dto.getCustomerId(), dto.getFirstName(), dto.getLastName(),
                dto.getEmail(), dto.getPhone(), dto.getManagerId() });
        }
        setStatus(lblCustStatus, "Loaded " + custModel.getRowCount() + " customer(s)", Color.DARK_GRAY);
    }

    private void custSearch() {
        String idText = txtCustId.getText().trim();
        if (idText.isEmpty()) { setInlineError(lblCustIdErr, "Required"); return; }
        clearInlineErrors(lblCustIdErr);
        try {
            int id = Integer.parseInt(idText);
            CustomerDTO dto = customerService.searchCustomer(id);
            if (dto != null) {
                txtCustFirstName.setText(dto.getFirstName()); txtCustLastName.setText(dto.getLastName());
                txtCustEmail.setText(dto.getEmail()); txtCustPhone.setText(dto.getPhone());
                for (int i = 0; i < comboManager.getItemCount(); i++) {
                    if (comboManager.getItemAt(i).startsWith(dto.getManagerId() + " ")) { comboManager.setSelectedIndex(i); break; }
                }
                setStatus(lblCustStatus, "Customer " + id + " found.", new Color(0, 128, 0));
            } else setStatus(lblCustStatus, "No customer found with ID = " + id, Color.ORANGE);
        } catch (NumberFormatException ex) { setInlineError(lblCustIdErr, "Must be a number"); }
    }

    private void custSearchByName() {
        String keyword = txtCustSearch.getText().trim();
        if (keyword.isEmpty()) { custSelectAll(); return; }
        custModel.setRowCount(0);
        for (CustomerDTO dto : customerService.searchByName(keyword)) {
            custModel.addRow(new Object[]{ dto.getCustomerId(), dto.getFirstName(), dto.getLastName(),
                dto.getEmail(), dto.getPhone(), dto.getManagerId() });
        }
        setStatus(lblCustStatus, "'" + keyword + "' — " + custModel.getRowCount() + " result(s)",
            custModel.getRowCount() > 0 ? new Color(0, 128, 0) : Color.ORANGE);
    }

    private void custInsert() {
        if (!validateCustInline()) return;
        CustomerDTO dto = new CustomerDTO(Integer.parseInt(txtCustId.getText().trim()),
            txtCustFirstName.getText().trim(), txtCustLastName.getText().trim(),
            txtCustEmail.getText().trim(), txtCustPhone.getText().trim(), getSelectedManagerId());
        int rows = customerService.addCustomer(dto);
        if (rows > 0) { setStatus(lblCustStatus, "Customer " + dto.getCustomerId() + " inserted.", new Color(0, 128, 0)); custSelectAll(); custClear(); }
        else setStatus(lblCustStatus, "Insert failed — check log viewer.", Color.RED);
    }

    private void custUpdate() {
        if (!validateCustInline()) return;
        CustomerDTO dto = new CustomerDTO(Integer.parseInt(txtCustId.getText().trim()),
            txtCustFirstName.getText().trim(), txtCustLastName.getText().trim(),
            txtCustEmail.getText().trim(), txtCustPhone.getText().trim(), getSelectedManagerId());
        int rows = customerService.updateCustomer(dto);
        if (rows > 0) { setStatus(lblCustStatus, rows + " row(s) updated.", new Color(0, 128, 0)); custSelectAll(); }
        else setStatus(lblCustStatus, "Update failed — check log viewer.", Color.RED);
    }

    private void custDelete() {
        if (txtCustId.getText().isBlank()) { setInlineError(lblCustIdErr, "Required"); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete Customer ID = " + txtCustId.getText() + " ?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        int rows = customerService.removeCustomer(Integer.parseInt(txtCustId.getText().trim()));
        if (rows > 0) { setStatus(lblCustStatus, rows + " row(s) deleted.", new Color(0, 128, 0)); custSelectAll(); custClear(); }
        else setStatus(lblCustStatus, "Delete failed — check log viewer.", Color.RED);
    }

    private void custClear() {
        txtCustId.setText(""); txtCustFirstName.setText(""); txtCustLastName.setText("");
        txtCustEmail.setText(""); txtCustPhone.setText(""); txtCustSearch.setText("");
        if (comboManager.getItemCount() > 0) comboManager.setSelectedIndex(0);
        custTable.clearSelection(); clearInlineErrors(lblCustIdErr, lblCustFirstNameErr, lblCustLastNameErr);
        setStatus(lblCustStatus, "Ready", Color.DARK_GRAY);
    }

    private boolean validateCustInline() {
        boolean ok = true;
        clearInlineErrors(lblCustIdErr, lblCustFirstNameErr, lblCustLastNameErr);
        if (txtCustId.getText().isBlank())        { setInlineError(lblCustIdErr,        "Required"); ok = false; }
        else { try { Integer.parseInt(txtCustId.getText().trim()); } catch (NumberFormatException e) { setInlineError(lblCustIdErr, "Must be a number"); ok = false; } }
        if (txtCustFirstName.getText().isBlank()) { setInlineError(lblCustFirstNameErr, "Required"); ok = false; }
        if (txtCustLastName.getText().isBlank())  { setInlineError(lblCustLastNameErr,  "Required"); ok = false; }
        return ok;
    }

    // =========================================================================
    //   Onglet comptes
    // =========================================================================

    private JPanel buildAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Account Data"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 4, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Ligne 0 — Identifiant du compte
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Account ID :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtAccId = new JTextField(12);
        txtAccId.setToolTipText("Unique numeric ID for this account (e.g. 1205)");
        formPanel.add(txtAccId, gbc);

        // Ligne 1 — Client
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Customer :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        comboAccCustomer = new JComboBox<>();
        comboAccCustomer.setPrototypeDisplayValue("101 — Alice Tremblay          ");
        formPanel.add(comboAccCustomer, gbc);

        // Ligne 2 — Type de compte
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Account Type :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        comboAccType = new JComboBox<>(new String[]{"SAVINGS", "CHECKING", "CREDIT"});
        formPanel.add(comboAccType, gbc);

        // Ligne 3 — Solde initial
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Opening Balance ($) :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtAccBalance = new JTextField("0.00", 12);
        formPanel.add(txtAccBalance, gbc);

        // Ligne 4 — Limite quotidienne
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Daily Limit ($) :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtAccDailyLimit = new JTextField("5000.00", 12);
        formPanel.add(txtAccDailyLimit, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Boutons d'action
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton btnSelectAll = styleBtn(new JButton("Select All"),  new Color(70, 130, 180));
        JButton btnInsert    = styleBtn(new JButton("Create"),      new Color(40, 167, 69));
        JButton btnDelete    = styleBtn(new JButton("Delete"),      new Color(220, 53, 69));
        JButton btnExportPdf = styleBtn(new JButton("Export PDF"),  new Color(80, 80, 80));
        JButton btnClear     = new JButton("Clear");
        btnPanel.add(btnSelectAll); btnPanel.add(btnInsert);
        btnPanel.add(btnDelete);    btnPanel.add(btnExportPdf); btnPanel.add(btnClear);

        // Tableau de données
        accModel = new DefaultTableModel(
            new String[]{"Account ID", "Customer ID", "Customer Name", "Type", "Balance", "Status", "Daily Limit"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        accTable = new JTable(accModel);
        accTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accTable.setRowHeight(24);
        accTable.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        accTable.setFont(new Font("Tahoma", Font.PLAIN, 12));

        // Clic sur une ligne → remplissage du formulaire
        accTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && accTable.getSelectedRow() >= 0) {
                int row = accTable.getSelectedRow();
                txtAccId.setText(accModel.getValueAt(row, 0).toString());
                txtAccBalance.setText(accModel.getValueAt(row, 4).toString().replace("$", ""));
                txtAccDailyLimit.setText(accModel.getValueAt(row, 6).toString().replace("$", ""));
                comboAccType.setSelectedItem(accModel.getValueAt(row, 3).toString());
                String custId = accModel.getValueAt(row, 1).toString();
                for (int i = 0; i < comboAccCustomer.getItemCount(); i++) {
                    if (comboAccCustomer.getItemAt(i).startsWith(custId + " ")) {
                        comboAccCustomer.setSelectedIndex(i); break;
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(accTable);
        scroll.setBorder(new TitledBorder("All Bank Accounts"));

        lblAccStatus = new JLabel("  Ready");
        lblAccStatus.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblAccStatus.setForeground(Color.DARK_GRAY);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(btnPanel,     BorderLayout.NORTH);
        center.add(scroll,       BorderLayout.CENTER);
        center.add(lblAccStatus, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        btnSelectAll.addActionListener(e -> accSelectAll());
        btnInsert.addActionListener(e -> accInsert());
        btnDelete.addActionListener(e -> accDelete());
        btnClear.addActionListener(e -> accClear());
        btnExportPdf.addActionListener(e -> exportAccountsPdf());

        return panel;
    }

    private void accSelectAll() {
        loadAccCustomerCombo();
        accModel.setRowCount(0);
        for (AccountDTO dto : accountDAO.selectAll()) {
            // Recherche du nom du client
            CustomerDTO c = customerService.searchCustomer(dto.getCustomerId());
            String name = (c != null) ? c.getFirstName() + " " + c.getLastName() : "—";
            accModel.addRow(new Object[]{
                dto.getAccountId(),
                dto.getCustomerId(),
                name,
                dto.getAccountType(),
                String.format("$%.2f", dto.getBalance()),
                dto.getStatus(),
                String.format("$%.2f", dto.getDailyLimit())
            });
        }
        setStatus(lblAccStatus, "Loaded " + accModel.getRowCount() + " account(s)", Color.DARK_GRAY);
    }

    private void accInsert() {
        // Validation de l'identifiant du compte
        String idText = txtAccId.getText().trim();
        if (idText.isEmpty()) { setStatus(lblAccStatus, "Account ID is required.", Color.RED); return; }
        long accId;
        try { accId = Long.parseLong(idText); }
        catch (NumberFormatException e) { setStatus(lblAccStatus, "Account ID must be a number.", Color.RED); return; }

        if (accountDAO.selectById(accId) != null) {
            setStatus(lblAccStatus, "Account ID " + accId + " already exists.", Color.ORANGE);
            JOptionPane.showMessageDialog(this, "Account ID " + accId + " already exists.\nChoose a different ID.",
                "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Conversion du solde et de la limite
        double balance, dailyLimit;
        try { balance    = Double.parseDouble(txtAccBalance.getText().trim()); }
        catch (NumberFormatException e) { setStatus(lblAccStatus, "Balance must be a number.", Color.RED); return; }
        try { dailyLimit = Double.parseDouble(txtAccDailyLimit.getText().trim()); }
        catch (NumberFormatException e) { setStatus(lblAccStatus, "Daily limit must be a number.", Color.RED); return; }

        int custId = getSelectedAccCustomerId();
        String accType = (String) comboAccType.getSelectedItem();

        AccountDTO dto = new AccountDTO(accId, custId, accType, balance, "ACTIVE", dailyLimit, new java.util.Date());
        int rows = accountDAO.insert(dto);
        if (rows > 0) {
            setStatus(lblAccStatus, "Account #" + accId + " created for Customer " + custId + ".", new Color(0, 128, 0));
            accSelectAll(); accClear();
            logger.info("BankManager", "Manager " + loggedInManager.getManagerId()
                    + " created Account #" + accId + " (" + accType + ") for Customer " + custId);
        } else {
            setStatus(lblAccStatus, "Insert failed — check log viewer.", Color.RED);
        }
    }

    private void accDelete() {
        String idText = txtAccId.getText().trim();
        if (idText.isEmpty()) { setStatus(lblAccStatus, "Select an account first.", Color.ORANGE); return; }
        long accId;
        try { accId = Long.parseLong(idText); }
        catch (NumberFormatException e) { setStatus(lblAccStatus, "Account ID must be a number.", Color.RED); return; }

        if (JOptionPane.showConfirmDialog(this,
                "Delete Account #" + accId + " permanently?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;

        int rows = accountDAO.delete(accId);
        if (rows > 0) {
            setStatus(lblAccStatus, "Account #" + accId + " deleted.", new Color(0, 128, 0));
            accSelectAll(); accClear();
        } else {
            setStatus(lblAccStatus, "Delete failed — check log viewer.", Color.RED);
        }
    }

    private void accClear() {
        txtAccId.setText(""); txtAccBalance.setText("0.00"); txtAccDailyLimit.setText("5000.00");
        comboAccType.setSelectedIndex(0);
        if (comboAccCustomer.getItemCount() > 0) comboAccCustomer.setSelectedIndex(0);
        accTable.clearSelection();
        setStatus(lblAccStatus, "Ready", Color.DARK_GRAY);
    }

    private void loadAccCustomerCombo() {
        comboAccCustomer.removeAllItems();
        for (CustomerDTO dto : customerService.getAllCustomers())
            comboAccCustomer.addItem(dto.getCustomerId() + " — " + dto.getFirstName() + " " + dto.getLastName());
    }

    private int getSelectedAccCustomerId() {
        String item = (String) comboAccCustomer.getSelectedItem();
        if (item == null) return 0;
        return Integer.parseInt(item.split(" ")[0]);
    }

    private void exportAccountsPdf() {
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < accModel.getRowCount(); r++) {
            rows.add(new String[]{
                accModel.getValueAt(r, 0).toString(),
                accModel.getValueAt(r, 1).toString(),
                accModel.getValueAt(r, 2).toString(),
                accModel.getValueAt(r, 3).toString(),
                accModel.getValueAt(r, 4).toString(),
                accModel.getValueAt(r, 5).toString(),
                accModel.getValueAt(r, 6).toString()
            });
        }
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "No accounts to export."); return; }
        runPdfExport("Account_List",
            new String[]{"Account ID", "Customer ID", "Customer Name", "Type", "Balance", "Status", "Daily Limit"},
            rows, lblAccStatus);
    }

    // =========================================================================
    //   Utilitaires
    // =========================================================================

    private int getSelectedManagerId() {
        String item = (String) comboManager.getSelectedItem();
        return Integer.parseInt(item.split(" ")[0]);
    }

    private void setStatus(JLabel lbl, String msg, Color color) { lbl.setText("  " + msg); lbl.setForeground(color); }

    private JLabel errLabel() {
        JLabel lbl = new JLabel("");
        lbl.setForeground(Color.RED);
        lbl.setFont(new Font("Tahoma", Font.ITALIC, 10));
        return lbl;
    }

    private void setInlineError(JLabel lbl, String msg) { lbl.setText("  ⚠ " + msg); }

    private void clearInlineErrors(JLabel... labels) { for (JLabel l : labels) l.setText(""); }

    private JButton styleBtn(JButton btn, Color color) {
        btn.setBackground(color); btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Tahoma", Font.BOLD, 12));
        btn.setOpaque(true); btn.setBorderPainted(false);
        return btn;
    }
}
