package banksystem.exceptions;

/**
 * Exception lancée lorsqu'une valeur est en dehors de la plage acceptée.
 */
public class NotInRangeException extends Exception {

	/**
	 * Crée l'exception avec un message d'erreur.
	 *
	 * @param message description du problème
	 */
	public NotInRangeException(String message) {
		super(message);
	}
}
