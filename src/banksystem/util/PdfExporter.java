package banksystem.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Génère des fichiers PDF 1.4 valides en Java SE pur — aucune bibliothèque externe.
 * Supporte le retour à la ligne automatique, les largeurs de colonnes personnalisées et la pagination.
 */
public class PdfExporter {

    private static final BankLogger logger = BankLogger.getInstance();

    // Dimensions d'une page A4 en points (1 pt = 1/72 pouce)
    private static final float PAGE_W     = 595f;
    private static final float PAGE_H     = 842f;
    private static final float MARGIN     = 40f;
    private static final float LINE_H     = 14f;
    private static final float CHAR_WIDTH = 5.5f;  // largeur moyenne d'un caractère Helvetica 10pt

    private PdfExporter() {}

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Export avec largeurs de colonnes égales, sans préfixe ni suffixe supplémentaires.
     * Le nom du fichier suit le format : {@code <title>_<horodatage>.pdf}.
     *
     * @param title     titre du rapport (utilisé comme préfixe du nom de fichier)
     * @param headers   en-têtes des colonnes
     * @param rows      données à exporter
     * @param outputDir répertoire de sortie
     * @return chemin absolu du fichier créé
     * @implNote O(n·k) — n = nombre de lignes, k = nombre de colonnes + temps de rendu PDF.
     */
    public static String export(String title, String[] headers,
                                List<String[]> rows, String outputDir) throws IOException {
        return export(title, "", headers, rows, outputDir, null);
    }

    /**
     * Export avec largeurs de colonnes égales et suffixe personnalisé.
     * Le nom du fichier suit le format : {@code <title>_<horodatage><suffix>.pdf}.
     *
     * @param title     titre du rapport (utilisé comme préfixe du nom de fichier)
     * @param suffix    suffixe ajouté avant l'extension (ex. {@code "_Final"}, {@code ""} pour aucun)
     * @param headers   en-têtes des colonnes
     * @param rows      données à exporter
     * @param outputDir répertoire de sortie
     * @return chemin absolu du fichier créé
     * @implNote O(n·k) — n = nombre de lignes, k = nombre de colonnes + temps de rendu PDF.
     */
    public static String export(String title, String suffix, String[] headers,
                                List<String[]> rows, String outputDir) throws IOException {
        return export(title, suffix, headers, rows, outputDir, null);
    }

    /**
     * Export avec largeurs de colonnes personnalisées (en points PDF).
     * Passer {@code null} pour colWidths afin d'utiliser la distribution égale.
     *
     * @param title     titre du rapport (utilisé comme préfixe du nom de fichier)
     * @param headers   en-têtes des colonnes
     * @param rows      données à exporter
     * @param outputDir répertoire de sortie
     * @param colWidths largeurs de colonnes en points ({@code null} = égales)
     * @return chemin absolu du fichier créé
     * @implNote O(n·k) — n = nombre de lignes, k = nombre de colonnes + temps de rendu PDF.
     */
    public static String export(String title, String[] headers,
                                List<String[]> rows, String outputDir,
                                float[] colWidths) throws IOException {
        return export(title, "", headers, rows, outputDir, colWidths);
    }

    /**
     * Export avec largeurs de colonnes personnalisées et suffixe.
     * Passer {@code null} pour colWidths afin d'utiliser la distribution égale.
     * Le nom du fichier suit le format : {@code <title>_<horodatage><suffix>.pdf}.
     *
     * @param title     titre du rapport (utilisé comme préfixe du nom de fichier)
     * @param suffix    suffixe ajouté avant l'extension (ex. {@code "_Approuvé"}, {@code ""} pour aucun)
     * @param headers   en-têtes des colonnes
     * @param rows      données à exporter
     * @param outputDir répertoire de sortie
     * @param colWidths largeurs de colonnes en points ({@code null} = égales)
     * @return chemin absolu du fichier créé
     * @implNote O(n·k) — n = nombre de lignes, k = nombre de colonnes + temps de rendu PDF.
     */
    public static String export(String title, String suffix, String[] headers,
                                List<String[]> rows, String outputDir,
                                float[] colWidths) throws IOException {
        new File(outputDir).mkdirs();
        String date     = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String safeName = title.replaceAll("[^a-zA-Z0-9_-]", "_");
        String safeSuffix = (suffix != null ? suffix : "").replaceAll("[^a-zA-Z0-9_-]", "_");
        String filePath = outputDir + File.separator + safeName + "_" + date + safeSuffix + ".pdf";

        float[] widths = resolveWidths(headers.length, colWidths);
        byte[]  pdf    = buildPdf(title, headers, rows, widths);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(pdf);
        }
        logger.info("PdfExporter", "PDF exporté : " + filePath + " (" + rows.size() + " lignes)");
        return filePath;
    }

    // -------------------------------------------------------------------------
    // Calcul des largeurs de colonnes
    // -------------------------------------------------------------------------

    private static float[] resolveWidths(int colCount, float[] provided) {
        if (provided != null && provided.length == colCount) return provided;
        float usable = PAGE_W - 2 * MARGIN;
        float[] w = new float[colCount];
        Arrays.fill(w, colCount > 0 ? usable / colCount : usable);
        return w;
    }

    /** Retourne la position X de la colonne i (somme des largeurs précédentes). */
    private static float colX(float[] widths, int i) {
        float x = MARGIN;
        for (int j = 0; j < i; j++) x += widths[j];
        return x;
    }

    /** Nombre maximum de caractères affichables dans une colonne. */
    private static int maxChars(float colWidth) {
        return Math.max(1, (int) (colWidth / CHAR_WIDTH));
    }

    // -------------------------------------------------------------------------
    // Retour à la ligne automatique
    // -------------------------------------------------------------------------

    private static String[] wrapText(String text, int maxCols) {
        if (text == null || text.isEmpty()) return new String[]{""};
        text = text.replace("\r", "").replace("\t", "  ");
        if (text.length() <= maxCols) return new String[]{text};

        List<String> lines = new ArrayList<>();
        while (text.length() > maxCols) {
            int breakAt = text.lastIndexOf(' ', maxCols);
            if (breakAt <= 0) breakAt = maxCols;
            lines.add(text.substring(0, breakAt).trim());
            text = text.substring(breakAt).trim();
        }
        if (!text.isEmpty()) lines.add(text);
        return lines.toArray(new String[0]);
    }

    // -------------------------------------------------------------------------
    // Construction de la structure PDF
    // -------------------------------------------------------------------------

    private static byte[] buildPdf(String title, String[] headers,
                                   List<String[]> rows, float[] widths) throws IOException {

        // Répartition des lignes sur les pages
        List<List<String[]>> pages = paginateRows(title, headers, rows, widths);

        // Flux de contenu par page
        List<byte[]> pageStreams = new ArrayList<>();
        for (int p = 0; p < pages.size(); p++) {
            String cs = buildContentStream(title, headers, pages.get(p), widths, p + 1, pages.size());
            pageStreams.add(cs.getBytes("ISO-8859-1"));
        }

        int numPages = pageStreams.size();

        // Numérotation des objets PDF (base 1) :
        //   1 = Catalog
        //   2 = Pages
        //   3..2+n = objets Page
        //   3+n..2+2n = flux de contenu
        //   3+2n = police Helvetica-Bold
        //   4+2n = police Helvetica

        int fontBoldObj   = 3 + 2 * numPages;
        int fontPlainObj  = 4 + 2 * numPages;
        int totalObjects  = fontPlainObj;

        List<byte[]> objects  = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();

        // Obj 1 — Catalog
        objects.add(obj(1, "<< /Type /Catalog /Pages 2 0 R >>"));

        // Obj 2 — Pages
        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < numPages; i++) kids.append(3 + i).append(" 0 R ");
        kids.append("]");
        objects.add(obj(2, "<< /Type /Pages /Kids " + kids + " /Count " + numPages + " >>"));

        // Obj 3..2+n — objets Page
        for (int i = 0; i < numPages; i++) {
            int contentObj = 3 + numPages + i;
            String pageDict = "<< /Type /Page /Parent 2 0 R"
                + " /MediaBox [0 0 " + (int)PAGE_W + " " + (int)PAGE_H + "]"
                + " /Contents " + contentObj + " 0 R"
                + " /Resources << /Font << /F1 " + fontBoldObj + " 0 R /F2 " + fontPlainObj + " 0 R >> >>"
                + " >>";
            objects.add(obj(3 + i, pageDict));
        }

        // Obj 3+n..2+2n — flux de contenu
        for (int i = 0; i < numPages; i++) {
            byte[] stream = pageStreams.get(i);
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            s.write(("" + (3 + numPages + i) + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n").getBytes("ISO-8859-1"));
            s.write(stream);
            s.write("\nendstream\nendobj\n".getBytes("ISO-8859-1"));
            objects.add(s.toByteArray());
        }

        // Polices
        objects.add(obj(fontBoldObj,  "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
        objects.add(obj(fontPlainObj, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

        // Assemblage du fichier
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("%PDF-1.4\n%âãÏÓ\n").getBytes("ISO-8859-1"));

        for (byte[] o : objects) {
            offsets.add(out.size());
            out.write(o);
        }

        // Table de références croisées
        int xrefOffset = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 ").append(totalObjects + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int off : offsets) xref.append(String.format("%010d 00000 n \n", off));
        out.write(xref.toString().getBytes("ISO-8859-1"));

        String trailer = "trailer\n<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>\nstartxref\n"
                       + xrefOffset + "\n%%EOF\n";
        out.write(trailer.getBytes("ISO-8859-1"));
        return out.toByteArray();
    }

    private static byte[] obj(int num, String dict) throws UnsupportedEncodingException {
        return (num + " 0 obj\n" + dict + "\nendobj\n").getBytes("ISO-8859-1");
    }

    // -------------------------------------------------------------------------
    // Pagination — répartit les lignes sur les pages
    // -------------------------------------------------------------------------

    private static List<List<String[]>> paginateRows(String title, String[] headers,
                                                      List<String[]> rows, float[] widths) {
        // Zone d'en-tête : titre(22) + date(14) + ligne(4) + colonne(20) + espace(8) ≈ 68
        float headerHeight = 68f;
        float footerHeight = 24f;
        float usableHeight = PAGE_H - MARGIN - headerHeight - footerHeight;

        List<List<String[]>> pages = new ArrayList<>();
        List<String[]> current    = new ArrayList<>();
        float usedHeight = 0;

        for (String[] row : rows) {
            int maxLines = 1;
            for (int i = 0; i < headers.length && i < row.length; i++) {
                String cell = row[i] != null ? row[i] : "";
                String[] wrapped = wrapText(cell, maxChars(widths[i]));
                maxLines = Math.max(maxLines, wrapped.length);
            }
            float rowH = maxLines * LINE_H + 2;

            if (usedHeight + rowH > usableHeight && !current.isEmpty()) {
                pages.add(current);
                current    = new ArrayList<>();
                usedHeight = 0;
            }
            current.add(row);
            usedHeight += rowH;
        }
        if (!current.isEmpty()) pages.add(current);
        if (pages.isEmpty()) pages.add(new ArrayList<>());
        return pages;
    }

    // -------------------------------------------------------------------------
    // Flux de contenu par page
    // -------------------------------------------------------------------------

    private static String buildContentStream(String title, String[] headers,
                                             List<String[]> rows, float[] widths,
                                             int pageNum, int totalPages) {
        StringBuilder sb = new StringBuilder();
        String genDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        float y = PAGE_H - MARGIN;

        // Titre
        sb.append(text(MARGIN, y, "/F1", 16, escape(title)));
        y -= 22;

        // Date de génération
        sb.append(text(MARGIN, y, "/F2", 9, "Généré le : " + escape(genDate)
                + "    Page " + pageNum + " de " + totalPages));
        y -= 14;

        // Séparateur
        sb.append(hLine(y));
        y -= 12;

        // En-têtes de colonnes
        for (int i = 0; i < headers.length; i++) {
            sb.append(text(colX(widths, i), y, "/F1", 10, escape(headers[i])));
        }
        y -= LINE_H;
        sb.append(hLine(y));
        y -= 6;

        // Lignes de données
        for (String[] row : rows) {
            String[][] wrapped = new String[headers.length][];
            int maxLines = 1;
            for (int i = 0; i < headers.length; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                wrapped[i] = wrapText(cell, maxChars(widths[i]));
                maxLines   = Math.max(maxLines, wrapped[i].length);
            }

            float rowH = maxLines * LINE_H + 2;

            for (int i = 0; i < headers.length; i++) {
                float x = colX(widths, i);
                for (int ln = 0; ln < wrapped[i].length; ln++) {
                    sb.append(text(x, y - ln * LINE_H, "/F2", 9, escape(wrapped[i][ln])));
                }
            }
            y -= rowH;
        }

        // Pied de page
        sb.append(hLine(MARGIN - 6));
        sb.append(text(MARGIN, MARGIN - 18, "/F2", 8, "BankSystem - Confidentiel"));
        sb.append(text(PAGE_W - MARGIN - 60, MARGIN - 18, "/F2", 8,
                "Page " + pageNum + " / " + totalPages));

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Fonctions d'aide au dessin PDF
    // -------------------------------------------------------------------------

    /** Affiche une chaîne avec une matrice de texte absolue — sans décalage cumulatif. */
    private static String text(float x, float y, String font, int size, String content) {
        return "BT\n" + font + " " + size + " Tf\n"
             + "1 0 0 1 " + String.format("%.2f", x) + " " + String.format("%.2f", y) + " Tm\n"
             + "(" + content + ") Tj\nET\n";
    }

    /** Trace une ligne horizontale sur toute la largeur utile de la page. */
    private static String hLine(float y) {
        return String.format("%.2f %.2f m\n%.2f %.2f l\nS\n",
            MARGIN, y, PAGE_W - MARGIN, y);
    }

    /** Échappe les caractères spéciaux des chaînes PDF. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", "")
                .replace("\n", " ");
    }
}
