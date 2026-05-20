package banksystem.ui;

import java.awt.EventQueue;

/**
 * Point d'entrée de l'application.
 * Lance la fenêtre de connexion, qui redirige ensuite vers BankManager ou BankCustomer
 * selon le rôle de l'utilisateur authentifié.
 *
 * <p>Classe utilitaire — ne doit pas être instanciée.</p>
 */
 public class Main {

    /** Classe utilitaire — construction interdite. */
    private Main() {}

    /**
     * Point d'entrée JVM.
     *
     * @param args arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
