/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.Materia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: MateriaDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "materias" con JOIN a "especialidades".
 *
 *  Particularidades de este DAO:
 *    1. SQL_SELECT_ALL usa LEFT JOIN + COALESCE para que las materias
 *       sin especialidad muestren "Sin especialidad" en lugar de null.
 *    2. mapear() tiene un flag "conEspecialidad" para reutilizarlo
 *       tanto en obtenerTodas() (con JOIN) como en buscarPorClave() (sin JOIN).
 *    3. setParams() centraliza los 5 campos comunes entre INSERT y UPDATE.
 *    4. MateriaDAO NO usa RETURNING id porque el INSERT no lo necesita
 *       (la vista no necesita el id recien insertado).
 *
 *  Tabla en BD: materias (id_materia PK, clave, nombre, id_especialidad,
 *                          id_semestre, horas_semanales)
 * ============================================================
 */
public class MateriaDAO {

    // ---------------------------------------------------------------
    //  Constantes SQL
    // ---------------------------------------------------------------

    /**
     * Trae todas las materias con nombre de especialidad via LEFT JOIN,
     * y la etiqueta de su modulo (si pertenece a uno de Competencias laborales).
     * COALESCE: si especialidades.nombre es null (FK sin referencia), usa 'Sin especialidad'.
     */
    private static final String SQL_SELECT_ALL = """
        SELECT m.id_materia, m.clave, m.nombre, m.id_especialidad,
               m.id_semestre, m.horas_semanales, m.id_modulo,
               COALESCE(e.nombre, 'Sin especialidad') AS especialidad_nombre,
               mo.numero AS modulo_numero, mo.nombre AS modulo_nombre
        FROM materias m
        LEFT JOIN especialidades e ON m.id_especialidad = e.id
        LEFT JOIN modulos mo ON m.id_modulo = mo.id_modulo
        ORDER BY m.id_materia
        """;

    /**
     * Busca una materia por su clave (codigo unico).
     * No incluye el JOIN de especialidad (se llama desde buscarPorClave
     * donde no se necesita el nombre de la especialidad).
     */
    private static final String SQL_SELECT_CLAVE =
        "SELECT id_materia, clave, nombre, id_especialidad, id_semestre, horas_semanales, id_modulo " +
        "FROM materias WHERE clave = ?";

    /**
     * Inserta una nueva materia. No usa RETURNING porque la vista
     * no necesita el id_materia recien generado.
     */
    private static final String SQL_INSERT = """
        INSERT INTO materias (clave, nombre, id_especialidad, id_semestre, horas_semanales, id_modulo)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    /**
     * Actualiza los 6 campos de una materia. El parametro 7 es el id_materia del WHERE.
     */
    private static final String SQL_UPDATE = """
        UPDATE materias
        SET clave = ?, nombre = ?, id_especialidad = ?, id_semestre = ?, horas_semanales = ?, id_modulo = ?
        WHERE id_materia = ?
        """;

    /** Elimina una materia por su id_materia. */
    private static final String SQL_DELETE = "DELETE FROM materias WHERE id_materia = ?";

    // ---------------------------------------------------------------
    //  Operaciones CRUD
    // ---------------------------------------------------------------

    /**
     * Inserta una nueva materia en el catalogo.
     * executeUpdate() > 0 confirma que la fila fue insertada.
     *
     * m materia a insertar (id_materia sera ignorado, lo asigna BD)
     * @return true si se inserto correctamente
     */
    
    public boolean agregar(Materia m) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            setParams(ps, m); // asigna los 5 campos comunes
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: clave duplicada
            return false;
        }
    }

    /**
     * Actualiza una materia existente.
     * setParams() llena los parametros 1-5 y luego se agrega el parametro
     * 6 (id_materia para el WHERE).
     *
     * m materia con id_materia y nuevos valores
     * @return true si se modifico al menos una fila
     */
    public boolean actualizar(Materia m) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setParams(ps, m);               // parametros 1-6 (SET ...)
            ps.setInt(7, m.getIdMateria()); // parametro 7: WHERE id_materia = ?
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una materia por su ID.
     * Puede fallar si la materia esta referenciada en profesor_materia
     * o en el horario generado (FK constraint).
     *
     * idMateria id de la materia a eliminar
     * @return true si se elimino correctamente
     */
    public boolean eliminar(int idMateria) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idMateria);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Retorna todas las materias con nombre de especialidad (via JOIN).
     * Usado para llenar la JTable en MateriasWindow.
     *
     * @return lista de Materia con especialidadNombre poblado
     */
    public List<Materia> obtenerTodas() {
        List<Materia> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            // true = el RS incluye especialidad_nombre (viene del LEFT JOIN)
            while (rs.next()) lista.add(mapear(rs, true));
        } catch (SQLException ex) {
            System.err.println("Error al obtener materias: " + ex.getMessage());
        }
        return lista;
    }

    /**
     * Busca una materia por su clave (codigo unico como "INF-101").
     * Retorna null si no existe.
     * No incluye el nombre de la especialidad (no tiene JOIN).
     *
     * clave clave de la materia a buscar
     * @return Materia encontrada, o null si no existe
     */
    public Materia buscarPorClave(String clave) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_CLAVE)) {

            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                // false = el RS NO incluye especialidad_nombre (query sin JOIN)
                if (rs.next()) return mapear(rs, false);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ---------------------------------------------------------------
    //  Helpers privados
    // ---------------------------------------------------------------

    /**
     * Asigna los 6 parametros comunes a INSERT y UPDATE.
     * Centralizado para no repetir el mismo codigo en agregar() y actualizar().
     *
     * ps PreparedStatement del INSERT o UPDATE
     * m  Materia con los datos a asignar
     */
    private void setParams(PreparedStatement ps, Materia m) throws SQLException {
        ps.setString(1, m.getClave());
        ps.setString(2, m.getNombre());
        ps.setInt(3, m.getIdEspecialidad());
        ps.setInt(4, m.getIdSemestre());
        ps.setInt(5, m.getHorasSemanales());
        if (m.getIdModulo() != null) ps.setInt(6, m.getIdModulo());
        else ps.setNull(6, Types.INTEGER); // materia fuera de Competencias laborales: sin modulo
    }

    /**
     * Convierte la fila actual del ResultSet en un objeto Materia.
     *
     * rs               ResultSet posicionado en una fila valida
     * conEspecialidad  true si el RS incluye las columnas especialidad_nombre/modulo_*
     *                         (vienen de los LEFT JOIN en SQL_SELECT_ALL).
     *                         false en buscarPorClave() donde no hay JOIN.
     * @return objeto Materia con sus campos poblados
     */
    private Materia mapear(ResultSet rs, boolean conEspecialidad) throws SQLException {
        Materia m = new Materia();
        m.setIdMateria(rs.getInt("id_materia"));
        m.setClave(rs.getString("clave"));
        m.setNombre(rs.getString("nombre"));
        m.setIdEspecialidad(rs.getInt("id_especialidad"));
        m.setIdSemestre(rs.getInt("id_semestre"));
        m.setHorasSemanales(rs.getInt("horas_semanales"));

        int idModulo = rs.getInt("id_modulo");
        m.setIdModulo(rs.wasNull() ? null : idModulo);

        // especialidad_nombre y modulo_* solo si el RS trae los JOIN (evita SQLException)
        if (conEspecialidad) {
            m.setEspecialidadNombre(rs.getString("especialidad_nombre"));
            String moduloNombre = rs.getString("modulo_nombre");
            if (moduloNombre != null) {
                int numero = rs.getInt("modulo_numero");
                String[] romanos = {"", "I", "II", "III", "IV", "V"};
                String num = (numero >= 1 && numero <= 5) ? romanos[numero] : String.valueOf(numero);
                m.setModuloEtiqueta("Módulo " + num + " — " + moduloNombre);
            }
        }
        return m;
    }
    
    /**
     * Busca materias cuyo nombre coincida parcialmente (insensible a mayúsculas).
     */
    public List<Materia> buscarPorNombre(String nombre) {
        List<Materia> lista = new ArrayList<>();
        String sql = """
            SELECT m.id_materia, m.clave, m.nombre, m.id_especialidad,
                   m.id_semestre, m.horas_semanales, m.id_modulo,
                   COALESCE(e.nombre, 'Sin especialidad') AS especialidad_nombre,
                   mo.numero AS modulo_numero, mo.nombre AS modulo_nombre
            FROM materias m
            LEFT JOIN especialidades e ON m.id_especialidad = e.id
            LEFT JOIN modulos mo ON m.id_modulo = mo.id_modulo
            WHERE m.nombre ILIKE ?
            ORDER BY m.id_materia
            """;
            
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            // El comodín % permite búsquedas parciales (ej. "algor" -> "Algoritmos")
            ps.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                // Le pasamos 'true' porque la consulta SÍ incluye especialidad_nombre
                while (rs.next()) lista.add(mapear(rs, true));
            }
        } catch (SQLException ex) {
            System.err.println("Error al buscar materia por nombre: " + ex.getMessage());
        }
        return lista;
    }
}
