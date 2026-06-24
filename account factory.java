package model;

/**
 * Factory class that creates the appropriate Account subtype based on
 * the selected account type string. Demonstrates the Factory design pattern
 * and returns the concrete object through the abstract Account reference,
 * enabling polymorphic behaviour.
 */
public class AccountFactory {

    /**
     * Creates and returns the correct Account subclass instance.
     * @param accountType the selected account type name
     * @param openingDeposit the deposit amount entered
     * @return the concrete Account object
     * @throws IllegalArgumentException if the account type is unknown
     */
    public static Account createAccount(String accountType, double openingDeposit) {
        if (accountType == null || accountType.trim().isEmpty()) {
            throw new IllegalArgumentException("Account type must be selected.");
        }

        switch (accountType.trim()) {
            case "Savings":
                return new SavingsAccount(openingDeposit);
            case "Current":
                return new CurrentAccount(openingDeposit);
            case "Fixed Deposit":
                return new FixedDepositAccount(openingDeposit);
            case "Student":
                return new StudentAccount(openingDeposit);
            case "Joint":
                return new JointAccount(openingDeposit);
            default:
                throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }
}