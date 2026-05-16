package banksystem.business;

import banksystem.enums.EnumTransactionType;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import java.util.Date;

/**
 * Représente une transaction bancaire (dépôt, retrait ou virement).
 */
public class Transaction {

	private long transactionNumber;
	private String description;
	private Double amount;
	private Date transactionDate;
	private EnumTransactionType transactionType;

	/**
	 * Constructeur par défaut.
	 * Initialise la date de transaction à aujourd'hui.
	 */
	public Transaction() {
		this.transactionDate = new Date();
	}

	/**
	 * Constructeur avec paramètres.
	 *
	 * @param transactionNumber numéro unique de la transaction
	 * @param description       description de la transaction
	 * @param amount            montant de la transaction
	 * @param transactionType   type de transaction
	 * @throws NotNullException    si un champ obligatoire est null
	 * @throws NotInRangeException si une valeur est hors limites
	 */
	public Transaction(long transactionNumber, String description, Double amount, EnumTransactionType transactionType)
			throws NotNullException, NotInRangeException {
		this();
		setTransactionNumber(transactionNumber);
		setDescription(description);
		setAmount(amount);
		setTransactionType(transactionType);
	}

	/**
	 * Retourne le numéro de la transaction.
	 *
	 * @return numéro de la transaction
	 */
	public long getTransactionNumber() {
		return transactionNumber;
	}

	/**
	 * Modifie le numéro de la transaction.
	 *
	 * @param transactionNumber numéro de la transaction
	 * @throws NotInRangeException si le numéro est invalide
	 */
	public void setTransactionNumber(long transactionNumber) throws NotInRangeException {
		Validator.validateId(transactionNumber);
		this.transactionNumber = transactionNumber;
	}

	/**
	 * Retourne la description de la transaction.
	 *
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Modifie la description de la transaction.
	 *
	 * @param description description de la transaction
	 * @throws NotNullException si la description est null ou vide
	 */
	public void setDescription(String description) throws NotNullException {
		Validator.validateNotNull(description, "Description");
		this.description = description;
	}

	/**
	 * Retourne le montant de la transaction.
	 *
	 * @return montant
	 */
	public Double getAmount() {
		return amount;
	}

	/**
	 * Modifie le montant de la transaction.
	 *
	 * @param amount montant de la transaction
	 * @throws NotInRangeException si le montant est invalide
	 */
	public void setAmount(Double amount) throws NotInRangeException {
		Validator.validateAmount(amount);
		this.amount = amount;
	}

	/**
	 * Retourne la date de la transaction.
	 *
	 * @return date de la transaction
	 */
	public Date getTransactionDate() {
		return transactionDate;
	}

	/**
	 * Modifie la date de la transaction.
	 *
	 * @param transactionDate nouvelle date de transaction
	 */
	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	/**
	 * Retourne le type de la transaction.
	 *
	 * @return type de transaction (DEPOSIT, WITHDRAWAL, TRANSFER)
	 */
	public EnumTransactionType getTransactionType() {
		return transactionType;
	}

	/**
	 * Modifie le type de la transaction.
	 *
	 * @param transactionType type de transaction
	 * @throws NotNullException si le type est null
	 */
	public void setTransactionType(EnumTransactionType transactionType) throws NotNullException {
		if (transactionType == null) {
			throw new NotNullException("Transaction type cannot be null");
		}
		this.transactionType = transactionType;
	}

	/**
	 * Retourne une représentation textuelle de la transaction.
	 *
	 * @return chaîne décrivant la transaction
	 */
	@Override
	public String toString() {
		return "[#" + transactionNumber + "] " + transactionType + " - $" + String.format("%.2f", amount)
				+ " | " + description + " | " + transactionDate;
	}
}
