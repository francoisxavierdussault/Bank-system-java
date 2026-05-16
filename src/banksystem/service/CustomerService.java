package banksystem.service;

import banksystem.dao.CustomerDAO;
import banksystem.dao.CustomerDAO.SearchMode;
import banksystem.dto.CustomerDTO;
import banksystem.util.BankLogger;
import java.util.ArrayList;

/**
 * Couche service pour les opérations sur les clients.
 * Valide les données avant de les déléguer au DAO.
 */
public class CustomerService {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "CustomerService";

    CustomerDAO customerDAO = new CustomerDAO();

    public CustomerService() {}

    /**
     * Retourne tous les clients de la base de données.
     *
     * @return liste de tous les clients
     * @implNote O(n) — n = nombre total de clients.
     */
    public ArrayList<CustomerDTO> getAllCustomers() {
        return customerDAO.selectAll();
    }

    /**
     * Recherche un client par son identifiant (clé primaire indexée).
     *
     * @param customerId identifiant du client
     * @return le CustomerDTO correspondant, ou null si non trouvé
     * @implNote O(1) — recherche par clé primaire.
     */
    public CustomerDTO searchCustomer(int customerId) {
        return customerDAO.selectById(customerId);
    }

    /**
     * Recherche les clients par prénom ou nom en mode CONTAINS (correspondance partielle, insensible à la casse).
     * Équivalent à {@code searchByName(keyword, SearchMode.CONTAINS)}.
     *
     * @param keyword mot-clé à rechercher
     * @return liste des clients correspondants, ou tous les clients si le mot-clé est vide
     * @implNote O(n) — balayage complet de la table.
     */
    public ArrayList<CustomerDTO> searchByName(String keyword) {
        return searchByName(keyword, SearchMode.CONTAINS);
    }

    /**
     * Recherche les clients par prénom ou nom selon le mode choisi (insensible à la casse).
     *
     * <ul>
     *   <li>{@link SearchMode#PREFIX}   — noms qui <b>commencent</b> par le mot-clé</li>
     *   <li>{@link SearchMode#SUFFIX}   — noms qui <b>se terminent</b> par le mot-clé</li>
     *   <li>{@link SearchMode#CONTAINS} — noms qui <b>contiennent</b> le mot-clé n'importe où</li>
     * </ul>
     *
     * @param keyword mot-clé à rechercher
     * @param mode    mode de correspondance
     * @return liste des clients correspondants, ou tous les clients si le mot-clé est vide
     * @implNote O(n) — balayage complet de la table.
     */
    public ArrayList<CustomerDTO> searchByName(String keyword, SearchMode mode) {
        if (keyword == null || keyword.trim().isEmpty()) {
            logger.warn(SRC, "searchByName appelé avec un mot-clé vide — retourne tous les clients");
            return customerDAO.selectAll();
        }
        return customerDAO.selectByName(keyword.trim(), mode);
    }

    /**
     * Ajoute un nouveau client après validation.
     *
     * @param dto les données du client à ajouter
     * @return le nombre de lignes insérées (0 si validation échouée)
     * @implNote O(1) — insertion d'une seule ligne.
     */
    public int addCustomer(CustomerDTO dto) {
        if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
            logger.warn(SRC, "addCustomer échoué - le prénom est obligatoire");
            return 0;
        }
        if (dto.getLastName() == null || dto.getLastName().trim().isEmpty()) {
            logger.warn(SRC, "addCustomer échoué - le nom de famille est obligatoire");
            return 0;
        }
        int rows = customerDAO.insert(dto);
        if (rows > 0) {
            logger.info(SRC, "Client ajouté : " + dto.getFirstName() + " " + dto.getLastName()
                    + " (ID=" + dto.getCustomerId() + ")");
        } else {
            logger.error(SRC, "addCustomer - insert DB retourné 0 lignes pour : "
                    + dto.getFirstName() + " " + dto.getLastName());
        }
        return rows;
    }

    /**
     * Met à jour un client existant après validation.
     *
     * @param dto les nouvelles données du client
     * @return le nombre de lignes modifiées (0 si validation échouée)
     * @implNote O(1) — mise à jour par clé primaire.
     */
    public int updateCustomer(CustomerDTO dto) {
        if (dto.getCustomerId() <= 0) {
            logger.warn(SRC, "updateCustomer échoué - ID client invalide : " + dto.getCustomerId());
            return 0;
        }
        int rows = customerDAO.update(dto);
        if (rows > 0) {
            logger.info(SRC, "Client modifié : ID=" + dto.getCustomerId()
                    + " -> " + dto.getFirstName() + " " + dto.getLastName());
        } else {
            logger.error(SRC, "updateCustomer - update DB retourné 0 lignes pour ID=" + dto.getCustomerId());
        }
        return rows;
    }

    /**
     * Supprime un client par son identifiant.
     *
     * @param customerId identifiant du client à supprimer
     * @return le nombre de lignes supprimées (0 si validation échouée)
     * @implNote O(1) — suppression par clé primaire.
     */
    public int removeCustomer(int customerId) {
        if (customerId <= 0) {
            logger.warn(SRC, "removeCustomer échoué - ID client invalide : " + customerId);
            return 0;
        }
        int rows = customerDAO.delete(customerId);
        if (rows > 0) {
            logger.info(SRC, "Client supprimé : ID=" + customerId);
        } else {
            logger.error(SRC, "removeCustomer - delete DB retourné 0 lignes pour ID=" + customerId);
        }
        return rows;
    }
}
