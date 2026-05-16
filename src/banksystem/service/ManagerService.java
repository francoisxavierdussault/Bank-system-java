package banksystem.service;

import banksystem.dao.CustomerDAO.SearchMode;
import banksystem.dao.ManagerDAO;
import banksystem.dto.ManagerDTO;
import banksystem.util.BankLogger;
import java.util.ArrayList;

/**
 * Couche service pour les opérations sur les gestionnaires.
 * Valide les données avant de les envoyer au DAO.
 */
public class ManagerService {

	private static final BankLogger logger = BankLogger.getInstance();
	private static final String SRC = "ManagerService";

	/** DAO utilisé pour accéder à la base de données. */
	ManagerDAO managerDAO = new ManagerDAO();

	/**
	 * Constructeur par défaut.
	 * Initialise le service avec un nouveau ManagerDAO.
	 */
	public ManagerService() {}

	/**
	 * Retourne tous les gestionnaires de la base de données.
	 *
	 * @return liste de tous les gestionnaires
	 * @implNote O(n) — n = nombre total de gestionnaires.
	 */
	public ArrayList<ManagerDTO> getAllManagers() {
		return managerDAO.selectAll();
	}

	/**
	 * Recherche un gestionnaire par son identifiant (clé primaire indexée).
	 *
	 * @param managerId identifiant du gestionnaire
	 * @return le ManagerDTO correspondant, ou null si non trouvé
	 * @implNote O(1) — recherche par clé primaire.
	 */
	public ManagerDTO searchManager(int managerId) {
		return managerDAO.selectById(managerId);
	}

	/**
	 * Recherche les gestionnaires par nom en mode CONTAINS (correspondance partielle, insensible à la casse).
	 * Équivalent à {@code searchByName(keyword, SearchMode.CONTAINS)}.
	 *
	 * @param keyword mot-clé à rechercher
	 * @return liste des gestionnaires correspondants, ou tous les gestionnaires si le mot-clé est vide
	 * @implNote O(n) — balayage complet de la table.
	 */
	public ArrayList<ManagerDTO> searchByName(String keyword) {
		return searchByName(keyword, SearchMode.CONTAINS);
	}

	/**
	 * Recherche les gestionnaires par nom selon le mode choisi (insensible à la casse).
	 *
	 * <ul>
	 *   <li>{@link SearchMode#PREFIX}   — noms qui <b>commencent</b> par le mot-clé</li>
	 *   <li>{@link SearchMode#SUFFIX}   — noms qui <b>se terminent</b> par le mot-clé</li>
	 *   <li>{@link SearchMode#CONTAINS} — noms qui <b>contiennent</b> le mot-clé n'importe où</li>
	 * </ul>
	 *
	 * @param keyword mot-clé à rechercher
	 * @param mode    mode de correspondance
	 * @return liste des gestionnaires correspondants, ou tous les gestionnaires si le mot-clé est vide
	 * @implNote O(n) — balayage complet de la table.
	 */
	public ArrayList<ManagerDTO> searchByName(String keyword, SearchMode mode) {
		if (keyword == null || keyword.trim().isEmpty()) {
			logger.warn(SRC, "searchByName appelé avec un mot-clé vide — retourne tous les gestionnaires");
			return managerDAO.selectAll();
		}
		return managerDAO.selectByName(keyword.trim(), mode);
	}

	/**
	 * Ajoute un nouveau gestionnaire après validation.
	 *
	 * @param dto les données du gestionnaire à ajouter
	 * @return le nombre de lignes insérées (0 si validation échouée)
	 * @implNote O(1) — insertion d'une seule ligne.
	 */
	public int addManager(ManagerDTO dto) {
		if (dto.getManagerName() == null || dto.getManagerName().trim().isEmpty()) {
			logger.warn(SRC, "addManager failed - manager name is required");
			return 0;
		}
		int rows = managerDAO.insert(dto);
		if (rows > 0) {
			logger.info(SRC, "Manager added: " + dto.getManagerName() + " (ID=" + dto.getManagerId() + ")");
		} else {
			logger.error(SRC, "addManager DB insert returned 0 rows for: " + dto.getManagerName());
		}
		return rows;
	}

	/**
	 * Met à jour un gestionnaire existant après validation.
	 *
	 * @param dto les nouvelles données du gestionnaire
	 * @return le nombre de lignes modifiées (0 si validation échouée)
	 * @implNote O(1) — mise à jour par clé primaire.
	 */
	public int updateManager(ManagerDTO dto) {
		if (dto.getManagerId() <= 0) {
			logger.warn(SRC, "updateManager failed - invalid manager ID: " + dto.getManagerId());
			return 0;
		}
		int rows = managerDAO.update(dto);
		if (rows > 0) {
			logger.info(SRC, "Manager updated: ID=" + dto.getManagerId() + " -> " + dto.getManagerName());
		} else {
			logger.error(SRC, "updateManager DB update returned 0 rows for ID=" + dto.getManagerId());
		}
		return rows;
	}

	/**
	 * Supprime un gestionnaire par son identifiant.
	 *
	 * @param managerId identifiant du gestionnaire à supprimer
	 * @return le nombre de lignes supprimées (0 si validation échouée)
	 * @implNote O(1) — suppression par clé primaire.
	 */
	public int removeManager(int managerId) {
		if (managerId <= 0) {
			logger.warn(SRC, "removeManager failed - invalid manager ID: " + managerId);
			return 0;
		}
		int rows = managerDAO.delete(managerId);
		if (rows > 0) {
			logger.info(SRC, "Manager removed: ID=" + managerId);
		} else {
			logger.error(SRC, "removeManager DB delete returned 0 rows for ID=" + managerId);
		}
		return rows;
	}
}
