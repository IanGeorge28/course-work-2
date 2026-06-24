package model;

/**
 * Fixed Deposit Account: Minimum deposit 1,000,000 UGX.
 * Locked term, highest interest rate.
 */
public class FixedDepositAccount extends Account {

    private static final double MIN_DEPOSIT = 1_000_000;

    public FixedDepositAccount(double openingDeposit) {
        super("Fixed Deposit", openingDeposit);
    }

    @Override
    public double minimumDeposit() {
        return MIN_DEPOSIT;
    }

    @Override
    public String getSpecialRule() {
        return "Locked term, highest interest";
    }

    @Override
    public boolean hasAdditionalRequirements() {
        return false;
    }
}