package banksystem.dto;

/**
 * Objet de transfert de données pour la table Client.
 * Transporte les données d'un client entre les couches de l'application.
 */
public class CustomerDTO {

	/** Identifiant unique du client. */
	private int customerId;

	/** Prénom du client. */
	private String firstName;

	/** Nom de famille du client. */
	private String lastName;

	/** Adresse courriel du client. */
	private String email;

	/** Numéro de téléphone du client. */
	private String phone;

	/** Identifiant du gestionnaire assigné à ce client (clé étrangère). */
	private int managerId;

	/** Constructeur vide. */
	public CustomerDTO() {}

	/**
	 * Constructeur avec tous les champs.
	 *
	 * @param customerId identifiant du client
	 * @param firstName  prénom
	 * @param lastName   nom de famille
	 * @param email      adresse courriel
	 * @param phone      numéro de téléphone
	 * @param managerId  identifiant du gestionnaire
	 */
	public CustomerDTO(int customerId, String firstName, String lastName,
	                   String email, String phone, int managerId) {
		this.customerId = customerId;
		this.firstName  = firstName;
		this.lastName   = lastName;
		this.email      = email;
		this.phone      = phone;
		this.managerId  = managerId;
	}

	/**
	 * Retourne l'identifiant du client.
	 *
	 * @return identifiant du client
	 */
	public int getCustomerId() { return customerId; }

	/**
	 * Modifie l'identifiant du client.
	 *
	 * @param customerId identifiant du client
	 */
	public void setCustomerId(int customerId) { this.customerId = customerId; }

	/**
	 * Retourne le prénom du client.
	 *
	 * @return prénom du client
	 */
	public String getFirstName() { return firstName; }

	/**
	 * Modifie le prénom du client.
	 *
	 * @param firstName prénom du client
	 */
	public void setFirstName(String firstName) { this.firstName = firstName; }

	/**
	 * Retourne le nom de famille du client.
	 *
	 * @return nom de famille du client
	 */
	public String getLastName() { return lastName; }

	/**
	 * Modifie le nom de famille du client.
	 *
	 * @param lastName nom de famille du client
	 */
	public void setLastName(String lastName) { this.lastName = lastName; }

	/**
	 * Retourne l'adresse courriel du client.
	 *
	 * @return adresse courriel du client
	 */
	public String getEmail() { return email; }

	/**
	 * Modifie l'adresse courriel du client.
	 *
	 * @param email adresse courriel du client
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 * Retourne le numéro de téléphone du client.
	 *
	 * @return numéro de téléphone du client
	 */
	public String getPhone() { return phone; }

	/**
	 * Modifie le numéro de téléphone du client.
	 *
	 * @param phone numéro de téléphone du client
	 */
	public void setPhone(String phone) { this.phone = phone; }

	/**
	 * Retourne l'identifiant du gestionnaire assigné.
	 *
	 * @return identifiant du gestionnaire
	 */
	public int getManagerId() { return managerId; }

	/**
	 * Modifie l'identifiant du gestionnaire assigné.
	 *
	 * @param managerId identifiant du gestionnaire
	 */
	public void setManagerId(int managerId) { this.managerId = managerId; }

	/**
	 * Retourne une représentation textuelle du client.
	 *
	 * @return chaîne décrivant le client
	 */
	@Override
	public String toString() {
		return "Client [" + customerId + "] " + firstName + " " + lastName
				+ " - " + email + " - " + phone + " | Gestionnaire: " + managerId;
	}
}
