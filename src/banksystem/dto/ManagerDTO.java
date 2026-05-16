package banksystem.dto;

/**
 * Objet de transfert de données pour la table Gestionnaire.
 * Transporte les données d'un gestionnaire entre les couches de l'application.
 */
public class ManagerDTO {

	/** Identifiant unique du gestionnaire. */
	private int managerId;

	/** Nom complet du gestionnaire. */
	private String managerName;

	/** Adresse courriel du gestionnaire. */
	private String email;

	/** Numéro de téléphone du gestionnaire. */
	private String phone;

	/** Constructeur vide. */
	public ManagerDTO() {}

	/**
	 * Constructeur avec tous les champs.
	 *
	 * @param managerId   identifiant du gestionnaire
	 * @param managerName nom complet
	 * @param email       adresse courriel
	 * @param phone       numéro de téléphone
	 */
	public ManagerDTO(int managerId, String managerName, String email, String phone) {
		this.managerId   = managerId;
		this.managerName = managerName;
		this.email       = email;
		this.phone       = phone;
	}

	/**
	 * Retourne l'identifiant du gestionnaire.
	 *
	 * @return identifiant du gestionnaire
	 */
	public int getManagerId() { return managerId; }

	/**
	 * Modifie l'identifiant du gestionnaire.
	 *
	 * @param managerId identifiant du gestionnaire
	 */
	public void setManagerId(int managerId) { this.managerId = managerId; }

	/**
	 * Retourne le nom complet du gestionnaire.
	 *
	 * @return nom complet du gestionnaire
	 */
	public String getManagerName() { return managerName; }

	/**
	 * Modifie le nom complet du gestionnaire.
	 *
	 * @param managerName nom complet du gestionnaire
	 */
	public void setManagerName(String managerName) { this.managerName = managerName; }

	/**
	 * Retourne l'adresse courriel du gestionnaire.
	 *
	 * @return adresse courriel du gestionnaire
	 */
	public String getEmail() { return email; }

	/**
	 * Modifie l'adresse courriel du gestionnaire.
	 *
	 * @param email adresse courriel du gestionnaire
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 * Retourne le numéro de téléphone du gestionnaire.
	 *
	 * @return numéro de téléphone du gestionnaire
	 */
	public String getPhone() { return phone; }

	/**
	 * Modifie le numéro de téléphone du gestionnaire.
	 *
	 * @param phone numéro de téléphone du gestionnaire
	 */
	public void setPhone(String phone) { this.phone = phone; }

	/**
	 * Retourne une représentation textuelle du gestionnaire.
	 *
	 * @return chaîne décrivant le gestionnaire
	 */
	@Override
	public String toString() {
		return "Gestionnaire [" + managerId + "] " + managerName + " - " + email + " - " + phone;
	}
}
