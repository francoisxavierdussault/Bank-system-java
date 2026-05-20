package banksystem.data;

import banksystem.business.Account;
import banksystem.business.Customer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collection de données en mémoire — magasin central partagé dans l'application.
 *
 * <p>Structures internes :</p>
 * <ul>
 *   <li>{@code customersMap} — {@link LinkedHashMap} indexé par {@code customerNumber}.
 *       Rend {@link #findCustomer} O(1) au lieu de O(n).</li>
 *   <li>{@code accountsMap}  — {@link LinkedHashMap} indexé par {@code accountNumber}.
 *       Rend {@link #findAccount} O(1) au lieu de O(n).</li>
 *   <li>L'ordre d'insertion est préservé pour les itérations (via LinkedHashMap).</li>
 * </ul>
 *
 * <p>Les méthodes {@code getAll*} et {@code getAccountsByCustomer} retournent des
 * {@link ArrayList} pour compatibilité avec le code existant.</p>
 */
public class DataCollection {

    /** Index clients : customerNumber → Customer. O(1) pour findCustomer. */
    private static final LinkedHashMap<Long, Customer> customersMap = new LinkedHashMap<>();

    /** Index comptes : accountNumber → Account. O(1) pour findAccount. */
    private static final LinkedHashMap<Long, Account>  accountsMap  = new LinkedHashMap<>();

    private DataCollection() {}

    // =========================================================================
    //   Clients
    // =========================================================================

    /**
     * Ajoute un client à la collection.
     *
     * @param c le client à ajouter
     * @implNote O(1) — insertion dans LinkedHashMap.
     */
    public static void addCustomer(Customer c) {
        customersMap.put(c.getCustomerNumber(), c);
    }

    /**
     * Retire un client de la collection.
     *
     * @param c le client à retirer
     * @implNote O(1) — suppression par clé dans LinkedHashMap.
     */
    public static void removeCustomer(Customer c) {
        customersMap.remove(c.getCustomerNumber());
    }

    /**
     * Recherche un client par son numéro.
     *
     * @param customerNumber numéro du client
     * @return le client trouvé, ou null si inexistant
     * @implNote O(1) — lookup par clé de hashmap.
     */
    public static Customer findCustomer(long customerNumber) {
        return customersMap.get(customerNumber);
    }

    /**
     * Retourne tous les clients de la collection.
     *
     * @return nouvelle liste snapshot des clients (ordre d'insertion)
     * @implNote O(n) — copie défensive pour isolation.
     */
    public static ArrayList<Customer> getAllCustomers() {
        return new ArrayList<>(customersMap.values());
    }

    /**
     * Retourne le nombre total de clients.
     *
     * @return nombre de clients
     * @implNote O(1) — LinkedHashMap.size() est constant.
     */
    public static int getCustomerCount() {
        return customersMap.size();
    }

    // =========================================================================
    //   Comptes
    // =========================================================================

    /**
     * Ajoute un compte à la collection.
     *
     * @param a le compte à ajouter
     * @implNote O(1) — insertion dans LinkedHashMap.
     */
    public static void addAccount(Account a) {
        accountsMap.put(a.getAccountNumber(), a);
    }

    /**
     * Retire un compte de la collection.
     *
     * @param a le compte à retirer
     * @implNote O(1) — suppression par clé dans LinkedHashMap.
     */
    public static void removeAccount(Account a) {
        accountsMap.remove(a.getAccountNumber());
    }

    /**
     * Recherche un compte par son numéro.
     *
     * @param accountNumber numéro du compte
     * @return le compte trouvé, ou null si inexistant
     * @implNote O(1) — lookup par clé de hashmap (vs O(n) avec ArrayList).
     */
    public static Account findAccount(long accountNumber) {
        return accountsMap.get(accountNumber);
    }

    /**
     * Retourne tous les comptes de la collection.
     *
     * @return nouvelle liste snapshot des comptes (ordre d'insertion)
     * @implNote O(n) — copie défensive pour isolation.
     */
    public static ArrayList<Account> getAllAccounts() {
        return new ArrayList<>(accountsMap.values());
    }

    /**
     * Retourne tous les comptes appartenant à un client donné.
     *
     * @param customerNumber numéro du client
     * @return liste des comptes du client (ordre d'insertion)
     * @implNote O(n) — filtre sur les valeurs de la map.
     */
    public static ArrayList<Account> getAccountsByCustomer(long customerNumber) {
        ArrayList<Account> result = new ArrayList<>();
        for (Account a : accountsMap.values()) {
            if (a.getCustomerNumber() == customerNumber) result.add(a);
        }
        return result;
    }
}
