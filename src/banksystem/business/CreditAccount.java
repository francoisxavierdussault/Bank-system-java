package banksystem.business;

import banksystem.enums.EnumAccountType;
import banksystem.enums.EnumTransactionType;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import banksystem.util.BankLogger;

/**
 * Compte crédit — autorise les retraits jusqu'à (solde + limiteCrédit).
 */
public class CreditAccount extends Account {

    private static final BankLogger logger = BankLogger.getInstance();

    private double creditLimit;
    private double interestRate;       // taux annuel, ex : 0.20 = 20 %
    private double minPaymentPercent;  // paiement minimum mensuel en %, ex : 2.0
    private double paymentDueAmount;   // montant dû ce cycle
    private double penaltyAmount;      // pénalité si paiement manqué
    private double totalInterestPaid;
    private double totalPenaltiesPaid;

    public CreditAccount() {
        super();
        this.interestRate      = 0.20;
        this.minPaymentPercent = 2.0;
    }

    public CreditAccount(long accountNumber, Long customerNumber, double balance, double creditLimit)
            throws NotInRangeException, NotNullException {
        super(accountNumber, customerNumber, balance, EnumAccountType.CREDIT);
        setCreditLimit(creditLimit);
        this.interestRate      = 0.20;
        this.minPaymentPercent = 2.0;
    }

    @Override
    public void setBalance(double balance) throws NotInRangeException {
        // Un compte crédit peut avoir un solde négatif (montant dû)
        this.balance = balance;
    }

    public double getCreditLimit() { return creditLimit; }

    public void setCreditLimit(double creditLimit) throws NotInRangeException {
        if (creditLimit <= 0)
            throw new NotInRangeException("La limite de crédit doit être supérieure à 0");
        this.creditLimit = creditLimit;
    }

    public double getInterestRate()             { return interestRate; }
    public void   setInterestRate(double v) throws NotInRangeException {
        if (v < 0 || v > 1) throw new NotInRangeException("Le taux doit être entre 0 et 1");
        this.interestRate = v;
    }

    public double getMinPaymentPercent()        { return minPaymentPercent; }
    public void   setMinPaymentPercent(double v){ this.minPaymentPercent = v; }

    public double getPaymentDueAmount()         { return paymentDueAmount; }
    public void   setPaymentDueAmount(double v) { this.paymentDueAmount = v; }

    public double getPenaltyAmount()            { return penaltyAmount; }
    public void   setPenaltyAmount(double v)    { this.penaltyAmount = v; }

    public double getTotalInterestPaid()        { return totalInterestPaid; }
    public void   setTotalInterestPaid(double v){ this.totalInterestPaid = v; }

    public double getTotalPenaltiesPaid()           { return totalPenaltiesPaid; }
    public void   setTotalPenaltiesPaid(double v)   { this.totalPenaltiesPaid = v; }

    /**
     * Applique les intérêts mensuels sur le solde dû (solde &lt; 0).
     * Calcule le paiement minimum et enregistre la charge.
     *
     * @implNote O(1) — calcul numérique simple + enregistrement d'une transaction en mémoire.
     */
    public void applyMonthlyInterest() {
        double owed = balance < 0 ? Math.abs(balance) : 0;
        if (owed == 0) return;

        double monthlyRate = interestRate / 12.0;
        double interest    = owed * monthlyRate;
        this.balance      -= interest;    // balance becomes more negative (more owed)
        this.totalInterestPaid += interest;
        this.paymentDueAmount   = Math.max(owed * (minPaymentPercent / 100.0), interest);

        try {
            Transaction t = new Transaction(
                getTransactions().size() + 1,
                String.format("Intérêt mensuel %.2f%% sur $%.2f dû", interestRate * 100, owed),
                interest, EnumTransactionType.WITHDRAWAL);
            getTransactions().add(t);
        } catch (Exception e) {
            logger.error("Account#" + getAccountNumber(), "Erreur enregistrement intérêt : " + e.getMessage());
        }
        logger.info("Account#" + getAccountNumber(),
            String.format("INTÉRÊT CRÉDIT $%.2f | Dû : $%.2f | PaiementMin : $%.2f",
                interest, Math.abs(balance), paymentDueAmount));
    }

    /**
     * Applique une pénalité de retard (25 % du montant dû) si le paiement minimum n'a pas été effectué.
     *
     * @implNote O(1) — calcul numérique simple + enregistrement d'une transaction en mémoire.
     */
    public void applyLatePenalty() {
        double penalty = paymentDueAmount * 0.25;
        if (penalty == 0) return;
        this.balance           -= penalty;
        this.penaltyAmount      = penalty;
        this.totalPenaltiesPaid += penalty;

        try {
            Transaction t = new Transaction(
                getTransactions().size() + 1,
                String.format("Pénalité retard (25%% de $%.2f dû)", paymentDueAmount),
                penalty, EnumTransactionType.WITHDRAWAL);
            getTransactions().add(t);
        } catch (Exception e) {
            logger.error("Account#" + getAccountNumber(), "Erreur enregistrement pénalité : " + e.getMessage());
        }
        logger.warn("Account#" + getAccountNumber(),
            String.format("PÉNALITÉ RETARD $%.2f | Total pénalités : $%.2f", penalty, totalPenaltiesPaid));
    }

    /**
     * Retrait respectant la limite de crédit plutôt que le seul solde.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundException, NotInRangeException {
        checkActive("Retrait crédit");
        Validator.validateAmount(amount);
        checkDailyLimit(amount);
        double available = balance + creditLimit;
        if (amount > available) {
            logger.warn("Account#" + getAccountNumber(), "RETRAIT CRÉDIT refusé — $"
                    + String.format("%.2f", amount) + " dépasse le disponible $" + String.format("%.2f", available));
            throw new InsufficientFundException(
                    "Montant dépasse la limite de crédit. Maximum disponible : $" + String.format("%.2f", available));
        }
        double before = balance;
        balance -= amount;
        dailyWithdrawnToday += amount;
        try {
            Transaction t = new Transaction(getTransactions().size() + 1, "Retrait", amount,
                    EnumTransactionType.WITHDRAWAL);
            getTransactions().add(t);
            logger.info("Account#" + getAccountNumber(), "RETRAIT CRÉDIT $" + String.format("%.2f", amount)
                    + " | Solde : $" + String.format("%.2f", before) + " -> $" + String.format("%.2f", balance)
                    + " | Limite : $" + String.format("%.2f", creditLimit));
        } catch (Exception e) {
            logger.error("Account#" + getAccountNumber(), "Erreur enregistrement retrait crédit : " + e.getMessage());
        }
    }

    public static void save(CreditAccount account) {
        System.out.println("Saving credit account to DB: " + account.getAccountNumber());
    }

    public static CreditAccount findById(long accountNumber) { return null; }

    public static void update(CreditAccount account) {}

    public static void delete(long accountNumber) {}

    @Override
    public String toString() {
        return "CreditAccount [#" + getAccountNumber() + "]"
            + " Balance: $" + String.format("%.2f", getBalance())
            + " | Limit: $" + String.format("%.2f", creditLimit)
            + " | Rate: " + (interestRate * 100) + "%"
            + " | PaymentDue: $" + String.format("%.2f", paymentDueAmount)
            + " | Penalty: $" + String.format("%.2f", penaltyAmount)
            + " | Status: " + getStatus()
            + " | Customer: " + getCustomerNumber();
    }
}
