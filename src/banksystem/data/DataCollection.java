package banksystem.data;

import banksystem.business.Account;
import banksystem.business.Customer;
import java.util.ArrayList;

/**
 * Collection de données en mémoire.
 * Contient toutes les listes de clients et de comptes partagées dans l'application.
 */
public class DataCollection {

	/** Liste de tous les clients en mémoire. */
	private static ArrayList<Customer> customers = new ArrayList<>();

	/** Liste de tous les comptes en mémoire. */
	private static ArrayList<Account> accounts = new ArrayList<>();

	/**
	 * Constructeur privé — classe utilitaire, ne doit pas être instanciée.
	 */
	private DataCollection() {}

	// --- Opérations sur les clients ---

	/**
	 * Ajoute un client à la collection.
	 *
	 * @param c le client à ajouter
	 * @implNote O(1) — ajout en fin de liste.
	 */
	public static void addCustomer(Customer c) {
		customers.add(c);
	}

	/**
	 * Retire un client de la collection.
	 *
	 * @param c le client à retirer
	 * @implNote O(n) — ArrayList.remove() effectue une recherche linéaire par égalité d'objet.
	 */
	public static void removeCustomer(Customer c) {
		customers.remove(c);
	}

	/**
	 * Recherche un client par son numéro.
	 *
	 * @param customerNumber numéro du client
	 * @return le client trouvé, ou null si inexistant
	 * @implNote O(n) — parcours séquentiel de la liste en mémoire.
	 */
	public static Customer findCustomer(long customerNumber) {
		for (Customer c : customers) {
			if (c.getCustomerNumber() == customerNumber) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Retourne tous les clients de la collection.
	 *
	 * @return liste de tous les clients
	 * @implNote O(1) — retourne la référence directe à la liste interne.
	 */
	public static ArrayList<Customer> getAllCustomers() {
		return customers;
	}

	/**
	 * Retourne le nombre total de clients.
	 *
	 * @return nombre de clients
	 * @implNote O(1) — ArrayList.size() est maintenu en temps constant.
	 */
	public static int getCustomerCount() {
		return customers.size();
	}

	// --- Opérations sur les comptes ---

	/**
	 * Ajoute un compte à la collection.
	 *
	 * @param a le compte à ajouter
	 * @implNote O(1) — ajout en fin de liste.
	 */
	public static void addAccount(Account a) {
		accounts.add(a);
	}

	/**
	 * Retire un compte de la collection.
	 *
	 * @param a le compte à retirer
	 * @implNote O(n) — ArrayList.remove() effectue une recherche linéaire par égalité d'objet.
	 */
	public static void removeAccount(Account a) {
		accounts.remove(a);
	}

	/**
	 * Recherche un compte par son numéro.
	 *
	 * @param accountNumber numéro du compte
	 * @return le compte trouvé, ou null si inexistant
	 * @implNote O(n) — parcours séquentiel de la liste en mémoire.
	 */
	public static Account findAccount(long accountNumber) {
		for (Account a : accounts) {
			if (a.getAccountNumber() == accountNumber) {
				return a;
			}
		}
		return null;
	}

	/**
	 * Retourne tous les comptes de la collection.
	 *
	 * @return liste de tous les comptes
	 * @implNote O(1) — retourne la référence directe à la liste interne.
	 */
	public static ArrayList<Account> getAllAccounts() {
		return accounts;
	}

	/**
	 * Retourne tous les comptes appartenant à un client donné.
	 *
	 * @param customerNumber numéro du client
	 * @return liste des comptes du client
	 * @implNote O(n) — parcours de tous les comptes en mémoire pour filtrer par client.
	 */
	public static ArrayList<Account> getAccountsByCustomer(long customerNumber) {
		ArrayList<Account> result = new ArrayList<>();
		for (Account a : accounts) {
			if (a.getCustomerNumber() == customerNumber) {
				result.add(a);
			}
		}
		return result;
	}
}
