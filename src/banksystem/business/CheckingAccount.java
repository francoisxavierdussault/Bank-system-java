package banksystem.business;

import banksystem.enums.EnumAccountType;
import banksystem.enums.EnumTransactionType;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import banksystem.util.BankLogger;

/**
 * Compte chèque — 3 transactions gratuites par mois.
 * Toute transaction supplémentaire entraîne des frais de 30% du montant.
 */
public class CheckingAccount extends Account {

    private static final BankLogger logger = BankLogger.getInstance();

    /** Nombre de transactions gratuites par mois (défaut : 3). */
    private int    monthlyFreeTransactions;

    /** Pourcentage de frais pour les transactions au-delà de la limite (défaut : 30%). */
    private double txFeePercent;

    /** Nombre de transactions effectuées ce mois-ci. */
    private int    txUsedThisMonth;

    /** Mois courant de référence (format YYYY-MM) pour réinitialiser le compteur. */
    private String currentMonth;

    /** Total des frais perçus ce mois-ci. */
    private double feesChargedThisMonth;

    /** Total des frais perçus depuis l'ouverture. */
    private double totalFeesCharged;

    public CheckingAccount() {
        super();
        this.monthlyFreeTransactions = 3;
        this.txFeePercent            = 30.0;
        this.txUsedThisMonth         = 0;
        this.currentMonth            = currentMonthString();
    }

    public CheckingAccount(long accountNumber, Long customerNumber, double balance, int monthlyFreeTransactions)
            throws NotInRangeException, NotNullException {
        super(accountNumber, customerNumber, balance, EnumAccountType.CHECKING);
        setMonthlyFreeTransactions(monthlyFreeTransactions);
        this.txFeePercent    = 30.0;
        this.txUsedThisMonth = 0;
        this.currentMonth    = currentMonthString();
    }

    // -------------------------------------------------------------------------
    // Overrides — frais de 30 % si la limite mensuelle est dépassée
    // -------------------------------------------------------------------------

    @Override
    public void deposit(double amount) throws InsufficientFundException, NotInRangeException {
        resetMonthCounterIfNewMonth();
        super.deposit(amount);
        txUsedThisMonth++;
        double fee = applyFeeIfOverLimit(amount, "Deposit");
        logger.info("Account#" + getAccountNumber(),
            "CHQ DÉPÔT $" + fmt(amount) + " | TX#" + txUsedThisMonth
            + "/" + monthlyFreeTransactions + " | Frais : $" + fmt(fee));
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundException, NotInRangeException {
        resetMonthCounterIfNewMonth();
        double fee = computeFeeIfOverLimit(amount);
        // Validation du montant combiné avant toute opération
        Validator.validateBalance(this.balance, amount + fee);
        super.withdraw(amount);
        txUsedThisMonth++;
        if (fee > 0) deductFee(fee, "Retrait");
        logger.info("Account#" + getAccountNumber(),
            "CHQ RETRAIT $" + fmt(amount) + " | TX#" + txUsedThisMonth
            + "/" + monthlyFreeTransactions + " | Frais : $" + fmt(fee));
    }

    @Override
    public void transfert(double amount, Account destination) throws InsufficientFundException, NotInRangeException {
        resetMonthCounterIfNewMonth();
        double fee = computeFeeIfOverLimit(amount);
        Validator.validateBalance(this.balance, amount + fee);
        super.transfert(amount, destination);
        txUsedThisMonth++;
        if (fee > 0) deductFee(fee, "Virement");
        logger.info("Account#" + getAccountNumber(),
            "CHQ VIREMENT $" + fmt(amount) + " -> CPT#" + destination.getAccountNumber()
            + " | TX#" + txUsedThisMonth + "/" + monthlyFreeTransactions + " | Frais : $" + fmt(fee));
    }

    // -------------------------------------------------------------------------
    // Calcul des frais
    // -------------------------------------------------------------------------

    private double computeFeeIfOverLimit(double amount) {
        resetMonthCounterIfNewMonth();
        if (txUsedThisMonth >= monthlyFreeTransactions) {
            return amount * (txFeePercent / 100.0);
        }
        return 0;
    }

    private double applyFeeIfOverLimit(double amount, String opType) {
        if (txUsedThisMonth > monthlyFreeTransactions) {
            double fee = amount * (txFeePercent / 100.0);
            deductFee(fee, opType);
            return fee;
        }
        return 0;
    }

    private void deductFee(double fee, String opType) {
        this.balance          -= fee;
        this.feesChargedThisMonth += fee;
        this.totalFeesCharged     += fee;
        try {
            banksystem.business.Transaction t = new banksystem.business.Transaction(
                getTransactions().size() + 1,
                "Frais dépassement limite (30%) — " + opType,
                fee, EnumTransactionType.WITHDRAWAL);
            getTransactions().add(t);
        } catch (Exception e) {
            logger.error("Account#" + getAccountNumber(), "Erreur enregistrement frais : " + e.getMessage());
        }
        logger.warn("Account#" + getAccountNumber(),
            "FRAIS DÉPASSEMENT $" + fmt(fee) + " (" + txFeePercent + "% de la TX)"
            + " | Total frais ce mois : $" + fmt(feesChargedThisMonth));
    }

    // -------------------------------------------------------------------------
    // Réinitialisation mensuelle
    // -------------------------------------------------------------------------

    private void resetMonthCounterIfNewMonth() {
        String month = currentMonthString();
        if (!month.equals(currentMonth)) {
            txUsedThisMonth       = 0;
            feesChargedThisMonth  = 0;
            currentMonth          = month;
        }
    }

    private static String currentMonthString() {
        return new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date());
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    // -------------------------------------------------------------------------
    // Accesseurs / Mutateurs
    // -------------------------------------------------------------------------

    public int    getMonthlyFreeTransactions()          { return monthlyFreeTransactions; }
    public void   setMonthlyFreeTransactions(int v) throws NotInRangeException {
        if (v < 0) throw new NotInRangeException("Le nombre de transactions gratuites ne peut pas être négatif");
        this.monthlyFreeTransactions = v;
    }

    public double getTxFeePercent()                     { return txFeePercent; }
    public void   setTxFeePercent(double v)             { this.txFeePercent = v; }

    public int    getTxUsedThisMonth()                  { return txUsedThisMonth; }
    public void   setTxUsedThisMonth(int v)             { this.txUsedThisMonth = v; }

    public String getCurrentMonth()                     { return currentMonth; }
    public void   setCurrentMonth(String v)             { this.currentMonth = v; }

    public double getFeesChargedThisMonth()             { return feesChargedThisMonth; }
    public double getTotalFeesCharged()                 { return totalFeesCharged; }

    public static void save(CheckingAccount account)    { System.out.println("Saving checking account: " + account.getAccountNumber()); }
    public static CheckingAccount findById(long id)     { return null; }
    public static void update(CheckingAccount account)  {}
    public static void delete(long accountNumber)       {}

    @Override
    public String toString() {
        return "CheckingAccount [#" + getAccountNumber() + "]"
            + " Balance: $" + fmt(getBalance())
            + " | TX: " + txUsedThisMonth + "/" + monthlyFreeTransactions + " free"
            + " | Fees this month: $" + fmt(feesChargedThisMonth)
            + " | Customer: " + getCustomerNumber();
    }
}
