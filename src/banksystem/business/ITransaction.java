package banksystem.business;

import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;

/**
 * Interface définissant les opérations bancaires que tout compte doit supporter.
 */
public interface ITransaction {

	/**
	 * Effectue un retrait du compte.
	 *
	 * @param amount montant à retirer
	 * @throws InsufficientFundException si le solde est insuffisant
	 * @throws NotInRangeException       si le montant est invalide
	 */
	void withdraw(double amount) throws InsufficientFundException, NotInRangeException;

	/**
	 * Effectue un dépôt dans le compte.
	 *
	 * @param amount montant à déposer
	 * @throws NotInRangeException si le montant est invalide
	 */
	void deposit(double amount) throws InsufficientFundException, NotInRangeException;

	/**
	 * Retourne le solde actuel du compte.
	 *
	 * @return solde du compte
	 */
	double checkBalance();

	/**
	 * Transfère un montant vers un autre compte.
	 *
	 * @param amount      montant à transférer
	 * @param destination compte destinataire
	 * @throws InsufficientFundException si le solde est insuffisant
	 * @throws NotInRangeException       si le montant est invalide
	 */
	void transfert(double amount, Account destination) throws InsufficientFundException, NotInRangeException;
}
