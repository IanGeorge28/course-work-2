package util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Utility class containing static validation methods for all form fields.
 * Each method returns an error message string if validation fails,
 * or null if the field is valid.
 */
public final class ValidationUtil {

    // Pre-compiled regex patterns for performance
    private static final Pattern LETTERS_ONLY = Pattern.compile("^[A-Za-z]{2,30}$");
    private static final Pattern NIN_PATTERN = Pattern.compile("^[A-Z0-9]{14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+256\\d{9}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4,6}$");

    // Private constructor to prevent instantiation
    private ValidationUtil() {
    }

    /**
     * Validates a name field (first or last name).
     */
    public static String validateName(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required.";
        }
        String trimmed = value.trim();
        if (!LETTERS_ONLY.matcher(trimmed).matches()) {
            return fieldName + " must contain only letters and be 2–30 characters.";
        }
        return null; // valid
    }

    /**
     * Validates the National ID (NIN): exactly 14 alphanumeric, UPPERCASE.
     */
    public static String validateNIN(String nin) {
        if (nin == null || nin.trim().isEmpty()) {
            return "National ID (NIN) is required.";
        }
        String trimmed = nin.trim().toUpperCase();
        if (!NIN_PATTERN.matcher(trimmed).matches()) {
            return "NIN must be exactly 14 alphanumeric characters in UPPERCASE.";
        }
        return null;
    }

    /**
     * Validates an email field for correct format.
     */
    public static String validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format.";
        }
        return null;
    }

    /**
     * Validates that email and confirm email match.
     */
    public static String validateEmailMatch(String email, String confirmEmail) {
        String emailError = validateEmailFormat(email);
        if (emailError != null) return emailError;

        if (confirmEmail == null || confirmEmail.trim().isEmpty()) {
            return "Confirm Email is required.";
        }
        if (!email.trim().equalsIgnoreCase(confirmEmail.trim())) {
            return "Email and Confirm Email do not match.";
        }
        return null;
    }

    /**
     * Validates phone number: must be +256XXXXXXXXX (9 digits after +256).
     */
    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone Number is required.";
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            return "Phone must follow format +256XXXXXXXXX (9 digits after +256).";
        }
        return null;
    }

    /**
     * Validates PIN: 4-6 numeric digits, must match confirmation,
     * and must not consist of all identical digits (e.g., 0000, 111111).
     */
    public static String validatePIN(String pin, String confirmPin) {
        if (pin == null || pin.trim().isEmpty()) {
            return "PIN is required.";
        }
        if (!PIN_PATTERN.matcher(pin.trim()).matches()) {
            return "PIN must be 4–6 numeric digits.";
        }
        // Check for all identical digits
        String trimmed = pin.trim();
        if (trimmed.chars().distinct().count() == 1) {
            return "PIN must not be all identical digits (e.g., 0000).";
        }
        if (confirmPin == null || confirmPin.trim().isEmpty()) {
            return "Confirm PIN is required.";
        }
        if (!trimmed.equals(confirmPin.trim())) {
            return "PIN and Confirm PIN do not match.";
        }
        return null;
    }

    /**
     * Validates the second NIN for Joint accounts.
     */
    public static String validateSecondNIN(String secondNin) {
        if (secondNin == null || secondNin.trim().isEmpty()) {
            return "Second NIN is required for Joint accounts.";
        }
        String trimmed = secondNin.trim().toUpperCase();
        if (!NIN_PATTERN.matcher(trimmed).matches()) {
            return "Second NIN must be exactly 14 alphanumeric characters in UPPERCASE.";
        }
        return null;
    }

    /**
     * Computes age from date of birth components.
     * @return the computed age in years
     */
    public static int computeAge(int year, int month, int day) {
        LocalDate dob = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now();
        return Period.between(dob, today).getYears();
    }

    /**
     * Validates age against the general rule (18-75) and optionally
     * against the Student account rule (18-25).
     * @return error message or null if valid
     */
    public static String validateAge(int year, int month, int day, String accountType) {
        int age = computeAge(year, month, day);
        boolean isStudent = "Student".equals(accountType);

        if (isStudent) {
            if (age < StudentAccountAdapter.MIN_AGE || age > StudentAccountAdapter.MAX_AGE) {
                return "Student account requires age between " +
                       StudentAccountAdapter.MIN_AGE + " and " +
                       StudentAccountAdapter.MAX_AGE + ". Your age: " + age;
            }
        } else {
            if (age < 18 || age > 75) {
                return "Age must be between 18 and 75. Your age: " + age;
            }
        }
        return null;
    }

    /**
     * Validates the opening deposit against the account's minimum requirement.
     * Uses polymorphism: the Account object's minimumDeposit() method is called,
     * and the actual subclass determines the minimum.
     * @return error message or null if valid
     */
    public static String validateDeposit(String depositText, model.Account account) {
        if (depositText == null || depositText.trim().isEmpty()) {
            return "Opening Deposit is required.";
        }
        try {
            double deposit = Double.parseDouble(depositText.trim().replace(",", ""));
            if (deposit < 0) {
                return "Opening Deposit cannot be negative.";
            }
            if (!account.isDepositValid()) {
                return String.format("Minimum deposit for %s is %,d UGX. You entered: %,.0f UGX.",
                        account.getAccountType(),
                        (long) account.minimumDeposit(),
                        deposit);
            }
            return null;
        } catch (NumberFormatException e) {
            return "Opening Deposit must be a valid number.";
        }
    }

    /**
     * Checks if a given year is a leap year.
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Returns the number of days in a given month/year.
     * @param month 1-based month (1=January, 12=December)
     */
    public static int getDaysInMonth(int year, int month) {
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                return isLeapYear(year) ? 29 : 28;
            default:
                return 31;
        }
    }

    /**
     * Formats a date from components as YYYY-MM-DD.
     */
    public static String formatDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month, day);
    }
}

/**
 * Adapter to access StudentAccount constants without circular dependency.
 * (Used by ValidationUtil for student age range checks.)
 */
class StudentAccountAdapter {
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 25;
}