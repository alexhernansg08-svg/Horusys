/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;
import java.sql.*;
import java.text.Normalizer;
import java.util.*;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: HorarioConsultaDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Responsabilidad unica: LEER el horario ya generado.
 *
 *  Metodos publicos:
 *    - obtenerHorarioGeneral      -> vista completa (hora x dias)
 *    - obtenerHorarioPorGrupo     -> horario filtrado por grupo
 *    - obtenerHorarioPorProfesor  -> horario filtrado por RFC
 *    - buscarHorarioPorNombreProfesor -> busqueda parcial por nombre
 *    - obtenerCodigosGrupos       -> lista de grupos con clases
 *    - obtenerProfesoresConNombre -> pares [rfc, nombre] para combos
 *    - validarHorarioDetallado    -> lista de problemas encontrados
 *    - validarHorarioSinChoques   -> true si el horario es valido
 *
 *  Tablas que lee: horario_general, horario_grupos,
 *                  horario_profesores, grupos, materias, profesor
 * ============================================================
 */
public class HorarioConsultaDAO {

    // ---------------------------------------------------------------
    //  Normalizacion de dias (compartida con HorarioDAO)
    // ---------------------------------------------------------------

    private static final Map<String, String> DIA_NORM = new HashMap<>();
    static {
        DIA_NORM.put("lunes",     "Lunes");
        DIA_NORM.put("martes",    "Martes");
        DIA_NORM.put("miercoles", "Miercoles");
        DIA_NORM.put("jueves",    "Jueves");
        DIA_NORM.put("viernes",   "Viernes");
    }

    // ---------------------------------------------------------------
    //  Consultas principales
    // ---------------------------------------------------------------

    /**
     * Retorna el horario general (todas las horas con al menos una clase).
     * Formato de cada fila: HORA, LUNES, MARTES, MIERCOLES, JUEVES, VIERNES.
     */
    public List<Map<String,Object>> obtenerHorarioGeneral() {
        List<Map<String,Object>> l = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT hora,lunes,martes,miercoles,jueves,viernes FROM horario_general " +
                "WHERE hora <> 'META' " +
                "  AND (lunes<>'' OR martes<>'' OR miercoles<>'' OR jueves<>'' OR viernes<>'') " +
                "ORDER BY hora");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> f = new LinkedHashMap<>();
                f.put("HORA",      rs.getString("hora"));
                f.put("LUNES",     nvl(rs.getString("lunes")));
                f.put("MARTES",    nvl(rs.getString("martes")));
                f.put("MIERCOLES", nvl(rs.getString("miercoles")));
                f.put("JUEVES",    nvl(rs.getString("jueves")));
                f.put("VIERNES",   nvl(rs.getString("viernes")));
                l.add(f);
            }
        } catch (SQLException e) { System.err.println("Error hGeneral: " + e.getMessage()); }
        return l;
    }

    /**
     * Retorna el horario de un grupo en formato pivote (hora x dias).
     * Cada celda: "materia\nprofesor"
     *
     * @param grupo codigo del grupo (Ej: "1A-INF")
     */
    public List<Map<String,Object>> obtenerHorarioPorGrupo(String grupo) {
        return queryPivote(
            "SELECT hora,dia,materia,profesor FROM horario_grupos WHERE grupo=? ORDER BY hora",
            grupo,
            (rs, piv) -> piv.computeIfAbsent(rs.getString("hora"), k -> new LinkedHashMap<>())
                            .put(normDia(rs.getString("dia")),
                                 rs.getString("materia") + "\n" + rs.getString("profesor")));
    }

    /**
     * Retorna el horario de un profesor en formato pivote (hora x dias).
     * Cada celda: "materia\n[grupo]"
     *
     * @param rfc RFC del profesor
     */
    public List<Map<String,Object>> obtenerHorarioPorProfesor(String rfc) {
        return queryPivote(
            "SELECT hora,dia,materia,grupo FROM horario_profesores WHERE rfc_profesor=? ORDER BY hora",
            rfc,
            (rs, piv) -> piv.computeIfAbsent(rs.getString("hora"), k -> new LinkedHashMap<>())
                            .put(normDia(rs.getString("dia")),
                                 rs.getString("materia") + "\n[" + rs.getString("grupo") + "]"));
    }

    /**
     * Busca el horario de un profesor por coincidencia parcial de nombre.
     *
     * @param nombre termino de busqueda (se aplica con ILIKE %nombre%)
     */
    public List<Map<String,Object>> buscarHorarioPorNombreProfesor(String nombre) {
        return queryPivote(
            "SELECT hora,dia,materia,grupo FROM horario_profesores WHERE nombre_profesor ILIKE ? ORDER BY hora",
            "%" + nombre + "%",
            (rs, piv) -> piv.computeIfAbsent(rs.getString("hora"), k -> new LinkedHashMap<>())
                            .put(normDia(rs.getString("dia")),
                                 rs.getString("materia") + "\n[" + rs.getString("grupo") + "]"));
    }

    /**
     * Retorna los codigos de grupos que tienen al menos una clase asignada.
     * Usado para poblar el JComboBox de grupos en HorariosWindow.
     */
    public List<String> obtenerCodigosGrupos() {
        List<String> l = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT DISTINCT grupo FROM horario_grupos ORDER BY grupo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(rs.getString("grupo"));
        } catch (SQLException e) { System.err.println("Error grupos: " + e.getMessage()); }
        return l;
    }

    /**
     * Retorna pares [rfc, nombreCompleto] de todos los profesores.
     * Usado para poblar el JComboBox de profesores en HorariosWindow.
     */
    public List<String[]> obtenerProfesoresConNombre() {
        List<String[]> l = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT rfc, nombre || ' ' || apellidos AS nombre_completo FROM profesor ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                l.add(new String[]{rs.getString("rfc"), rs.getString("nombre_completo")});
        } catch (SQLException e) { System.err.println("Error profes: " + e.getMessage()); }
        return l;
    }

    // ---------------------------------------------------------------
    //  Validacion
    // ---------------------------------------------------------------

    /**
     * Valida el horario generado y retorna la lista de problemas encontrados.
     *
     * Revisiones (4 queries SQL):
     *   1. Choques de grupo: mismo grupo, mismo dia/hora, 2+ clases.
     *   2. Choques de profesor: mismo profesor, mismo dia/hora, 2+ clases.
     *   3. Grupos sin ninguna clase asignada.
     *   4. Materias con menos horas asignadas de las requeridas.
     *
     * @return lista de problemas, o lista con "SIN PROBLEMAS..." si todo esta bien
     */
    public List<String> validarHorarioDetallado() {
        List<String> p = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery(
                    "SELECT grupo, dia, hora, COUNT(*) AS cnt FROM horario_grupos " +
                    "WHERE grupo <> '(Actividad)' " + // no es un grupo real: no debe validarse como choque de grupo
                    "GROUP BY grupo, dia, hora HAVING COUNT(*) > 1 ORDER BY grupo, dia, hora")) {
                while (rs.next())
                    p.add(String.format("CHOQUE GRUPO: %s tiene %d clases el %s a las %s",
                        rs.getString("grupo"), rs.getInt("cnt"),
                        rs.getString("dia"), rs.getString("hora")));
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT nombre_profesor, dia, hora, COUNT(*) AS cnt FROM horario_profesores " +
                    "GROUP BY nombre_profesor, dia, hora HAVING COUNT(*) > 1 " +
                    "ORDER BY nombre_profesor, dia, hora")) {
                while (rs.next())
                    p.add(String.format("CHOQUE PROF: %s tiene %d clases el %s a las %s",
                        rs.getString("nombre_profesor"), rs.getInt("cnt"),
                        rs.getString("dia"), rs.getString("hora")));
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT g.codigo FROM grupos g WHERE NOT EXISTS " +
                    "(SELECT 1 FROM horario_grupos hg WHERE hg.grupo = g.codigo)")) {
                while (rs.next())
                    p.add("SIN CLASES: grupo " + rs.getString("codigo"));
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT m.nombre AS mat, g.codigo AS grp, " +
                    "       m.horas_semanales AS req, COALESCE(hg.cnt,0) AS asi " +
                    "FROM materias m " +
                    "JOIN grupos g ON g.especialidad_id = m.id_especialidad " +
                    "             AND g.semestre = m.id_semestre " +
                    "LEFT JOIN (SELECT grupo, materia, COUNT(*) AS cnt " +
                    "           FROM horario_grupos GROUP BY grupo, materia) hg " +
                    "       ON hg.grupo = g.codigo AND hg.materia = m.nombre " +
                    "WHERE EXISTS (SELECT 1 FROM profesor_materia pm " +
                    "              WHERE pm.materia_id = m.id_materia) " +
                    "  AND COALESCE(hg.cnt,0) < m.horas_semanales " +
                    "ORDER BY g.codigo, m.nombre")) {
                while (rs.next())
                    p.add(String.format("INCOMPLETA: %s / %s - %d/%d hrs",
                        rs.getString("grp"), rs.getString("mat"),
                        rs.getInt("asi"), rs.getInt("req")));
            }

            int total = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM horario_grupos")) {
                if (rs.next()) total = rs.getInt(1);
            }

            if (p.isEmpty())
                p.add("SIN PROBLEMAS - " + total + " bloques asignados correctamente.");
            else
                p.add(0, "Bloques asignados: " + total + " | Problemas: " + p.size());

        } catch (SQLException e) {
            p.add("Error al validar: " + e.getMessage());
        }
        return p;
    }

    /**
     * Version simplificada: retorna true si no hay ningun problema.
     */
    public boolean validarHorarioSinChoques() {
        List<String> p = validarHorarioDetallado();
        return p.size() == 1 && p.get(0).startsWith("SIN PROBLEMAS");
    }

    // ---------------------------------------------------------------
    //  Infraestructura privada: pivote y utilidades
    // ---------------------------------------------------------------

    @FunctionalInterface
    private interface PivoteFiller {
        void fill(ResultSet rs, Map<String,Map<String,String>> piv) throws SQLException;
    }

    /** Ejecuta una query con un parametro y transforma el resultado en formato pivote. */
    private List<Map<String,Object>> queryPivote(String sql, String param, PivoteFiller filler) {
        Map<String,Map<String,String>> piv = new TreeMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) filler.fill(rs, piv);
            }
        } catch (SQLException e) { System.err.println("Error pivote: " + e.getMessage()); }
        return pivote(piv);
    }

    /** Convierte Map(hora -> Map(dia -> contenido)) a List<Map> compatible con DefaultTableModel. */
    private List<Map<String,Object>> pivote(Map<String,Map<String,String>> p) {
        List<Map<String,Object>> l = new ArrayList<>();
        for (Map.Entry<String,Map<String,String>> e : p.entrySet()) {
            Map<String,String> d = e.getValue();
            Map<String,Object> f = new LinkedHashMap<>();
            f.put("HORA",      e.getKey());
            f.put("LUNES",     d.getOrDefault("Lunes",     ""));
            f.put("MARTES",    d.getOrDefault("Martes",    ""));
            f.put("MIERCOLES", d.getOrDefault("Miercoles", ""));
            f.put("JUEVES",    d.getOrDefault("Jueves",    ""));
            f.put("VIERNES",   d.getOrDefault("Viernes",   ""));
            l.add(f);
        }
        return l;
    }

    /** Normaliza el nombre de un dia quitando tildes y capitalizando. Ej: "miércoles" -> "Miercoles". */
    private String normDia(String dia) {
        if (dia == null) return "";
        String sinTildes = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD)
                                     .replaceAll("\\p{M}", "")
                                     .toLowerCase();
        return DIA_NORM.getOrDefault(sinTildes, dia.trim());
    }

    /** Retorna cadena vacia en lugar de null (null-safe para columnas SQL). */
    private String nvl(String s) { return s == null ? "" : s; }
}
