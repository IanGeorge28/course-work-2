package model;

/**
 * Joint Account: Minimum deposit 100,000 UGX.
 * Additional requirement: a second National ID (NIN) must be provided.
 */
public class JointAccount extends Account {

    private static final double MIN_DEPOSIT = 100_000;

    public JointAccount(double openingDeposit) {
        super("Joint", openingDeposit);
    }

    @Override
    public double minimumDeposit() {
        return MIN_DEPOSIT;
    }

    @Override
    public String getSpecialRule() {
        return "Requires a second NIN";
    }

    @Override
    public boolean hasAdditionalRequirements() {
        return true;
    }

    /**
     * Validates the second NIN for the joint account holder.
     * @param secondNin the second National ID number
     * @return true if the second NIN is valid (14 alphanumeric, uppercase)
     */
    public boolean isSecondNinValid(String secondNin) {
        return secondNin != null && secondNin.matches("^[A-Z0-9]{14}$");
    }
}