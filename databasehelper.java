package util;

import model.Account;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Handles all database operations for the First Bank Uganda application.
 * Uses UCanAccess JDBC driver to connect to MS Access (.accdb) database.
 * Provides methods to generate account numbers and persist new account records.
 */
public class DatabaseHelper {

    // Branch code mapping
    public static final String[][] BRANCH_CODES = {
            {"Kampala", "KLA"},
            {"Gulu", "GUL"},
            {"Mbarara", "MBR"},
            {"Jinja", "JIN"},
            {"Mbale", "MBL"}
    };

    private static DatabaseHelper instance;
    private Connection connection;
    private String dbPath;

    /**
     * Private constructor for Singleton pattern.
     * Establishes connection to the MS Access database.
     */
    private DatabaseHelper(String dbPath) throws SQLException {
        this.dbPath = dbPath;
        connect();
        initializeTables();
    }

    /**
     * Returns the singleton instance, creating it if necessary.
     * @param dbPath absolute or relative path to the .accdb file
     */
    public static synchronized DatabaseHelper getInstance(String dbPath) throws SQLException {
        if (instance == null) {
            instance = new DatabaseHelper(dbPath);
        }
        return instance;
    }

    /**
     * Establishes a connection to the Access database via UCanAccess.
     */
    private void connect() throws SQLException {
        try {
            // Resolve to absolute path
            File dbFile = new File(dbPath);
            String absolutePath = dbFile.getAbsolutePath();
            // Replace backslashes for JDBC URL
            String url = "jdbc:ucanaccess://" + absolutePath.replace("\\", "/");

            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            connection = DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("UCanAccess driver not found. Add the JAR to your classpath.", e);
        }
    }

    /**
     * Creates the required tables if they do not already exist.
     */
    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Account Counters table: tracks the next sequential number per branch per year
            String createCounters = "CREATE TABLE IF NOT EXISTS AccountCounters ("
                    + "branch_code VARCHAR(3) NOT NULL, "
                    + "year_num INTEGER NOT NULL, "
                    + "next_number BIGINT NOT NULL DEFAULT 1, "
                    + "PRIMARY KEY (branch_code, year_num)"
                    + ")";
            stmt.execute(createCounters);

            // Bank Accounts table: stores all opened accounts
            String createAccounts = "CREATE TABLE IF NOT EXISTS BankAccounts ("
                    + "account_number VARCHAR(20) PRIMARY KEY, "
                    + "first_name VARCHAR(30) NOT NULL, "
                    + "last_name VARCHAR(30) NOT NULL, "
                    + "nin VARCHAR(14) NOT NULL, "
                    + "email VARCHAR(100) NOT NULL, "
                    + "phone VARCHAR(15) NOT NULL, "
                    + "dob DATE NOT NULL, "
                    + "account_type VARCHAR(20) NOT NULL, "
                    + "branch VARCHAR(20) NOT NULL, "
                    + "branch_code VARCHAR(3) NOT NULL, "
                    + "opening_deposit DOUBLE NOT NULL, "
                    + "pin VARCHAR(6) NOT NULL, "
                    + "second_nin VARCHAR(14), "
                    + "created_date DATETIME NOT NULL"
                    + ")";
            stmt.execute(createAccounts);
        }
    }

    /**
     * Gets the branch code for a given branch name.
     * @param branchName the display name of the branch
     * @return the 3-letter branch code
     * @throws IllegalArgumentException if branch name is unknown
     */
    public static String getBranchCode(String branchName) {
        for (String[] bc : BRANCH_CODES) {
            if (bc[0].equals(branchName)) {
                return bc[1];
            }
        }
        throw new IllegalArgumentException("Unknown branch: " + branchName);
    }

    /**
     * Generates the next account number in the format BRANCHCODE-YYYY-xxxxxx.
     * Uses a per-year, per-branch sequential counter stored in the database.
     * This method is synchronized to prevent duplicate numbers in concurrent scenarios.
     *
     * @param branchCode the 3-letter branch code
     * @param year the 4-digit year
     * @return the generated account number string
     */
    public synchronized String generateAccountNumber(String branchCode, int year) throws SQLException {
        String sql = "SELECT next_number FROM AccountCounters WHERE branch_code = ? AND year_num = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, branchCode);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();
            long nextNum;

            if (rs.next()) {
                nextNum = rs.getLong("next_number");
            } else {
                // First account for this branch and year
                nextNum = 1;
            }
            rs.close();

            // Format: BRANCHCODE-YYYY-xxxxxx (6-digit zero-padded)
            String accountNumber = String.format("%s-%d-%06d", branchCode, year, nextNum);

            // Upsert the counter for next time
            upsertCounter(branchCode, year, nextNum + 1);

            return accountNumber;
        }
    }

    /**
     * Inserts or updates the counter row for a given branch and year.
     */
    private void upsertCounter(String branchCode, int year, long nextNumber) throws SQLException {
        // Check if row exists
        String checkSql = "SELECT COUNT(*) FROM AccountCounters WHERE branch_code = ? AND year_num = ?";
        try (PreparedStatement checkPs = connection.prepareStatement(checkSql)) {
            checkPs.setString(1, branchCode);
            checkPs.setInt(2, year);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();

            if (count > 0) {
                // Update existing
                String updateSql = "UPDATE AccountCounters SET next_number = ? WHERE branch_code = ? AND year_num = ?";
                try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                    updatePs.setLong(1, nextNumber);
                    updatePs.setString(2, branchCode);
                    updatePs.setInt(3, year);
                    updatePs.executeUpdate();
                }
            } else {
                // Insert new
                String insertSql = "INSERT INTO AccountCounters (branch_code, year_num, next_number) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                    insertPs.setString(1, branchCode);
                    insertPs.setInt(2, year);
                    insertPs.setLong(3, nextNumber);
                    insertPs.executeUpdate();
                }
            }
        }
    }

    /**
     * Persists a new bank account record to the database.
     *
     * @param accountNumber the generated account number
     * @param firstName client's first name
     * @param lastName client's last name
     * @param nin National ID number
     * @param email email address
     * @param phone phone number
     * @param dob date of birth as "YYYY-MM-DD"
     * @param account the Account object (polymorphic)
     * @param branchName the branch display name
     * @param branchCode the 3-letter branch code
     * @param pin the selected PIN
     * @param secondNin second NIN for Joint accounts (null otherwise)
     * @throws SQLException if the insert fails
     */
    public void insertAccount(String accountNumber, String firstName, String lastName,
                              String nin, String email, String phone, String dob,
                              Account account, String branchName, String branchCode,
                              String pin, String secondNin) throws SQLException {

        String sql = "INSERT INTO BankAccounts (account_number, first_name, last_name, "
                + "nin, email, phone, dob, account_type, branch, branch_code, "
                + "opening_deposit, pin, second_nin, created_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, nin);
            ps.setString(5, email);
            ps.setString(6, phone);
            ps.setString(7, dob); // UCanAccess accepts YYYY-MM-DD
            ps.setString(8, account.getAccountType());
            ps.setString(9, branchName);
            ps.setString(10, branchCode);
            ps.setDouble(11, account.getOpeningDeposit());
            ps.setString(12, pin);

            if (secondNin != null && !secondNin.trim().isEmpty()) {
                ps.setString(13, secondNin.trim().toUpperCase());
            } else {
                ps.setNull(13, Types.VARCHAR);
            }

            ps.setString(14, java.time.LocalDateTime.now().toString());

            ps.executeUpdate();
        }
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Tests the database connection.
     * @return true if connection is valid
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}