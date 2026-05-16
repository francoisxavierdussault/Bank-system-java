package banksystem.business;

import banksystem.exceptions.NotInRangeException;
import banksystem.exceptions.NotNullException;
import java.util.Date;

/**
 * Représente un client de la banque.
 */
public class Customer {

	private long customerNumber;
	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
	private Date dateCreated;

	/**
	 * Constructeur par défaut.
	 * Initialise la date de création à aujourd'hui.
	 */
	public Customer() {
		this.dateCreated = new Date();
	}

	/**
	 * Constructeur avec paramètres.
	 *
	 * @param customerNumber numéro unique du client
	 * @param firstName      prénom
	 * @param lastName       nom de famille
	 * @param email          adresse courriel
	 * @param phoneNumber    numéro de téléphone
	 * @throws NotNullException    si un champ obligatoire est null
	 * @throws NotInRangeException si une valeur est hors limites
	 */
	public Customer(long customerNumber, String firstName, String lastName, String email, String phoneNumber)
			throws NotNullException, NotInRangeException {
		this();
		setCustomerNumber(customerNumber);
		setFirstName(firstName);
		setLastName(lastName);
		setEmail(email);
		setPhoneNumber(phoneNumber);
	}

	/**
	 * Retourne le numéro du client.
	 *
	 * @return numéro du client
	 */
	public long getCustomerNumber() {
		return customerNumber;
	}

	/**
	 * Modifie le numéro du client.
	 *
	 * @param customerNumber numéro du client
	 * @throws NotInRangeException si le numéro est invalide
	 */
	public void setCustomerNumber(long customerNumber) throws NotInRangeException {
		Validator.validateId(customerNumber);
		this.customerNumber = customerNumber;
	}

	/**
	 * Retourne le prénom du client.
	 *
	 * @return prénom
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Modifie le prénom du client.
	 *
	 * @param firstName prénom
	 * @throws NotNullException si le prénom est null ou vide
	 */
	public void setFirstName(String firstName) throws NotNullException {
		Validator.validateNotNull(firstName, "First name");
		this.firstName = firstName;
	}

	/**
	 * Retourne le nom de famille du client.
	 *
	 * @return nom de famille
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Modifie le nom de famille du client.
	 *
	 * @param lastName nom de famille
	 * @throws NotNullException si le nom est null ou vide
	 */
	public void setLastName(String lastName) throws NotNullException {
		Validator.validateNotNull(lastName, "Last name");
		this.lastName = lastName;
	}

	/**
	 * Retourne l'adresse courriel du client.
	 *
	 * @return adresse courriel
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Modifie l'adresse courriel du client.
	 *
	 * @param email adresse courriel
	 * @throws NotNullException si le courriel est null ou vide
	 */
	public void setEmail(String email) throws NotNullException {
		Validator.validateNotNull(email, "Email");
		this.email = email;
	}

	/**
	 * Retourne le numéro de téléphone du client.
	 *
	 * @return numéro de téléphone
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * Modifie le numéro de téléphone du client.
	 *
	 * @param phoneNumber numéro de téléphone
	 * @throws NotNullException si le numéro est null ou vide
	 */
	public void setPhoneNumber(String phoneNumber) throws NotNullException {
		Validator.validateNotNull(phoneNumber, "Phone number");
		this.phoneNumber = phoneNumber;
	}

	/**
	 * Retourne la date de création du client.
	 *
	 * @return date de création
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * Retourne une représentation textuelle du client.
	 *
	 * @return chaîne décrivant le client
	 */
	@Override
	public String toString() {
		return "Customer [" + customerNumber + "] " + firstName + " " + lastName + " - " + email + " - " + phoneNumber;
	}
}
