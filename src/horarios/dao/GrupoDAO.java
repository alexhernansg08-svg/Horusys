/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.Grupo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: GrupoDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "grupos" con JOIN a "especialidades".
 *
 *  Particularidades de este DAO:
 *    1. SQL_SELECT_ALL usa LEFT JOIN con especialidades para obtener
 *       el nombre de la especialidad en la misma query (sin N+1).
 *    2. setParams() centraliza los 7 parametros comunes entre
 *       INSERT y UPDATE (evita duplicacion de codigo).
 *    3. El campo id_tutor es nullable (Integer, no int), por lo que
 *       se usa ps.setObject(idx, valor, Types.INTEGER) y
 *       rs.getObject() + rs.getInt() para leerlo de forma segura.
 *    4. SQL_INSERT usa RETURNING id para obtener el PK autogenerado.
 *
 *  Tabla en BD: grupos (id, nombre, codigo, especialidad_id,
 *                        semestre, turno, capacidad, id_tutor)
 * ============================================================
 */
public class GrupoDAO {

    // ---------------------------------------------------------------
    //  Constantes SQL (Text Blocks de Java 15+ para mayor legibilidad)
    // ---------------------------------------------------------------

    /**
     * Trae todos los grupos con nombre de especialidad via LEFT JOIN.
     * LEFT JOIN asegura que los grupos sin especialidad asignada
     * igual aparezcan en el resultado (especialidad_nombre seria null).
     * Ordenado por semestre asc y nombre asc para la tabla de la UI.
     */
    private static final String SQL_SELECT_ALL = """
        SELECT g.*, e.nombre AS especialidad_nombre
        FROM grupos g
        LEFT JOIN especialidades e ON g.especialidad_id = e.id
        ORDER BY g.semestre ASC, g.nombre ASC
        """;

    /**
     * Inserta un nuevo grupo con RETURNING id para recuperar el PK generado.
     * Los 7 campos son: nombre, codigo, especialidad_id, semestre, turno, capacidad, id_tutor.
     */
    private static final String SQL_INSERT = """
        INSERT INTO grupos (nombre, codigo, especialidad_id, semestre, turno, capacidad, id_tutor)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    /**
     * Actualiza los 7 campos de un grupo existente por su ID (parametro 8).
     */
    private static final String SQL_UPDATE = """
        UPDATE grupos
        SET nombre = ?, codigo = ?, especialidad_id = ?,
            semestre = ?, turno = ?, capacidad = ?, id_tutor = ?
        WHERE id = ?
        """;

    /** Elimina un grupo por su ID. */
    private static final String SQL_DELETE = "DELETE FROM grupos WHERE id = ?";

    // ---------------------------------------------------------------
    //  Operaciones CRUD
    // ---------------------------------------------------------------

    /**
     * Inserta un nuevo grupo en la BD.
     * Usa setParams() para los 7 campos y luego lee el ID generado con RETURNING.
     * Si tiene exito, actualiza g.setId() con el ID real asignado por PostgreSQL.
     *
     * g grupo a insertar
     * @return true si se inserto correctamente
     */
    public boolean agregar(Grupo g) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            setParams(ps, g);             // asignar los 7 parametros comunes
            try (ResultSet rs = ps.executeQuery()) { //hacemos el insert
                if (rs.next()) {
                    g.setId(rs.getInt("id")); // actualizar el objeto con el PK real
                    return true;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al agregar grupo: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Actualiza los datos de un grupo existente.
     * Llama a setParams() para los 7 campos comunes y luego
     * agrega el parametro 8 (id del WHERE).
     *
     * g grupo con el id del registro y los nuevos valores
     * @return true si se modifico al menos una fila
     */
    public boolean actualizar(Grupo g) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setParams(ps, g);          // llenamos los ? con los datos del objeto
            ps.setInt(8, g.getId());   // guardamos el id en el objeto de java
            return ps.executeUpdate() > 0; //si es 1 es porque se encontro el grupo, si es 0 es false
        } catch (SQLException ex) {
            System.err.println("Error al actualizar grupo: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Elimina un grupo por su ID.
     * Puede fallar si el grupo tiene asignaciones en el horario (FK constraint).
     *
     * id ID del grupo a eliminar
     * @return true si se elimino correctamente
     */
    public boolean eliminar(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Error al eliminar grupo: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Retorna todos los grupos con el nombre de especialidad incluido (via JOIN).
     * Usado para llenar la JTable en GruposWindow.
     *
     * @return lista de Grupo (puede estar vacia)
     */
    public List<Grupo> obtenerTodos() {
        List<Grupo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            System.err.println("Error al obtener grupos: " + ex.getMessage());
        }
        return lista;
    }

    /**
     * Retorna los grupos que tienen asignada una materia especifica
     * segun la tabla materia_grupos.
     * Usado en el dialogo de asignacion de materias para mostrar
     * solo los grupos que cursan esa materia.
     *
     * @param materiaId ID de la materia a filtrar
     * @return lista de Grupo que llevan esa materia (puede estar vacia)
     */
    public List<Grupo> obtenerPorMateria(int materiaId) {
        List<Grupo> lista = new ArrayList<>();
        String sql = """
            SELECT g.*, e.nombre AS especialidad_nombre
            FROM grupos g
            LEFT JOIN especialidades e ON g.especialidad_id = e.id
            WHERE g.id IN (
                SELECT grupo_id FROM materia_grupos WHERE materia_id = ?
            )
            ORDER BY g.semestre ASC, g.nombre ASC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, materiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error al obtener grupos por materia: " + ex.getMessage());
        }
        return lista;
    }
    
    /**
     * Busca grupos por nombre.
     */
    public List<Grupo> buscarPorNombre(String nombre) {
        List<Grupo> lista = new ArrayList<>();
        String sql = """
            SELECT g.*, e.nombre AS especialidad_nombre
            FROM grupos g
            LEFT JOIN especialidades e ON g.especialidad_id = e.id
            WHERE g.nombre ILIKE ?
            ORDER BY g.semestre ASC, g.nombre ASC
            """;           
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            // El % permite buscar "1a", "1A", etc.
            ps.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error al buscar grupo por nombre: " + ex.getMessage());
        }
        return lista;
    }

    /**
     * Asigna los 7 parametros comunes a INSERT y UPDATE.
     * Nota en parametro 6 (capacidad):
     *   Si capacidad es 0, se usa 30 como default.
     *
     * ps PreparedStatement del INSERT o UPDATE
     * g  objeto Grupo con los datos a asignar
     */
    private void setParams(PreparedStatement ps, Grupo g) throws SQLException {
        ps.setString(1, g.getNombre());
        ps.setString(2, g.getCodigo());
        ps.setInt(3, g.getEspecialidadId());
        ps.setInt(4, g.getSemestre());
        ps.setString(5, g.getTurno());
        ps.setInt(6, g.getCapacidad() > 0 ? g.getCapacidad() : 30); // default 30 si no se especifico
        ps.setObject(7, g.getIdTutor(), Types.INTEGER);              // null-safe para FK opcional
    }

    /**
     * Convierte la fila actual del ResultSet en un objeto Grupo.
     * rs ResultSet posicionado en una fila valida
     * @return objeto Grupo con todos sus campos poblados
     */
    private Grupo mapear(ResultSet rs) throws SQLException {
        Grupo g = new Grupo();
        g.setId(rs.getInt("id"));
        g.setNombre(rs.getString("nombre"));
        g.setCodigo(rs.getString("codigo"));
        g.setEspecialidadId(rs.getInt("especialidad_id"));
        g.setEspecialidadNombre(rs.getString("especialidad_nombre")); // viene del JOIN
        g.setSemestre(rs.getInt("semestre"));
        g.setTurno(rs.getString("turno"));
        g.setCapacidad(rs.getInt("capacidad"));
        // Lectura null-safe del FK opcional id_tutor
        g.setIdTutor(rs.getObject("id_tutor") != null ? rs.getInt("id_tutor") : null);
        return g;
    }
}
