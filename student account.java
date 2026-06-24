package model;

/**
 * Student Account: Minimum deposit 10,000 UGX.
 * Additional requirement: applicant age must be between 18 and 25 (inclusive).
 */
public class StudentAccount extends Account {

    private static final double MIN_DEPOSIT = 10_000;
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 25;

    public StudentAccount(double openingDeposit) {
        super("Student", openingDeposit);
    }

    @Override
    public double minimumDeposit() {
        return MIN_DEPOSIT;
    }

    @Override
    public String getSpecialRule() {
        return "Applicant age must be " + MIN_AGE + "-" + MAX_AGE;
    }

    @Override
    public boolean hasAdditionalRequirements() {
        return true;
    }

    /**
     * Validates that the applicant's age falls within the student range.
     * @param age the computed age of the applicant
     * @return true if age is within 18-25 inclusive
     */
    public boolean isAgeValid(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }
}