package banksystem.util;

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.event.*;

/**
 * Détecte l'inactivité de l'utilisateur et déclenche un rappel après un délai configurable.
 * S'attache à une fenêtre via {@link #attach(JFrame, int, Runnable)} et réinitialise
 * le chronomètre à chaque mouvement de souris ou frappe de touche.
 */
public class SessionManager {

    private static final BankLogger logger = BankLogger.getInstance();

    private final javax.swing.Timer idleTimer;
    private final String            role;
    private long                    sessionStart;

    /**
     * @param role           "Manager" ou "Customer" — utilisé dans les journaux
     * @param timeoutSeconds secondes d'inactivité avant le déclenchement de onTimeout
     * @param onTimeout      rappel exécuté à l'expiration de la session (sur l'EDT)
     */
    public SessionManager(String role, int timeoutSeconds, Runnable onTimeout) {
        this.role = role;
        this.idleTimer = new javax.swing.Timer(timeoutSeconds * 1000, e -> {
            logger.warn("SessionManager", role + " session expirée après " + timeoutSeconds + "s d'inactivité");
            onTimeout.run();
        });
        this.idleTimer.setRepeats(false);
    }

    /**
     * Attache ce gestionnaire de session à une fenêtre.
     * Démarre le chronomètre et enregistre les écouteurs souris/clavier.
     *
     * @param frame          fenêtre à surveiller
     * @param timeoutSeconds durée du délai (pour les journaux)
     * @param unused         paramètre de symétrie API — passer null
     */
    public void attach(JFrame frame, int timeoutSeconds, Runnable unused) {
        sessionStart = System.currentTimeMillis();
        logger.info("SessionManager", "Session " + role + " démarrée");

        AWTEventListener resetListener = event -> reset();
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
            resetListener,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
        );

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                end();
                java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(resetListener);
            }
        });

        idleTimer.start();
    }

    /** Réinitialise le compte à rebours — appelé à chaque activité de l'utilisateur. */
    public void reset() {
        idleTimer.restart();
    }

    /** Arrête le chronomètre et journalise la durée de la session. */
    public void end() {
        idleTimer.stop();
        long seconds = (System.currentTimeMillis() - sessionStart) / 1000;
        logger.info("SessionManager", "Session " + role + " terminée après " + seconds + "s");
    }
}
