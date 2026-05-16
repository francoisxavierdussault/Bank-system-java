package banksystem.service;

import banksystem.dao.CustomerDAO;
import banksystem.dao.LoginDAO;
import banksystem.dao.LoginDAO.AuthRow;
import banksystem.dao.ManagerDAO;
import banksystem.dto.CustomerDTO;
import banksystem.dto.ManagerDTO;
import banksystem.util.BankLogger;
import banksystem.util.PasswordUtil;

/**
 * Gère l'authentification des gestionnaires et des clients.
 *
 * Règles :
 *  - Maximum 3 tentatives échouées avant le verrouillage du compte dans Oracle.
 *  - Si Pwd_Hash est NULL (première connexion), l'ID numérique de l'utilisateur
 *    est accepté comme mot de passe temporaire et haché immédiatement.
 *  - En cas de succès : le compteur d'échecs est réinitialisé en base.
 */
public class LoginService {

    public static final int MAX_ATTEMPTS = 3;

    private static final BankLogger  logger      = BankLogger.getInstance();
    private static final String      SRC         = "LoginService";

    private final LoginDAO    loginDAO    = new LoginDAO();
    private final ManagerDAO  managerDAO  = new ManagerDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    // -------------------------------------------------------------------------
    // Objet résultat
    // -------------------------------------------------------------------------

    public enum Role { MANAGER, CUSTOMER }

    public static class LoginResult {
        public final boolean     success;
        public final String      message;
        public final Role        role;
        public final ManagerDTO  manager;
        public final CustomerDTO customer;
        public final int         attemptsLeft;

        private LoginResult(boolean success, String message, Role role,
                            ManagerDTO manager, CustomerDTO customer, int attemptsLeft) {
            this.success      = success;
            this.message      = message;
            this.role         = role;
            this.manager      = manager;
            this.customer     = customer;
            this.attemptsLeft = attemptsLeft;
        }

        public static LoginResult ok(Role role, ManagerDTO m, CustomerDTO c) {
            return new LoginResult(true, "Connexion réussie", role, m, c, MAX_ATTEMPTS);
        }
        public static LoginResult fail(String msg, int attemptsLeft) {
            return new LoginResult(false, msg, null, null, null, attemptsLeft);
        }
    }

    // -------------------------------------------------------------------------
    // Connexion gestionnaire
    // -------------------------------------------------------------------------

    /**
     * Authentifie un gestionnaire par identifiant et mot de passe.
     * Gère le verrouillage après {@link #MAX_ATTEMPTS} tentatives échouées.
     * Lors de la première connexion (hash null), l'identifiant numérique sert de mot de passe temporaire.
     *
     * @param managerId     identifiant numérique du gestionnaire
     * @param plainPassword mot de passe en clair saisi
     * @return {@link LoginResult} indiquant le succès ou l'échec
     * @implNote O(1) — deux requêtes SQL par clé primaire + un hachage SHA-256.
     */
    public LoginResult loginManager(int managerId, String plainPassword) {
        logger.info(SRC, "Tentative de connexion gestionnaire ID=" + managerId);

        AuthRow auth = loginDAO.getManagerAuth(managerId);
        if (auth == null) {
            logger.warn(SRC, "Connexion gestionnaire échouée — ID=" + managerId + " introuvable");
            return LoginResult.fail("Identifiant gestionnaire introuvable.", MAX_ATTEMPTS);
        }

        if (auth.isLocked) {
            logger.warn(SRC, "Connexion gestionnaire bloquée — compte verrouillé (ID=" + managerId + ")");
            return LoginResult.fail("Compte verrouillé après trop d'échecs.\nContactez votre administrateur.", 0);
        }

        boolean verified = verifyPassword(plainPassword, String.valueOf(managerId), auth.pwdHash);
        if (!verified) {
            loginDAO.incrementManagerFail(managerId, MAX_ATTEMPTS);
            int left = MAX_ATTEMPTS - (auth.failedLogins + 1);
            logger.warn(SRC, "Mot de passe incorrect (ID=" + managerId + ") tentatives restantes=" + left);
            if (left <= 0) {
                return LoginResult.fail("Compte verrouillé après " + MAX_ATTEMPTS + " tentatives échouées.", 0);
            }
            return LoginResult.fail("Mot de passe incorrect. " + left + " tentative(s) restante(s).", left);
        }

        // Succès — hacher et stocker si première connexion (hash null)
        String hashToStore = (auth.pwdHash == null)
            ? PasswordUtil.hashForStorage(plainPassword)
            : auth.pwdHash;
        loginDAO.onManagerLoginSuccess(managerId, hashToStore);

        ManagerDTO dto = managerDAO.selectById(managerId);
        logger.info(SRC, "Connexion gestionnaire réussie — ID=" + managerId + " (" + (dto != null ? dto.getManagerName() : "") + ")");
        return LoginResult.ok(Role.MANAGER, dto, null);
    }

    // -------------------------------------------------------------------------
    // Connexion client
    // -------------------------------------------------------------------------

    /**
     * Authentifie un client par identifiant et mot de passe.
     * Gère le verrouillage après {@link #MAX_ATTEMPTS} tentatives échouées.
     * Lors de la première connexion (hash null), l'identifiant numérique sert de mot de passe temporaire.
     *
     * @param customerId    identifiant numérique du client
     * @param plainPassword mot de passe en clair saisi
     * @return {@link LoginResult} indiquant le succès ou l'échec
     * @implNote O(1) — deux requêtes SQL par clé primaire + un hachage SHA-256.
     */
    public LoginResult loginCustomer(int customerId, String plainPassword) {
        logger.info(SRC, "Tentative de connexion client ID=" + customerId);

        AuthRow auth = loginDAO.getCustomerAuth(customerId);
        if (auth == null) {
            logger.warn(SRC, "Connexion client échouée — ID=" + customerId + " introuvable");
            return LoginResult.fail("Identifiant client introuvable.", MAX_ATTEMPTS);
        }

        if (auth.isLocked) {
            logger.warn(SRC, "Connexion client bloquée — compte verrouillé (ID=" + customerId + ")");
            return LoginResult.fail("Compte verrouillé après trop d'échecs.\nContactez votre gestionnaire.", 0);
        }

        boolean verified = verifyPassword(plainPassword, String.valueOf(customerId), auth.pwdHash);
        if (!verified) {
            loginDAO.incrementCustomerFail(customerId, MAX_ATTEMPTS);
            int left = MAX_ATTEMPTS - (auth.failedLogins + 1);
            logger.warn(SRC, "Mot de passe incorrect (ID=" + customerId + ") tentatives restantes=" + left);
            if (left <= 0) {
                return LoginResult.fail("Compte verrouillé après " + MAX_ATTEMPTS + " tentatives échouées.", 0);
            }
            return LoginResult.fail("Mot de passe incorrect. " + left + " tentative(s) restante(s).", left);
        }

        String hashToStore = (auth.pwdHash == null)
            ? PasswordUtil.hashForStorage(plainPassword)
            : auth.pwdHash;
        loginDAO.onCustomerLoginSuccess(customerId, hashToStore);

        CustomerDTO dto = customerDAO.selectById(customerId);
        logger.info(SRC, "Connexion client réussie — ID=" + customerId
                + " (" + (dto != null ? dto.getFirstName() + " " + dto.getLastName() : "") + ")");
        return LoginResult.ok(Role.CUSTOMER, null, dto);
    }

    // -------------------------------------------------------------------------
    // Méthode privée
    // -------------------------------------------------------------------------

    /**
     * Vérifie un mot de passe.
     * Si storedHash est NULL (première connexion), accepte l'ID de l'utilisateur comme mot de passe.
     */
    private boolean verifyPassword(String plainPassword, String idAsDefaultPwd, String storedHash) {
        if (storedHash == null) {
            // Première connexion — le mot de passe par défaut est l'identifiant de l'utilisateur
            return plainPassword.equals(idAsDefaultPwd);
        }
        return PasswordUtil.verifyStored(plainPassword, storedHash);
    }
}
