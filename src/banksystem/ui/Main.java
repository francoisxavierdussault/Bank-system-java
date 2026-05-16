package banksystem.ui;

import java.awt.EventQueue;

/**
 * Point d'entrée de l'application.
 * Lance la fenêtre de connexion, qui redirige ensuite vers BankManager ou BankCustomer
 * selon le rôle de l'utilisateur authentifié.
 */
public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
