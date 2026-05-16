package banksystem.dao;

import banksystem.dao.CustomerDAO.SearchMode;
import banksystem.dto.ManagerDTO;
import banksystem.util.BankLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Couche d'accès aux données pour la table Manager — CRUD complet via Oracle XE.
 */
public class ManagerDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "ManagerDAO";

    Connection        myConnection        = null;
    Statement         myStatement         = null;
    PreparedStatement myPreparedStatement = null;
    String            mySqlStatement      = null;
    ResultSet         myResultSet         = null;

    public ManagerDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne tous les gestionnaires de la base de données.
     *
     * @return liste de tous les gestionnaires
     * @implNote O(n) — n = nombre total de gestionnaires dans la table.
     */
    public ArrayList<ManagerDTO> selectAll() {
        ArrayList<ManagerDTO> list = new ArrayList<>();
        mySqlStatement = "SELECT Manager_ID, Manager_Name, Email, Phone FROM Manager ORDER BY Manager_ID";
        try {
            myStatement = myConnection.createStatement();
            myResultSet = myStatement.executeQuery(mySqlStatement);
            while (myResultSet.next()) {
                list.add(new ManagerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2),
                    myResultSet.getString(3), myResultSet.getString(4)
                ));
            }
        } catch (SQLException ex) {
            logger.error(SRC, "selectAll() error: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Recherche un gestionnaire par son identifiant (clé primaire indexée).
     *
     * @param managerId identifiant du gestionnaire
     * @return le ManagerDTO correspondant, ou null si non trouvé
     * @implNote O(1) — recherche par clé primaire.
     */
    public ManagerDTO selectById(int managerId) {
        ManagerDTO dto = null;
        mySqlStatement = "SELECT Manager_ID, Manager_Name, Email, Phone FROM Manager WHERE Manager_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, managerId);
            myResultSet = myPreparedStatement.executeQuery();
            if (myResultSet.next()) {
                dto = new ManagerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2),
                    myResultSet.getString(3), myResultSet.getString(4)
                );
            }
        } catch (SQLException ex) {
            logger.error(SRC, "selectById(" + managerId + ") error: " + ex.getMessage());
        }
        return dto;
    }

    /**
     * Recherche les gestionnaires dont le nom correspond au mot-clé en mode CONTAINS.
     * Équivalent à {@code selectByName(keyword, SearchMode.CONTAINS)}.
     *
     * @param keyword mot-clé à rechercher (insensible à la casse)
     * @return liste des gestionnaires correspondants
     * @implNote O(n) — balayage complet de la table (LIKE sans index sur Manager_Name).
     */
    public ArrayList<ManagerDTO> selectByName(String keyword) {
        return selectByName(keyword, SearchMode.CONTAINS);
    }

    /**
     * Recherche les gestionnaires dont le nom correspond au mot-clé selon le mode choisi.
     *
     * <ul>
     *   <li>{@link SearchMode#PREFIX}   — noms qui <b>commencent</b> par le mot-clé</li>
     *   <li>{@link SearchMode#SUFFIX}   — noms qui <b>se terminent</b> par le mot-clé</li>
     *   <li>{@link SearchMode#CONTAINS} — noms qui <b>contiennent</b> le mot-clé n'importe où</li>
     * </ul>
     *
     * @param keyword mot-clé à rechercher (insensible à la casse)
     * @param mode    mode de correspondance
     * @return liste des gestionnaires correspondants
     * @implNote O(n) — balayage complet de la table.
     */
    public ArrayList<ManagerDTO> selectByName(String keyword, SearchMode mode) {
        ArrayList<ManagerDTO> list = new ArrayList<>();
        mySqlStatement = "SELECT Manager_ID, Manager_Name, Email, Phone "
                       + "FROM Manager WHERE UPPER(Manager_Name) LIKE ? ORDER BY Manager_Name";
        try {
            String kw = keyword.toUpperCase();
            String pattern = switch (mode) {
                case PREFIX   -> kw + "%";
                case SUFFIX   -> "%" + kw;
                case CONTAINS -> "%" + kw + "%";
            };
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setString(1, pattern);
            myResultSet = myPreparedStatement.executeQuery();
            while (myResultSet.next()) {
                list.add(new ManagerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2),
                    myResultSet.getString(3), myResultSet.getString(4)
                ));
            }
            logger.info(SRC, "selectByName('" + keyword + "', " + mode + ") → " + list.size() + " résultat(s)");
        } catch (SQLException ex) {
            logger.error(SRC, "selectByName('" + keyword + "') error: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Insère un nouveau gestionnaire dans la base de données.
     *
     * @param dto les données du gestionnaire à insérer
     * @return nombre de lignes insérées (1 si succès, 0 si échec)
     * @implNote O(1) — insertion d'une seule ligne.
     */
    public int insert(ManagerDTO dto) {
        int rows = 0;
        mySqlStatement = "INSERT INTO Manager (Manager_ID, Manager_Name, Email, Phone) VALUES(?, ?, ?, ?)";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, dto.getManagerId());
            myPreparedStatement.setString(2, dto.getManagerName());
            myPreparedStatement.setString(3, dto.getEmail());
            myPreparedStatement.setString(4, dto.getPhone());
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "insert() success — Manager ID=" + dto.getManagerId());
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() error for ID=" + dto.getManagerId() + ": " + ex.getMessage());
        }
        return rows;
    }

    /**
     * Met à jour un gestionnaire existant dans la base de données.
     *
     * @param dto les nouvelles données du gestionnaire
     * @return nombre de lignes modifiées (1 si succès, 0 si échec)
     * @implNote O(1) — mise à jour par clé primaire.
     */
    public int update(ManagerDTO dto) {
        int rows = 0;
        mySqlStatement = "UPDATE Manager SET Manager_Name = ?, Email = ?, Phone = ? WHERE Manager_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setString(1, dto.getManagerName());
            myPreparedStatement.setString(2, dto.getEmail());
            myPreparedStatement.setString(3, dto.getPhone());
            myPreparedStatement.setInt(4, dto.getManagerId());
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "update() success — Manager ID=" + dto.getManagerId());
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "update() error for ID=" + dto.getManagerId() + ": " + ex.getMessage());
        }
        return rows;
    }

    /**
     * Supprime un gestionnaire par son identifiant.
     *
     * @param managerId identifiant du gestionnaire à supprimer
     * @return nombre de lignes supprimées (1 si succès, 0 si échec)
     * @implNote O(1) — suppression par clé primaire.
     */
    public int delete(int managerId) {
        int rows = 0;
        mySqlStatement = "DELETE FROM Manager WHERE Manager_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, managerId);
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "delete() success — Manager ID=" + managerId);
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "delete() error for ID=" + managerId + ": " + ex.getMessage());
        }
        return rows;
    }
}
