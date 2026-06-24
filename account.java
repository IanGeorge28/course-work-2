package model;

/**
 * Abstract base class for all bank account types.
 * Defines common state and an abstract method for minimum deposit,
 * which each concrete subclass must override (polymorphism).
 */
public abstract class Account {

    protected String accountType;
    protected double openingDeposit;

    /**
     * Constructs an Account with the given type and deposit.
     * @param accountType the name of the account type
     * @param openingDeposit the initial deposit amount in UGX
     */
    public Account(String accountType, double openingDeposit) {
        this.accountType = accountType;
        this.openingDeposit = openingDeposit;
    }

    /**
     * Each account subtype defines its own minimum opening deposit.
     * @return the minimum required deposit in UGX
     */
    public abstract double minimumDeposit();

    /**
     * Returns a human-readable description of the special rule for this account type.
     * @return special rule description
     */
    public abstract String getSpecialRule();

    /**
     * Indicates whether this account type has additional validation requirements
     * beyond the standard deposit check (e.g., age for Student, second NIN for Joint).
     * @return true if additional requirements exist
     */
    public abstract boolean hasAdditionalRequirements();

    /**
     * Checks if the opening deposit meets or exceeds the minimum required.
     * @return true if deposit is valid
     */
    public boolean isDepositValid() {
        return openingDeposit >= minimumDeposit();
    }

    public String getAccountType() {
        return accountType;
    }

    public double getOpeningDeposit() {
        return openingDeposit;
    }

    @Override
    public String toString() {
        return accountType + " (Min: " + String.format("%,d", (long) minimumDeposit()) + " UGX)";
    }
}