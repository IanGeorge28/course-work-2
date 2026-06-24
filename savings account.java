package model;

/**
 * Savings Account: Minimum deposit 50,000 UGX.
 * Earns interest, no overdraft facility.
 */
public class SavingsAccount extends Account {

    private static final double MIN_DEPOSIT = 50_000;

    public SavingsAccount(double openingDeposit) {
        super("Savings", openingDeposit);
    }

    @Override
    public double minimumDeposit() {
        return MIN_DEPOSIT;
    }

    @Override
    public String getSpecialRule() {
        return "Earns interest, no overdraft";
    }

    @Override
    public boolean hasAdditionalRequirements() {
        return false;
    }
}