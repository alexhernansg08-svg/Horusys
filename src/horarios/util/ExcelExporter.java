/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.util;

import horarios.dao.HorarioConsultaDAO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.*;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  UTILIDAD: ExcelExporter
 * ============================================================
 *  Genera archivos Excel (.xlsx) con el horario generado,
 *  usando la libreria Apache POI (XSSF para formato xlsx).
 *
 *  Funciones disponibles:
 *    - exportarHorarioPorGrupo:    hoja Excel del horario de un grupo
 *    - exportarHorarioPorProfesor: hoja Excel del horario de un docente
 *    - exportarHorarioGeneral:     hoja Excel del horario completo
 *
 *  Caracteristicas del Excel generado:
 *    - Cabecera con logotipo institucional e info del grupo/profesor.
 *    - Colores por materia (mapa COLOR_MATERIA -> color de celda).
 *    - Colores institucionales vino y amarillo en encabezados.
 *    - Celdas combinadas para el titulo principal.
 *    - Fila de receso coloreada en verde.
 *    - Ancho de columnas ajustado automaticamente.
 *
 *  Patron de diseno: Utility Class con metodos estaticos.
 *  Constructor privado: no se instancia, solo se llaman los metodos estaticos.
 * ============================================================
 */
public final class ExcelExporter {

    // ── Colores institucionales (ARGB hex para POI) ──────────────────────
    private static final String COL_VINO        = "FF640019"; // encabezado principal
    private static final String COL_VINO_CLARO  = "FF8B0000"; // fila de días
    private static final String COL_AMARILLO    = "FFCCA000"; // fila de horas / subtítulos
    private static final String COL_VERDE       = "FF1E7A1E"; // receso
    private static final String COL_AZUL        = "FF1565C0"; // Humanidades / cols frías
    private static final String COL_NARANJA     = "FFE65100"; // Interacciones
    private static final String COL_CELESTE     = "FF0277BD"; // Temas selectos
    private static final String COL_MORADO      = "FF6A1B9A"; // Organismos
    private static final String COL_ROSA        = "FFC62828"; // Tutorías
    private static final String COL_CAFE        = "FF4E342E"; // Diseña apps
    private static final String COL_VERDE2      = "FF2E7D32"; // Implementa apps
    private static final String COL_AMARILLO2   = "FFF9A825"; // Conciencia histórica
    private static final String COL_BLANCO      = "FFFFFFFF";
    private static final String COL_GRIS_CLARO  = "FFF5F5F5";

    // Mapa de palabras clave → color de fondo de celda
    private static final Map<String, String> COLOR_MATERIA = new LinkedHashMap<>();
    static {
        COLOR_MATERIA.put("HUMANIDADES",        COL_AZUL);
        COLOR_MATERIA.put("INTERACCIONES",       COL_NARANJA);
        COLOR_MATERIA.put("TEMAS SELECTOS",      COL_CELESTE);
        COLOR_MATERIA.put("ORGANISMOS",          COL_MORADO);
        COLOR_MATERIA.put("TUTORI",              COL_ROSA);     // TUTORÍA / TUTORÍAS
        COLOR_MATERIA.put("DISE\u00d1A APLICACIONES",  COL_CAFE);
        COLOR_MATERIA.put("IMPLEMENTA APLICACIONES", COL_VERDE2);
        COLOR_MATERIA.put("CONCIENCIA HIST",     COL_AMARILLO2);
    }

    private static final String[] DIAS = {"HORA", "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES"};

    private ExcelExporter() {}

    public static void exportarHorarioPorGrupo(java.awt.Component parent,
            String grupo, String tutor, String aula, String especialidad) {
        HorarioConsultaDAO dao = new HorarioConsultaDAO();
        List<Map<String, Object>> filas = dao.obtenerHorarioPorGrupo(grupo);
        if (filas.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "No hay datos de horario para el grupo " + grupo,
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String titulo = "HORARIO DE GRUPO: " + grupo;
        String subtitulo = "AULA: " + aula + "     ESPECIALIDAD: " + especialidad;
        String tutorLine = "TUTOR: " + tutor;
        guardarExcel(parent, grupo,
            wb -> construirHoja(wb, titulo, subtitulo, tutorLine, filas));
    }

    /**
     * Exporta el horario general (todos los grupos).
     */
    public static void exportarHorarioGeneral(java.awt.Component parent) {
        HorarioConsultaDAO dao = new HorarioConsultaDAO();
        List<Map<String, Object>> filas = dao.obtenerHorarioGeneral();
        if (filas.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "No hay horario general generado.", "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        guardarExcel(parent, "Horario_General",
            wb -> construirHoja(wb, "HORARIO GENERAL", "PERÍODO ESCOLAR: FEBRERO – JULIO 2026", "", filas));
    }

    /**
     * Exporta el horario personal de un profesor.
     */
    public static void exportarHorarioPorProfesor(java.awt.Component parent,
            String rfc, String nombreProfesor) {
        HorarioConsultaDAO dao = new HorarioConsultaDAO();
        List<Map<String, Object>> filas = dao.obtenerHorarioPorProfesor(rfc);
        if (filas.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "No hay clases asignadas a " + nombreProfesor,
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        guardarExcel(parent, "Horario_" + rfc,
            wb -> construirHoja(wb, "HORARIO DEL PROFESOR",
                    "PROF: " + nombreProfesor, "", filas));
    }

    // =====================================================================
    //  CONSTRUCCIÓN DE LA HOJA
    // =====================================================================

    private static void construirHoja(XSSFWorkbook wb,
            String titulo, String subtitulo, String tutorLine,
            List<Map<String, Object>> filas) {

        XSSFSheet sheet = wb.createSheet("Horario");
        sheet.setDefaultColumnWidth(22);
        sheet.setColumnWidth(0, 14 * 256); // columna HORA más angosta

        int fila = 0;

        // ── Fila 0: título principal ──────────────────────────────────────
        fila = crearFilaTitulo(wb, sheet, fila, titulo,
                COL_VINO, COL_BLANCO, 16, true);

        // ── Fila 1: subtítulo (período / aula / especialidad) ────────────
        if (!subtitulo.isBlank()) {
            fila = crearFilaTitulo(wb, sheet, fila, subtitulo,
                    COL_AMARILLO, "FF000000", 11, false);
        }

        // ── Fila 2: tutor ────────────────────────────────────────────────
        if (!tutorLine.isBlank()) {
            fila = crearFilaTitulo(wb, sheet, fila, tutorLine,
                    COL_GRIS_CLARO, "FF000000", 10, false);
        }

        // ── Fila encabezado de días ───────────────────────────────────────
        XSSFRow rowDias = sheet.createRow(fila++);
        rowDias.setHeightInPoints(22);
        for (int c = 0; c < DIAS.length; c++) {
            XSSFCell cell = rowDias.createCell(c);
            cell.setCellValue(DIAS[c]);
            cell.setCellStyle(estiloCabeceraDia(wb));
        }

        // ── Filas de datos ────────────────────────────────────────────────
        boolean par = true;
        for (Map<String, Object> row : filas) {
            String hora = String.valueOf(row.getOrDefault("HORA", ""));

            // Detectar RECESO
            boolean esReceso = hora.equalsIgnoreCase("RECESO")
                    || String.valueOf(row.getOrDefault("LUNES", "")).toUpperCase().contains("RECESO")
                    || esHoraReceso(hora);

            XSSFRow excelRow = sheet.createRow(fila++);
            excelRow.setHeightInPoints(esReceso ? 18 : 42);

            // Columna HORA
            XSSFCell cHora = excelRow.createCell(0);
            cHora.setCellValue(formatearHora(hora));
            cHora.setCellStyle(estiloHora(wb, esReceso));

            if (esReceso) {
                // Merge toda la fila y poner "RECESO"
                cHora.setCellValue("R E C E S O");
                sheet.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 0, 5));
                cHora.setCellStyle(estiloReceso(wb));
            } else {
                String[] cols = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
                for (int c = 0; c < cols.length; c++) {
                    String contenido = String.valueOf(row.getOrDefault(cols[c], ""));
                    XSSFCell cell = excelRow.createCell(c + 1);
                    cell.setCellValue(contenido.replace("\\n", "\n"));
                    cell.setCellStyle(estiloCeldaMateria(wb, contenido, par));
                }
                par = !par;
            }
        }

        // ── Ajustar altura mínima fila encabezado ─────────────────────────
        sheet.createFreezePane(0, 3 + (subtitulo.isBlank() ? 0 : 1) + (tutorLine.isBlank() ? 0 : 1));
    }

    // =====================================================================
    //  HELPERS DE FILA
    // =====================================================================

    private static int crearFilaTitulo(XSSFWorkbook wb, XSSFSheet sheet,
            int filaIdx, String texto, String bgHex, String fgHex,
            int fontSize, boolean bold) {
        XSSFRow row = sheet.createRow(filaIdx);
        row.setHeightInPoints(bold ? 28 : 18);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(texto);
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(hexToBytes(bgHex), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(hexToBytes(fgHex), null));
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        font.setFontName("Arial");
        style.setFont(font);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(filaIdx, filaIdx, 0, 5));
        return filaIdx + 1;
    }

    // =====================================================================
    //  ESTILOS
    // =====================================================================

    private static XSSFCellStyle estiloCabeceraDia(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(COL_VINO_CLARO), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(hexToBytes(COL_BLANCO), null));
        f.setBold(true);
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private static XSSFCellStyle estiloHora(XSSFWorkbook wb, boolean esReceso) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(COL_AMARILLO), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 9);
        s.setFont(f);
        return s;
    }

    private static XSSFCellStyle estiloReceso(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(COL_VERDE), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(hexToBytes(COL_BLANCO), null));
        f.setBold(true);
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private static XSSFCellStyle estiloCeldaMateria(XSSFWorkbook wb,
            String contenido, boolean par) {
        String bgColor = resolverColor(contenido, par);
        boolean oscuro = !bgColor.equals(COL_GRIS_CLARO) && !bgColor.equals(COL_BLANCO);

        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(bgColor), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);

        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(hexToBytes(oscuro ? COL_BLANCO : "FF000000"), null));
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 8);
        s.setFont(f);
        return s;
    }

    // =====================================================================
    //  LÓGICA DE COLOR
    // =====================================================================

    private static String resolverColor(String contenido, boolean par) {
        if (contenido == null || contenido.isBlank())
            return par ? COL_BLANCO : COL_GRIS_CLARO;
        String upper = contenido.toUpperCase();
        for (Map.Entry<String, String> e : COLOR_MATERIA.entrySet()) {
            if (upper.contains(e.getKey())) return e.getValue();
        }
        return par ? COL_BLANCO : COL_GRIS_CLARO;
    }

    private static boolean esHoraReceso(String hora) {
        // Marca la hora 14:40-15:00 como receso (ajustar según tu turno)
        return hora != null && (hora.startsWith("14:40") || hora.equals("14:00-15:00"));
    }

    // =====================================================================
    //  UTILIDADES
    // =====================================================================

    private static String formatearHora(String hora) {
        // Si viene como "13:00" lo devuelve igual;
        // si viene como "13:00-13:50" lo deja igual
        return hora == null ? "" : hora.trim();
    }

    /** Convierte AARRGGBB hex a byte[4] para XSSFColor */
    private static byte[] hexToBytes(String hex) {
        // hex puede ser AARRGGBB (8 chars) o RRGGBB (6 chars)
        String h = hex.startsWith("FF") ? hex.substring(2) : hex;
        // Devolvemos RGB
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    // =====================================================================
    //  GUARDAR CON DIÁLOGO
    // =====================================================================

    @FunctionalInterface
    private interface SheetBuilder {
        void build(XSSFWorkbook wb);
    }

    private static void guardarExcel(java.awt.Component parent,
            String nombreSugerido, SheetBuilder builder) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Horario_" + nombreSugerido + ".xlsx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel 2007+ (*.xlsx)", "xlsx"));
        chooser.setDialogTitle("Guardar horario como Excel");

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File archivo = chooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".xlsx"))
            archivo = new File(archivo.getAbsolutePath() + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            builder.build(wb);
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                wb.write(out);
            }
            JOptionPane.showMessageDialog(parent,
                "✅ Excel guardado exitosamente:\n" + archivo.getAbsolutePath(),
                "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                "❌ Error al guardar el archivo:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}