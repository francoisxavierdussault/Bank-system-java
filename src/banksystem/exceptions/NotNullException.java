package banksystem.exceptions;

/**
 * Exception lancée lorsqu'un champ obligatoire est null ou vide.
 */
public class NotNullException extends Exception {

	/**
	 * Crée l'exception avec un message d'erreur.
	 *
	 * @param message description du problème
	 */
	public NotNullException(String message) {
		super(message);
	}
}
