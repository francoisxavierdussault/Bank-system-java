package banksystem.business;

import banksystem.enums.EnumAccountStatus;
import banksystem.enums.EnumAccountType;
import banksystem.enums.EnumTransactionType;
import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import banksystem.util.BankLogger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Classe abstraite de base pour tous les types de comptes bancaires.
 */
public abstract class Account implements ITransaction {

    private static final BankLogger logger = BankLogger.getInstance();

    /** Limite de retrait journalier par défaut (peut être remplacée par compte). */
    public static final double DEFAULT_DAILY_LIMIT = 5000.00;

    private long              accountNumber;
    private Long              customerNumber;
    /** Solde — accessible par les sous-classes. */
    protected double          balance;
    private Date              openedDate;
    private EnumAccountType   type;
    private EnumAccountStatus status;
    private List<Transaction> transactions;

    /** Montant maximal pouvant être retiré ou transféré en un seul jour calendaire. */
    private double dailyWithdrawalLimit;
    /** Total cumulé des retraits/virements déjà effectués aujourd'hui. */
    protected double dailyWithdrawnToday;
    /** Date (yyyy-MM-dd) de la dernière réinitialisation de dailyWithdrawnToday. */
    private String dailyResetDate;

    public Account() {
        this.openedDate           = new Date();
        this.transactions         = new ArrayList<>();
        this.balance              = 0.0;
        this.status               = EnumAccountStatus.ACTIVE;
        this.dailyWithdrawalLimit = DEFAULT_DAILY_LIMIT;
        this.dailyWithdrawnToday  = 0.0;
        this.dailyResetDate       = todayString();
    }

    public Account(long accountNumber, Long customerNumber, double balance, EnumAccountType type)
            throws NotInRangeException, NotNullException {
        this();
        setAccountNumber(accountNumber);
        setCustomerNumber(customerNumber);
        setBalance(balance);
        setType(type);
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public long getAccountNumber() { return accountNumber; }

    public void setAccountNumber(long accountNumber) throws NotInRangeException {
        Validator.validateId(accountNumber);
        this.accountNumber = accountNumber;
    }

    public Long getCustomerNumber() { return customerNumber; }

    public void setCustomerNumber(Long customerNumber) throws NotInRangeException {
        if (customerNumber == null || customerNumber <= 0)
            throw new NotInRangeException("Le numéro de client doit être supérieur à 0");
        this.customerNumber = customerNumber;
    }

    public double getBalance() { return balance; }

    public void setBalance(double balance) throws NotInRangeException {
        if (balance < 0) throw new NotInRangeException("Le solde ne peut pas être négatif");
        this.balance = balance;
    }

    public Date getOpenedDate()            { return openedDate; }
    public void setOpenedDate(Date d)      { this.openedDate = d; }

    public EnumAccountType getType()              { return type; }
    public void setType(EnumAccountType type) throws NotNullException {
        if (type == null) throw new NotNullException("Le type de compte ne peut pas être null");
        this.type = type;
    }

    public EnumAccountStatus getStatus()              { return status; }
    public void setStatus(EnumAccountStatus status)   { this.status = status; }

    public double getDailyWithdrawalLimit()                { return dailyWithdrawalLimit; }
    public void   setDailyWithdrawalLimit(double limit)    { this.dailyWithdrawalLimit = limit; }

    public double getDailyWithdrawnToday() {
        resetDailyCounterIfNewDay();
        return dailyWithdrawnToday;
    }

    public List<Transaction> getTransactions() { return transactions; }

    // -------------------------------------------------------------------------
    // Implémentations de ITransaction
    // -------------------------------------------------------------------------

    /**
     * Crédite le montant sur le solde du compte et enregistre la transaction.
     *
     * @param amount montant à déposer (doit être &gt; 0)
     * @throws InsufficientFundException si le compte est gelé ou fermé
     * @throws NotInRangeException       si le montant est invalide
     * @implNote O(1) — toutes les opérations sont en temps constant.
     */
    @Override
    public void deposit(double amount) throws InsufficientFundException, NotInRangeException {
        checkActive("Dépôt");
        Validator.validateAmount(amount);
        double before = this.balance;
        this.balance += amount;
        try {
            Transaction t = new Transaction(transactions.size() + 1, "Dépôt", amount, EnumTransactionType.DEPOSIT);
            transactions.add(t);
            logger.info("Compte#" + accountNumber, "DÉPÔT $" + fmt(amount)
                    + " | Solde : $" + fmt(before) + " -> $" + fmt(this.balance));
        } catch (Exception e) {
            logger.error("Compte#" + accountNumber, "Erreur lors de l'enregistrement du dépôt : " + e.getMessage());
        }
    }

    /**
     * Débite le montant du solde du compte, vérifie la limite journalière et enregistre la transaction.
     *
     * @param amount montant à retirer (doit être &gt; 0)
     * @throws InsufficientFundException si le compte est gelé/fermé ou si le solde est insuffisant
     * @throws NotInRangeException       si le montant est invalide ou si la limite journalière est dépassée
     * @implNote O(1) — toutes les opérations sont en temps constant.
     */
    @Override
    public void withdraw(double amount) throws InsufficientFundException, NotInRangeException {
        checkActive("Retrait");
        Validator.validateAmount(amount);
        Validator.validateBalance(this.balance, amount);
        checkDailyLimit(amount);
        double before = this.balance;
        this.balance -= amount;
        dailyWithdrawnToday += amount;
        try {
            Transaction t = new Transaction(transactions.size() + 1, "Retrait", amount, EnumTransactionType.WITHDRAWAL);
            transactions.add(t);
            logger.info("Compte#" + accountNumber, "RETRAIT $" + fmt(amount)
                    + " | Solde : $" + fmt(before) + " -> $" + fmt(this.balance)
                    + " | Total jour : $" + fmt(dailyWithdrawnToday) + "/" + fmt(dailyWithdrawalLimit));
        } catch (Exception e) {
            logger.error("Compte#" + accountNumber, "Erreur lors de l'enregistrement du retrait : " + e.getMessage());
        }
    }

    @Override
    public double checkBalance() { return this.balance; }

    /**
     * Transfère le montant du compte source vers le compte destination,
     * vérifie la limite journalière et enregistre la transaction des deux côtés.
     *
     * @param amount      montant à transférer (doit être &gt; 0)
     * @param destination compte destinataire
     * @throws InsufficientFundException si le compte est gelé/fermé ou si le solde est insuffisant
     * @throws NotInRangeException       si le montant est invalide ou si la limite journalière est dépassée
     * @implNote O(1) — toutes les opérations sont en temps constant.
     */
    @Override
    public void transfert(double amount, Account destination) throws InsufficientFundException, NotInRangeException {
        checkActive("Virement");
        Validator.validateAmount(amount);
        Validator.validateBalance(this.balance, amount);
        checkDailyLimit(amount);
        double srcBefore  = this.balance;
        double destBefore = destination.balance;
        this.balance -= amount;
        destination.balance += amount;
        dailyWithdrawnToday += amount;
        try {
            Transaction tSrc = new Transaction(transactions.size() + 1,
                    "Virement vers le compte #" + destination.getAccountNumber(), amount, EnumTransactionType.TRANSFER);
            transactions.add(tSrc);
            Transaction tDst = new Transaction(destination.transactions.size() + 1,
                    "Virement reçu du compte #" + accountNumber, amount, EnumTransactionType.TRANSFER);
            destination.transactions.add(tDst);
            logger.info("Compte#" + accountNumber, "VIREMENT $" + fmt(amount)
                    + " vers Compte#" + destination.getAccountNumber()
                    + " | Source : $" + fmt(srcBefore) + " -> $" + fmt(this.balance)
                    + " | Dest : $" + fmt(destBefore) + " -> $" + fmt(destination.balance));
        } catch (Exception e) {
            logger.error("Compte#" + accountNumber, "Erreur lors de l'enregistrement du virement : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes utilitaires
    // -------------------------------------------------------------------------

    /** Lance InsufficientFundException si le compte n'est pas ACTIF. */
    protected void checkActive(String operation) throws InsufficientFundException {
        if (status == EnumAccountStatus.FROZEN) {
            logger.warn("Compte#" + accountNumber, operation + " bloquée — compte GELé");
            throw new InsufficientFundException("Compte #" + accountNumber + " gelé. Contactez votre gestionnaire.");
        }
        if (status == EnumAccountStatus.CLOSED) {
            logger.warn("Compte#" + accountNumber, operation + " bloquée — compte FERMÉ");
            throw new InsufficientFundException("Compte #" + accountNumber + " est fermé.");
        }
    }

    /** Lance NotInRangeException si le montant dépasse la limite journalière. */
    protected void checkDailyLimit(double amount) throws NotInRangeException {
        resetDailyCounterIfNewDay();
        double newTotal = dailyWithdrawnToday + amount;
        if (newTotal > dailyWithdrawalLimit) {
            logger.warn("Compte#" + accountNumber, "Limite journalière dépassée — demandé $" + fmt(amount)
                    + " | Déjà retiré : $" + fmt(dailyWithdrawnToday)
                    + " | Limite : $" + fmt(dailyWithdrawalLimit));
            throw new NotInRangeException(
                    "Limite journalière de $" + fmt(dailyWithdrawalLimit) + " dépassée. "
                    + "Déjà retiré : $" + fmt(dailyWithdrawnToday)
                    + ". Disponible aujourd'hui : $" + fmt(dailyWithdrawalLimit - dailyWithdrawnToday));
        }
    }

    private void resetDailyCounterIfNewDay() {
        String today = todayString();
        if (!today.equals(dailyResetDate)) {
            dailyWithdrawnToday = 0.0;
            dailyResetDate      = today;
        }
    }

    private static String todayString() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    @Override
    public String toString() {
        return "Compte [" + accountNumber + "] Type : " + type
                + " | Statut : " + status
                + " | Solde : $" + fmt(balance)
                + " | Client : " + customerNumber
                + " | Ouvert : " + openedDate
                + " | Limite jour : $" + fmt(dailyWithdrawalLimit);
    }
}
