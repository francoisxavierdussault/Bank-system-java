package banksystem.business;

import banksystem.enums.EnumAccountType;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import banksystem.util.BankLogger;

/**
 * Compte épargne — génère des intérêts annuels à taux configurable.
 */
public class SavingsAccount extends Account {

    private static final BankLogger logger = BankLogger.getInstance();

    private double interestRate;
    private double monthlyGain;   // gains accumulés ce mois-ci
    private double totalGain;     // gains cumulés depuis l'ouverture

    public SavingsAccount() { super(); }

    public SavingsAccount(long accountNumber, Long customerNumber, double balance, double interestRate)
            throws NotInRangeException, NotNullException {
        super(accountNumber, customerNumber, balance, EnumAccountType.SAVINGS);
        setInterestRate(interestRate);
    }

    public double getInterestRate() { return interestRate; }

    public void setInterestRate(double interestRate) throws NotInRangeException {
        if (interestRate < 0 || interestRate > 1)
            throw new NotInRangeException("Le taux doit être entre 0 et 1 (ex. 0.025 pour 2.5%)");
        this.interestRate = interestRate;
    }

    public double getMonthlyGain()         { return monthlyGain; }
    public void   setMonthlyGain(double v) { this.monthlyGain = v; }
    public double getTotalGain()           { return totalGain; }
    public void   setTotalGain(double v)   { this.totalGain = v; }

    /** Réinitialise le compteur de gains mensuels (à appeler en début de mois). */
    public void resetMonthlyGain() { this.monthlyGain = 0; }

    /**
     * Calcule et dépose les intérêts mensuels sur le solde ({@code balance × interestRate / 12}).
     * Met à jour monthlyGain et totalGain.
     *
     * @implNote O(1) — une multiplication, un appel à deposit() et deux additions.
     */
    public void applyInterest() {
        double interest = balance * (interestRate / 12.0);
        double before   = balance;
        try {
            deposit(interest);
            monthlyGain += interest;
            totalGain   += interest;
            logger.info("Account#" + getAccountNumber(), "INTÉRÊT $" + String.format("%.2f", interest)
                    + " (" + (interestRate * 100) + "%) | Solde : $"
                    + String.format("%.2f", before) + " -> $" + String.format("%.2f", balance)
                    + " | GainMensuel : $" + String.format("%.2f", monthlyGain)
                    + " | GainTotal : $" + String.format("%.2f", totalGain));
        } catch (InsufficientFundException | NotInRangeException e) {
            logger.error("Account#" + getAccountNumber(), "Impossible d'appliquer l'intérêt : " + e.getMessage());
        }
    }

    public static void save(SavingsAccount account) {
        System.out.println("Saving savings account to DB: " + account.getAccountNumber());
    }

    public static SavingsAccount findById(long accountNumber) { return null; }

    public static void update(SavingsAccount account) {}

    public static void delete(long accountNumber) {}

    @Override
    public String toString() {
        return "SavingsAccount [#" + getAccountNumber() + "]"
                + " Balance: $" + String.format("%.2f", getBalance())
                + " | Interest Rate: " + (interestRate * 100) + "%"
                + " | Monthly Gain: $" + String.format("%.2f", monthlyGain)
                + " | Total Gain: $" + String.format("%.2f", totalGain)
                + " | Status: " + getStatus()
                + " | Customer: " + getCustomerNumber();
    }
}
