package banksystem.exceptions;

/**
 * Exception lancée lorsqu'un retrait ou virement dépasse le solde disponible.
 */
public class InsufficientFundException extends Exception {

	/**
	 * Crée l'exception avec un message d'erreur.
	 *
	 * @param message description du problème
	 */
	public InsufficientFundException(String message) {
		super(message);
	}
}
