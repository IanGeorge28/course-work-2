package view;

import model.Account;
import model.AccountFactory;
import model.JointAccount;
import model.StudentAccount;
import util.DatabaseHelper;
import util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.LocalDate;

/**
 * Main GUI class for the First Bank Uganda New Account Opening Application.
 * Implements a comprehensive form with all required fields, validation,
 * dynamic behaviour (day auto-update, conditional fields), and database persistence.
 */
public class BankAccountForm extends JFrame {

    // --- Constants ---
    private static final String APP_TITLE = "First Bank Uganda — New Account Opening";
    private static final String[] ACCOUNT_TYPES = {"Savings", "Current", "Fixed Deposit", "Student", "Joint"};
    private static final String[] BRANCHES = {"Kampala", "Gulu", "Mbarara", "Jinja", "Mbale"};
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final int MIN_BIRTH_YEAR = CURRENT_YEAR - 75; // Oldest possible: 75 years ago
    private static final int MAX_BIRTH_YEAR = CURRENT_YEAR - 18;   // Youngest possible: 18 years ago

    // --- Color constants ---
    private static final Color ERROR_COLOR = new Color(220, 20, 60);
    private static final Color HEADER_COLOR = new Color(0, 51, 102);
    private static final Color ACCENT_COLOR = new Color(0, 102, 51);
    private static final Color BG_COLOR = new Color(245, 245, 250);

    // --- Form Input Components ---
    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtNIN;
    private JTextField txtEmail;
    private JTextField txtConfirmEmail;
    private JTextField txtPhone;
    private JPasswordField txtPIN;
    private JPasswordField txtConfirmPIN;
    private JTextField txtDeposit;
    private JTextField txtSecondNIN;

    private JComboBox<Integer> cboYear;
    private JComboBox<String> cboMonth;
    private JComboBox<Integer> cboDay;
    private JComboBox<String> cboAccountType;
    private JComboBox<String> cboBranch;

    private JTextArea txtSummary;

    // --- Error Labels (inline) ---
    private JLabel errFirstName, errLastName, errNIN, errEmail, errPhone;
    private JLabel errPIN, errDOB, errAccountType, errBranch, errDeposit, errSecondNin;

    // --- Panel for conditional Second NIN ---
    private JPanel pnlSecondNin;

    // --- Database helper ---
    private DatabaseHelper dbHelper;

    /**
     * Constructs and displays the Bank Account Opening form.
     */
    public BankAccountForm() {
        super(APP_TITLE);
        initializeDatabase();
        initComponents();
        layoutComponents();
        attachListeners();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (dbHelper != null) dbHelper.close();
                dispose();
                System.exit(0);
            }
        });
        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
    }

    /**
     * Initializes the database connection. If it fails, shows a warning
     * but allows the form to operate (without persistence).
     */
    private void initializeDatabase() {
        try {
            String dbPath = "db/FirstBankUganda.accdb";
            dbHelper = DatabaseHelper.getInstance(dbPath);
            if (!dbHelper.isConnected()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: Database connection failed. Records will not be saved.\n"
                                + "Ensure 'db/FirstBankUganda.accdb' exists and UCanAccess JARs are in classpath.",
                        "Database Warning", JOptionPane.WARNING_MESSAGE);
                dbHelper = null;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Warning: Could not connect to database.\n" + e.getMessage()
                            + "\n\nRecords will not be saved to MS Access.",
                    "Database Warning", JOptionPane.WARNING_MESSAGE);
            dbHelper = null;
        }
    }

    /**
     * Creates and initializes all Swing components.
     */
    private void initComponents() {
        // Text fields
        txtFirstName = new JTextField(25);
        txtLastName = new JTextField(25);
        txtNIN = new JTextField(14);
        txtEmail = new JTextField(30);
        txtConfirmEmail = new JTextField(30);
        txtPhone = new JTextField(15);
        txtPIN = new JPasswordField(6);
        txtConfirmPIN = new JPasswordField(6);
        txtDeposit = new JTextField(15);
        txtSecondNIN = new JTextField(14);

        // Year combo box
        Integer[] years = new Integer[MAX_BIRTH_YEAR - MIN_BIRTH_YEAR + 1];
        for (int i = 0; i < years.length; i++) {
            years[i] = MIN_BIRTH_YEAR + i;
        }
        cboYear = new JComboBox<>(years);

        // Month combo box
        cboMonth = new JComboBox<>(MONTH_NAMES);

        // Day combo box (populated dynamically)
        cboDay = new JComboBox<>();
        updateDaysInMonth();

        // Account type and branch
        cboAccountType = new JComboBox<>(ACCOUNT_TYPES);
        cboBranch = new JComboBox<>(BRANCHES);

        // Summary area
        txtSummary = new JTextArea(4, 60);
        txtSummary.setEditable(false);
        txtSummary.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtSummary.setBackground(new Color(240, 248, 255));

        // Error labels
        errFirstName = createErrorLabel();
        errLastName = createErrorLabel();
        errNIN = createErrorLabel();
        errEmail = createErrorLabel();
        errPhone = createErrorLabel();
        errPIN = createErrorLabel();
        errDOB = createErrorLabel();
        errAccountType = createErrorLabel();
        errBranch = createErrorLabel();
        errDeposit = createErrorLabel();
        errSecondNin = createErrorLabel();

        // Second NIN panel (initially hidden for non-Joint accounts)
        pnlSecondNin = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSecondNin.add(new JLabel("Second NIN:"));
        pnlSecondNin.add(Box.createHorizontalStrut(5));
        pnlSecondNin.add(txtSecondNIN);
        pnlSecondNin.add(Box.createHorizontalStrut(5));
        pnlSecondNin.add(errSecondNin);
        pnlSecondNin.setVisible(false);
    }

    /**
     * Creates a standard error label with red text.
     */
    private JLabel createErrorLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setForeground(ERROR_COLOR);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11));
        lbl.setPreferredSize(new Dimension(300, 16));
        return lbl;
    }

    /**
     * Lays out all components in the frame using nested panels.
     */
    private void layoutComponents() {
        // Main content pane with scroll support
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_COLOR);

        // --- Header Banner ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblTitle = new JLabel("FIRST BANK UGANDA", JLabel.CENTER);
        lblTitle.setFont(new Font("Serif", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        JLabel lblSubtitle = new JLabel("New Account Opening Application Form", JLabel.CENTER);
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(200, 220, 255));
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Form Body (scrollable) ---
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setBackground(BG_COLOR);

        // Section 1: Personal Details
        formBody.add(createPersonalDetailsSection());

        // Section 2: Contact Information
        formBody.add(createContactSection());

        // Section 3: Account Information
        formBody.add(createAccountSection());

        // Section 4: Security
        formBody.add(createSecuritySection());

        // Section 5: Buttons
        formBody.add(createButtonSection());

        // Section 6: Account Summary
        formBody.add(createSummarySection());

        JScrollPane scrollPane = new JScrollPane(formBody);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    /**
     * Creates the Personal Details section with Name, NIN, and DOB fields.
     */
    private JPanel createPersonalDetailsSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Personal Details",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 13),
                HEADER_COLOR));
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "  Personal Details  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), HEADER_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // First Name
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("First Name:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtFirstName, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errFirstName, gbc);

        // Last Name
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Last Name:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtLastName, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errLastName, gbc);

        // National ID
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("National ID (NIN):*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtNIN, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errNIN, gbc);

        // Date of Birth label
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Date of Birth:*"), gbc);

        // DOB combo boxes in a sub-panel
        JPanel dobPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dobPanel.add(cboDay);
        dobPanel.add(new JLabel(" / "));
        dobPanel.add(cboMonth);
        dobPanel.add(new JLabel(" / "));
        cboYear.setPrototypeDisplayValue(2000);
        dobPanel.add(cboYear);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(dobPanel, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errDOB, gbc);

        // Make the error label column wider
        panel.setColumnWidth(2, 320);

        return panel;
    }

    /**
     * Creates the Contact Information section with Email and Phone fields.
     */
    private JPanel createContactSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "  Contact Information  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), HEADER_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Email
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Email:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtEmail, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errEmail, gbc);

        // Confirm Email
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Confirm Email:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtConfirmEmail, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel(" "), gbc);

        // Phone
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Phone Number:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtPhone, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errPhone, gbc);

        panel.setColumnWidth(2, 320);
        return panel;
    }

    /**
     * Creates the Account Information section with Account Type, Branch, Deposit,
     * and the conditional Second NIN field for Joint accounts.
     */
    private JPanel createAccountSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "  Account Information  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), HEADER_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Account Type
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Account Type:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(cboAccountType, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errAccountType, gbc);

        // Branch
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Branch:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(cboBranch, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errBranch, gbc);

        // Opening Deposit
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Opening Deposit (UGX):*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtDeposit, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errDeposit, gbc);

        // Minimum deposit hint label
        row++;
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        JLabel lblHint = new JLabel("<html><i style='color:gray;'>Minimum deposit shown will update based on selected Account Type.</i></html>");
        panel.add(lblHint, gbc);

        // Second NIN (for Joint accounts, shown/hidden dynamically)
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.gridwidth = 3; gbc.weightx = 1;
        panel.add(pnlSecondNin, gbc);

        panel.setColumnWidth(2, 320);
        return panel;
    }

    /**
     * Creates the Security section with PIN and Confirm PIN fields.
     */
    private JPanel createSecuritySection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "  Security (PIN)  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), HEADER_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // PIN
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("PIN:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtPIN, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(errPIN, gbc);

        // Confirm PIN
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Confirm PIN:*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(txtConfirmPIN, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel(" "), gbc);

        // Hint
        row++;
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        JLabel lblPinHint = new JLabel("<html><i style='color:gray;'>PIN must be 4–6 digits and cannot be all identical (e.g., 0000).</i></html>");
        panel.add(lblPinHint, gbc);

        panel.setColumnWidth(2, 320);
        return panel;
    }

    /**
     * Creates the Submit and Reset buttons section.
     */
    private JPanel createButtonSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(BG_COLOR);

        JButton btnSubmit = new JButton("  Submit  ");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnSubmit.setBackground(ACCENT_COLOR);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> handleSubmit());

        JButton btnReset = new JButton("  Reset  ");
        btnReset.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnReset.setBackground(new Color(120, 120, 120));
        btnReset.setForeground(Color.WHITE);
        btnReset.setFocusPainted(false);
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(e -> handleReset());

        panel.add(btnSubmit);
        panel.add(btnReset);
        return panel;
    }

    /**
     * Creates the Account Summary section with a read-only text area.
     */
    private JPanel createSummarySection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "  Account Summary is Below:  ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), ACCENT_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        JScrollPane scrollPane = new JScrollPane(txtSummary);
        scrollPane.setPreferredSize(new Dimension(700, 80));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Attaches event listeners for dynamic behaviour:
     * - Day combo updates when month or year changes
     * - Second NIN panel shows/hides when account type changes
     */
    private void attachListeners() {
        // Update days when month changes
        cboMonth.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDaysInMonth();
            }
        });

        // Update days when year changes
        cboYear.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDaysInMonth();
            }
        });

        // Show/hide Second NIN based on account type
        cboAccountType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selected = (String) cboAccountType.getSelectedItem();
                pnlSecondNin.setVisible("Joint".equals(selected));
                // Re-layout the parent to account for visibility change
                pnlSecondNin.revalidate();
                pnlSecondNin.repaint();
            }
        });
    }

    /**
     * Updates the Day combo box to reflect the correct number of days
     * for the currently selected month and year, accounting for leap years.
     */
    private void updateDaysInMonth() {
        int year = (Integer) cboYear.getSelectedItem();
        int month = cboMonth.getSelectedIndex() + 1; // 1-based
        int maxDays = ValidationUtil.getDaysInMonth(year, month);

        int previouslySelected = cboDay.getItemCount() > 0 ?
                (Integer) cboDay.getSelectedItem() : 1;

        cboDay.removeAllItems();
        for (int d = 1; d <= maxDays; d++) {
            cboDay.addItem(d);
        }

        // Restore selection or clamp to max
        if (previouslySelected > maxDays) {
            cboDay.setSelectedItem(maxDays);
        } else if (previouslySelected >= 1) {
            cboDay.setSelectedItem(previouslySelected);
        } else {
            cboDay.setSelectedIndex(0);
        }
    }

    /**
     * Handles the Submit button action:
     * 1. Validates all fields
     * 2. Shows inline errors and summary dialog if invalid
     * 3. Generates account number and displays formatted summary if valid
     * 4. Persists record to MS Access database
     */
    private void handleSubmit() {
        // Clear all previous errors
        clearAllErrors();

        // Collect all error messages
        java.util.List<String> errors = new java.util.ArrayList<>();

        // Gather field values
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        String nin = txtNIN.getText().trim().toUpperCase();
        String email = txtEmail.getText().trim();
        String confirmEmail = txtConfirmEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String pin = new String(txtPIN.getPassword());
        String confirmPin = new String(txtConfirmPIN.getPassword());
        String depositText = txtDeposit.getText().trim();
        String accountType = (String) cboAccountType.getSelectedItem();
        String branch = (String) cboBranch.getSelectedItem();
        int day = (Integer) cboDay.getSelectedItem();
        int month = cboMonth.getSelectedIndex() + 1;
        int year = (Integer) cboYear.getSelectedItem();
        String secondNin = txtSecondNIN.getText().trim().toUpperCase();

        // --- Validate each field ---
        String error;

        // First Name
        error = ValidationUtil.validateName(firstName, "First Name");
        if (error != null) { errors.add(error); errFirstName.setText(error); }

        // Last Name
        error = ValidationUtil.validateName(lastName, "Last Name");
        if (error != null) { errors.add(error); errLastName.setText(error); }

        // NIN
        error = ValidationUtil.validateNIN(nin);
        if (error != null) { errors.add(error); errNIN.setText(error); }

        // Email
        error = ValidationUtil.validateEmailMatch(email, confirmEmail);
        if (error != null) { errors.add(error); errEmail.setText(error); }

        // Phone
        error = ValidationUtil.validatePhone(phone);
        if (error != null) { errors.add(error); errPhone.setText(error); }

        // PIN
        error = ValidationUtil.validatePIN(pin, confirmPin);
        if (error != null) { errors.add(error); errPIN.setText(error); }

        // DOB / Age
        error = ValidationUtil.validateAge(year, month, day, accountType);
        if (error != null) { errors.add(error); errDOB.setText(error); }

        // Account Type (always selected from combo, but check anyway)
        if (accountType == null || accountType.isEmpty()) {
            error = "Please select an Account Type.";
            errors.add(error);
            errAccountType.setText(error);
        }

        // Branch (always selected from combo, but check anyway)
        if (branch == null || branch.isEmpty()) {
            error = "Please select a Branch.";
            errors.add(error);
            errBranch.setText(error);
        }

        // Parse deposit amount
        double depositAmount = 0;
        try {
            depositAmount = Double.parseDouble(depositText.replace(",", ""));
        } catch (NumberFormatException ex) {
            // Will be caught by validateDeposit
        }

        // Create Account object using Factory (polymorphism)
        Account account = null;
        try {
            account = AccountFactory.createAccount(accountType, depositAmount);
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        }

        // Deposit validation (uses polymorphism: account.minimumDeposit())
        if (account != null) {
            error = ValidationUtil.validateDeposit(depositText, account);
            if (error != null) { errors.add(error); errDeposit.setText(error); }
        }

        // Second NIN for Joint accounts
        if ("Joint".equals(accountType)) {
            error = ValidationUtil.validateSecondNIN(secondNin);
            if (error != null) { errors.add(error); errSecondNin.setText(error); }
        }

        // --- If errors found, show summary dialog and stop ---
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Please fix the following errors:\n\n");
            int num = 1;
            for (String e : errors) {
                sb.append(num++).append(". ").append(e).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(),
                    "Validation Errors", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- All valid: generate account number and display summary ---
        String branchCode = DatabaseHelper.getBranchCode(branch);
        String accountNumber = null;

        if (dbHelper != null) {
            try {
                accountNumber = dbHelper.generateAccountNumber(branchCode, CURRENT_YEAR);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error generating account number: " + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            // Fallback if no database: generate locally
            accountNumber = String.format("%s-%d-%06d", branchCode, CURRENT_YEAR,
                    (int) (Math.random() * 999999) + 1);
        }

        String dobStr = ValidationUtil.formatDate(year, month, day);
        String formattedDeposit = String.format("%,d", (long) depositAmount);

        // Build the summary line as specified in the coursework
        String summaryLine = String.format("ACC: %s | %s %s | %s | %s | DOB %s | %s | Deposit %s | %s",
                accountNumber,
                lastName, firstName,  // "Okello Allan" format
                accountType,
                branch,
                dobStr,
                phone,
                formattedDeposit,
                email);

        txtSummary.setText(summaryLine);

        // --- Persist to MS Access database ---
        if (dbHelper != null) {
            try {
                String secondNinToSave = "Joint".equals(accountType) ? secondNin : null;
                dbHelper.insertAccount(accountNumber,
                        firstName.trim(), lastName.trim(),
                        nin, email, phone, dobStr,
                        account, branch, branchCode,
                        pin, secondNinToSave);

                JOptionPane.showMessageDialog(this,
                        "Account created successfully!\n\n" + summaryLine,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Account number generated but failed to save to database:\n" + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Account created (display only — not saved to database).\n\n" + summaryLine,
                    "Success (No Database)", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Handles the Reset button: clears all fields and error labels.
     */
    private void handleReset() {
        txtFirstName.setText("");
        txtLastName.setText("");
        txtNIN.setText("");
        txtEmail.setText("");
        txtConfirmEmail.setText("");
        txtPhone.setText("");
        txtPIN.setText("");
        txtConfirmPIN.setText("");
        txtDeposit.setText("");
        txtSecondNIN.setText("");
        cboYear.setSelectedIndex(0);
        cboMonth.setSelectedIndex(0);
        updateDaysInMonth();
        cboAccountType.setSelectedIndex(0);
        cboBranch.setSelectedIndex(0);
        txtSummary.setText("");
        pnlSecondNin.setVisible(false);
        clearAllErrors();
    }

    /**
     * Clears all inline error labels.
     */
    private void clearAllErrors() {
        errFirstName.setText(" ");
        errLastName.setText(" ");
        errNIN.setText(" ");
        errEmail.setText(" ");
        errPhone.setText(" ");
        errPIN.setText(" ");
        errDOB.setText(" ");
        errAccountType.setText(" ");
        errBranch.setText(" ");
        errDeposit.setText(" ");
        errSecondNin.setText(" ");
    }
}