package model;

/**
 * Current Account: Minimum deposit 200,000 UGX.
 * Overdraft allowed, no interest earned.
 */
public class CurrentAccount extends Account {

    private static final double MIN_DEPOSIT = 200_000;

    public CurrentAccount(double openingDeposit) {
        super("Current", openingDeposit);
    }

    @Override
    public double minimumDeposit() {
        return MIN_DEPOSIT;
    }

    @Override
    public String getSpecialRule() {
        return "Overdraft allowed, no interest";
    }

    @Override
    public boolean hasAdditionalRequirements() {
        return false;
    }
}