package banksystem.business;

import banksystem.exceptions.InsufficientFundException;
import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;

/**
 * Classe utilitaire de validation des données.
 * Toutes les méthodes sont statiques — aucune instanciation requise.
 */
public class Validator {

	/**
	 * Constructeur privé — classe utilitaire, ne doit pas être instanciée.
	 */
	private Validator() {}

	/**
	 * Vérifie qu'une chaîne n'est pas null ou vide.
	 *
	 * @param value     valeur à valider
	 * @param fieldName nom du champ (utilisé dans le message d'erreur)
	 * @throws NotNullException si la valeur est null ou vide
	 * @implNote O(1) — une seule comparaison et un trim().isEmpty().
	 */
	public static void validateNotNull(String value, String fieldName) throws NotNullException {
		if (value == null || value.trim().isEmpty()) {
			throw new NotNullException(fieldName + " cannot be null or empty");
		}
	}

	/**
	 * Vérifie qu'un montant est un nombre positif.
	 *
	 * @param amount montant à valider
	 * @throws NotInRangeException si le montant est inférieur ou égal à zéro
	 * @implNote O(1) — une seule comparaison numérique.
	 */
	public static void validateAmount(double amount) throws NotInRangeException {
		if (amount <= 0) {
			throw new NotInRangeException("Amount must be greater than 0");
		}
	}

	/**
	 * Vérifie qu'un solde est suffisant pour effectuer une opération.
	 *
	 * @param balance solde disponible
	 * @param amount  montant demandé
	 * @throws InsufficientFundException si le montant dépasse le solde
	 * @implNote O(1) — une seule comparaison numérique.
	 */
	public static void validateBalance(double balance, double amount) throws InsufficientFundException {
		if (amount > balance) {
			throw new InsufficientFundException("Insufficient funds. Current balance: $" + String.format("%.2f", balance));
		}
	}

	/**
	 * Vérifie qu'un identifiant est valide (doit être supérieur à zéro).
	 *
	 * @param id identifiant à valider
	 * @throws NotInRangeException si l'identifiant est inférieur ou égal à zéro
	 * @implNote O(1) — une seule comparaison numérique.
	 */
	public static void validateId(long id) throws NotInRangeException {
		if (id <= 0) {
			throw new NotInRangeException("ID must be greater than 0");
		}
	}
}
