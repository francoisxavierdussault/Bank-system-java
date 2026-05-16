package banksystem.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitaire de hachage et vérification de mots de passe (SHA-256 + sel).
 * Utilise uniquement les API standard Java SE.
 */
public class PasswordUtil {

    private static final String ALGORITHM  = "SHA-256";
    private static final int    SALT_BYTES = 16;

    private PasswordUtil() {}

    /**
     * Génère un sel aléatoire de 16 octets encodé en Base64.
     *
     * @return sel encodé en Base64
     * @implNote O(1) — génération d'un bloc de taille fixe (16 octets).
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hache un mot de passe en clair combiné avec le sel fourni (SHA-256).
     *
     * @param plainPassword mot de passe en clair
     * @param salt          sel encodé en Base64
     * @return hachage SHA-256 encodé en Base64
     * @implNote O(k) — k = longueur du mot de passe ; SHA-256 traite des blocs de taille fixe (64 octets).
     *           En pratique O(1) car les mots de passe ont une longueur bornée.
     */
    public static String hash(String plainPassword, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashed = md.digest(plainPassword.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Échec du hachage du mot de passe", e);
        }
    }

    /**
     * Vérifie un mot de passe en clair par rapport au hachage et au sel stockés.
     *
     * @param plainPassword mot de passe candidat
     * @param storedHash    hachage stocké
     * @param storedSalt    sel stocké
     * @return true si le mot de passe est correct
     * @implNote O(1) en pratique — hachage de taille bornée + comparaison de deux chaînes de longueur fixe (44 chars Base64).
     */
    public static boolean verify(String plainPassword, String storedHash, String storedSalt) {
        String candidate = hash(plainPassword, storedSalt);
        return candidate.equals(storedHash);
    }

    /**
     * Hache un mot de passe et retourne la chaîne {@code "sel:hachage"} prête à stocker.
     *
     * @param plainPassword mot de passe en clair
     * @return chaîne {@code "sel:hachage"}
     * @implNote O(1) en pratique — génération d'un sel fixe + un seul hachage SHA-256.
     */
    public static String hashForStorage(String plainPassword) {
        String salt = generateSalt();
        String hash = hash(plainPassword, salt);
        return salt + ":" + hash;
    }

    /**
     * Vérifie un mot de passe par rapport à une chaîne {@code "sel:hachage"} produite par {@link #hashForStorage}.
     *
     * @param plainPassword mot de passe candidat
     * @param saltAndHash   chaîne {@code "sel:hachage"}
     * @return true si le mot de passe est correct
     * @implNote O(1) en pratique — split sur le premier ':', puis un seul hachage SHA-256 et une comparaison.
     */
    public static boolean verifyStored(String plainPassword, String saltAndHash) {
        if (saltAndHash == null || !saltAndHash.contains(":")) return false;
        String[] parts = saltAndHash.split(":", 2);
        return verify(plainPassword, parts[1], parts[0]);
    }
}
