package banksystem.dao;

import banksystem.util.BankLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO d'authentification — lit et écrit les colonnes Pwd_Hash, Failed_Login, Is_Locked
 * sur les tables Manager et Customer.
 */
public class LoginDAO {

    private static final BankLogger logger = BankLogger.getInstance();
    private static final String     SRC    = "LoginDAO";

    private final Connection conn;

    public LoginDAO() {
        this.conn = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // Conteneur de données d'authentification
    // -------------------------------------------------------------------------

    /** Données brutes d'authentification lues depuis la base. */
    public static class AuthRow {
        public final String  pwdHash;
        public final int     failedLogins;
        public final boolean isLocked;

        public AuthRow(String pwdHash, int failedLogins, boolean isLocked) {
            this.pwdHash      = pwdHash;
            this.failedLogins = failedLogins;
            this.isLocked     = isLocked;
        }
    }

    // -------------------------------------------------------------------------
    // Authentification gestionnaire
    // -------------------------------------------------------------------------

    /**
     * Retourne les données d'auth du gestionnaire donné, ou null s'il n'existe pas.
     *
     * @param managerId identifiant du gestionnaire
     * @return AuthRow contenant le hachage, le compteur d'échecs et le statut verrouillé, ou null
     * @implNote O(1) — lecture d'une seule ligne par clé primaire indexée.
     */
    public AuthRow getManagerAuth(int managerId) {
        String sql = "SELECT Pwd_Hash, Failed_Login, Is_Locked FROM Manager WHERE Manager_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new AuthRow(rs.getString(1), rs.getInt(2), rs.getInt(3) == 1);
            }
        } catch (SQLException ex) {
            logger.error(SRC, "getManagerAuth(" + managerId + ") erreur : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Incrémente le compteur d'échecs du gestionnaire ; verrouille si &gt;= maxAttempts.
     *
     * @param managerId   identifiant du gestionnaire
     * @param maxAttempts nombre maximal de tentatives avant verrouillage
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public void incrementManagerFail(int managerId, int maxAttempts) {
        String sql = "UPDATE Manager SET Failed_Login = Failed_Login + 1, "
                   + "Is_Locked = CASE WHEN Failed_Login + 1 >= ? THEN 1 ELSE 0 END "
                   + "WHERE Manager_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxAttempts);
            ps.setInt(2, managerId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "incrementManagerFail(" + managerId + ") erreur : " + ex.getMessage());
        }
    }

    /**
     * Réinitialise le compteur d'échecs et stocke le hachage du mot de passe après connexion réussie.
     *
     * @param managerId identifiant du gestionnaire
     * @param pwdHash   hachage {@code "sel:hachage"} à stocker
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public void onManagerLoginSuccess(int managerId, String pwdHash) {
        String sql = "UPDATE Manager SET Failed_Login = 0, Is_Locked = 0, Pwd_Hash = ? WHERE Manager_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pwdHash);
            ps.setInt(2, managerId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "onManagerLoginSuccess(" + managerId + ") erreur : " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Authentification client
    // -------------------------------------------------------------------------

    /**
     * Retourne les données d'auth du client donné, ou null s'il n'existe pas.
     *
     * @param customerId identifiant du client
     * @return AuthRow contenant le hachage, le compteur d'échecs et le statut verrouillé, ou null
     * @implNote O(1) — lecture d'une seule ligne par clé primaire indexée.
     */
    public AuthRow getCustomerAuth(int customerId) {
        String sql = "SELECT Pwd_Hash, Failed_Login, Is_Locked FROM Customer WHERE Customer_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new AuthRow(rs.getString(1), rs.getInt(2), rs.getInt(3) == 1);
            }
        } catch (SQLException ex) {
            logger.error(SRC, "getCustomerAuth(" + customerId + ") erreur : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Incrémente le compteur d'échecs du client ; verrouille si &gt;= maxAttempts.
     *
     * @param customerId  identifiant du client
     * @param maxAttempts nombre maximal de tentatives avant verrouillage
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public void incrementCustomerFail(int customerId, int maxAttempts) {
        String sql = "UPDATE Customer SET Failed_Login = Failed_Login + 1, "
                   + "Is_Locked = CASE WHEN Failed_Login + 1 >= ? THEN 1 ELSE 0 END "
                   + "WHERE Customer_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxAttempts);
            ps.setInt(2, customerId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "incrementCustomerFail(" + customerId + ") erreur : " + ex.getMessage());
        }
    }

    /**
     * Réinitialise le compteur d'échecs et stocke le hachage après connexion réussie.
     *
     * @param customerId identifiant du client
     * @param pwdHash    hachage {@code "sel:hachage"} à stocker
     * @implNote O(1) — mise à jour d'une seule ligne par clé primaire.
     */
    public void onCustomerLoginSuccess(int customerId, String pwdHash) {
        String sql = "UPDATE Customer SET Failed_Login = 0, Is_Locked = 0, Pwd_Hash = ? WHERE Customer_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pwdHash);
            ps.setInt(2, customerId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            logger.error(SRC, "onCustomerLoginSuccess(" + customerId + ") erreur : " + ex.getMessage());
        }
    }
}
