package banksystem.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Journal singleton — enregistre chaque opération bancaire dans un fichier
 * daté sous le répertoire "logs/" (ex. logs/log_2026-05-13.txt).
 */
public class BankLogger {

    private static final BankLogger INSTANCE = new BankLogger();
    private static final String LOG_DIR = "logs";
    private static final SimpleDateFormat FILE_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIMESTAMP_FMT  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private BankLogger() {
        new File(LOG_DIR).mkdirs();
    }

    public static BankLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Écrit une entrée dans le fichier journal du jour.
     *
     * @param level   niveau de sévérité (INFO, WARN, ERROR)
     * @param source  classe ou couche qui a émis l'événement (ex. "CustomerService")
     * @param message description lisible de l'opération
     * @implNote O(1) — une seule écriture séquentielle en fin de fichier (append).
     */
    public void log(String level, String source, String message) {
        String today    = FILE_DATE_FMT.format(new Date());
        String filePath = LOG_DIR + File.separator + "log_" + today + ".txt";
        String entry    = "[" + TIMESTAMP_FMT.format(new Date()) + "] [" + level + "] [" + source + "] " + message;

        System.out.println(entry);

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("BankLogger - Écriture impossible : " + e.getMessage());
        }
    }

    public void info(String source, String message)  { log("INFO",  source, message); }
    public void warn(String source, String message)  { log("WARN",  source, message); }
    public void error(String source, String message) { log("ERROR", source, message); }
}
