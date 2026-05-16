package banksystem.dao;

import banksystem.dto.CustomerDTO;
import banksystem.util.BankLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Couche d'accès aux données pour la table Customer — CRUD complet via Oracle XE.
 */
public class CustomerDAO {

    /**
     * Mode de recherche par nom : préfixe, suffixe ou contenu.
     */
    public enum SearchMode {
        /** Cherche les noms qui commencent par le mot-clé (ex. "Mar" → "Marie", "Marc"). */
        PREFIX,
        /** Cherche les noms qui se terminent par le mot-clé (ex. "eau" → "Moreau"). */
        SUFFIX,
        /** Cherche les noms qui contiennent le mot-clé n'importe où (comportement par défaut). */
        CONTAINS
    }

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "CustomerDAO";

    Connection        myConnection        = null;
    Statement         myStatement         = null;
    PreparedStatement myPreparedStatement = null;
    String            mySqlStatement      = null;
    ResultSet         myResultSet         = null;

    public CustomerDAO() {
        myConnection = DBConnection.getConnection();
    }

    /**
     * Retourne tous les clients de la base de données.
     *
     * @return liste de tous les clients
     * @implNote O(n) — n = nombre total de clients dans la table.
     */
    public ArrayList<CustomerDTO> selectAll() {
        ArrayList<CustomerDTO> list = new ArrayList<>();
        mySqlStatement = "SELECT c.Customer_ID, c.First_Name, c.Last_Name, c.Email, c.Phone, c.Manager_ID "
                       + "FROM Customer c ORDER BY c.Customer_ID";
        try {
            myStatement = myConnection.createStatement();
            myResultSet = myStatement.executeQuery(mySqlStatement);
            while (myResultSet.next()) {
                list.add(new CustomerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2), myResultSet.getString(3),
                    myResultSet.getString(4), myResultSet.getString(5), myResultSet.getInt(6)
                ));
            }
        } catch (SQLException ex) {
            logger.error(SRC, "selectAll() error: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Recherche un client par son identifiant (clé primaire indexée).
     *
     * @param customerId identifiant du client
     * @return le CustomerDTO correspondant, ou null si non trouvé
     * @implNote O(1) — recherche par clé primaire indexée.
     */
    public CustomerDTO selectById(int customerId) {
        CustomerDTO dto = null;
        mySqlStatement = "SELECT Customer_ID, First_Name, Last_Name, Email, Phone, Manager_ID "
                       + "FROM Customer WHERE Customer_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, customerId);
            myResultSet = myPreparedStatement.executeQuery();
            if (myResultSet.next()) {
                dto = new CustomerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2), myResultSet.getString(3),
                    myResultSet.getString(4), myResultSet.getString(5), myResultSet.getInt(6)
                );
            }
        } catch (SQLException ex) {
            logger.error(SRC, "selectById(" + customerId + ") error: " + ex.getMessage());
        }
        return dto;
    }

    /**
     * Recherche les clients dont le prénom ou le nom contient le mot-clé (insensible à la casse).
     * Équivalent à {@code selectByName(keyword, SearchMode.CONTAINS)}.
     *
     * @param keyword mot-clé à rechercher
     * @return liste des clients correspondants
     * @implNote O(n) — balayage complet de la table sans index sur la colonne nom.
     */
    public ArrayList<CustomerDTO> selectByName(String keyword) {
        return selectByName(keyword, SearchMode.CONTAINS);
    }

    /**
     * Recherche les clients dont le prénom ou le nom correspond au mot-clé selon le mode choisi.
     *
     * <ul>
     *   <li>{@link SearchMode#PREFIX}   — noms qui <b>commencent</b> par le mot-clé (ex. "Mar" → "Marie")</li>
     *   <li>{@link SearchMode#SUFFIX}   — noms qui <b>se terminent</b> par le mot-clé (ex. "eau" → "Moreau")</li>
     *   <li>{@link SearchMode#CONTAINS} — noms qui <b>contiennent</b> le mot-clé n'importe où (défaut)</li>
     * </ul>
     *
     * @param keyword mot-clé à rechercher
     * @param mode    mode de correspondance
     * @return liste des clients correspondants
     * @implNote O(n) — balayage complet de la table (LIKE ne bénéficie d'un index que pour PREFIX sans wildcard initial).
     */
    public ArrayList<CustomerDTO> selectByName(String keyword, SearchMode mode) {
        ArrayList<CustomerDTO> list = new ArrayList<>();
        mySqlStatement = "SELECT Customer_ID, First_Name, Last_Name, Email, Phone, Manager_ID "
                       + "FROM Customer WHERE UPPER(First_Name) LIKE ? OR UPPER(Last_Name) LIKE ? "
                       + "ORDER BY Last_Name, First_Name";
        try {
            String kw = keyword.toUpperCase();
            String pattern = switch (mode) {
                case PREFIX   -> kw + "%";
                case SUFFIX   -> "%" + kw;
                case CONTAINS -> "%" + kw + "%";
            };
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setString(1, pattern);
            myPreparedStatement.setString(2, pattern);
            myResultSet = myPreparedStatement.executeQuery();
            while (myResultSet.next()) {
                list.add(new CustomerDTO(
                    myResultSet.getInt(1), myResultSet.getString(2), myResultSet.getString(3),
                    myResultSet.getString(4), myResultSet.getString(5), myResultSet.getInt(6)
                ));
            }
            logger.info(SRC, "selectByName('" + keyword + "', " + mode + ") returned " + list.size() + " result(s)");
        } catch (SQLException ex) {
            logger.error(SRC, "selectByName('" + keyword + "') error: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Insère un nouveau client dans la base de données.
     *
     * @param dto les données du client à insérer
     * @return nombre de lignes insérées (1 si succès, 0 si échec)
     * @implNote O(1) — insertion d'une seule ligne.
     */
    public int insert(CustomerDTO dto) {
        int rows = 0;
        mySqlStatement = "INSERT INTO Customer (Customer_ID, First_Name, Last_Name, Email, Phone, Manager_ID) VALUES(?, ?, ?, ?, ?, ?)";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, dto.getCustomerId());
            myPreparedStatement.setString(2, dto.getFirstName());
            myPreparedStatement.setString(3, dto.getLastName());
            myPreparedStatement.setString(4, dto.getEmail());
            myPreparedStatement.setString(5, dto.getPhone());
            myPreparedStatement.setInt(6, dto.getManagerId());
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "insert() success — Customer ID=" + dto.getCustomerId());
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "insert() error for ID=" + dto.getCustomerId() + ": " + ex.getMessage());
        }
        return rows;
    }

    /**
     * Met à jour un client existant dans la base de données.
     *
     * @param dto les nouvelles données du client
     * @return nombre de lignes modifiées (1 si succès, 0 si échec)
     * @implNote O(1) — mise à jour par clé primaire indexée.
     */
    public int update(CustomerDTO dto) {
        int rows = 0;
        mySqlStatement = "UPDATE Customer SET First_Name = ?, Last_Name = ?, Email = ?, Phone = ?, Manager_ID = ? "
                       + "WHERE Customer_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setString(1, dto.getFirstName());
            myPreparedStatement.setString(2, dto.getLastName());
            myPreparedStatement.setString(3, dto.getEmail());
            myPreparedStatement.setString(4, dto.getPhone());
            myPreparedStatement.setInt(5, dto.getManagerId());
            myPreparedStatement.setInt(6, dto.getCustomerId());
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "update() success — Customer ID=" + dto.getCustomerId());
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "update() error for ID=" + dto.getCustomerId() + ": " + ex.getMessage());
        }
        return rows;
    }

    /**
     * Supprime un client par son identifiant.
     *
     * @param customerId identifiant du client à supprimer
     * @return nombre de lignes supprimées (1 si succès, 0 si échec)
     * @implNote O(1) — suppression par clé primaire indexée.
     */
    public int delete(int customerId) {
        int rows = 0;
        mySqlStatement = "DELETE FROM Customer WHERE Customer_ID = ?";
        try {
            myPreparedStatement = myConnection.prepareStatement(mySqlStatement);
            myPreparedStatement.setInt(1, customerId);
            rows = myPreparedStatement.executeUpdate();
            myConnection.commit();
            logger.info(SRC, "delete() success — Customer ID=" + customerId);
        } catch (SQLException ex) {
            try { myConnection.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "delete() error for ID=" + customerId + ": " + ex.getMessage());
        }
        return rows;
    }
}
