package horarios.dao;

import horarios.modelo.Grupo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  DAO: GrupoDAO
 *  CAPA: Data Access Object
 * ============================================================
 */
public class GrupoDAO {

    // ---------------------------------------------------------------
    //  Constantes SQL
    // ---------------------------------------------------------------

    /**
     * Trae todos los grupos con nombre de especialidad y nombre completo del tutor via LEFT JOIN.
     */
    private static final String SQL_SELECT_ALL = """
        SELECT g.*, 
               e.nombre AS especialidad_nombre,
               TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS tutor_nombre
        FROM grupos g
        LEFT JOIN especialidades e ON g.especialidad_id = e.id
        LEFT JOIN profesor p ON g.id_tutor = p.id_profesor
        ORDER BY g.semestre ASC, g.nombre ASC
        """;

    private static final String SQL_INSERT = """
        INSERT INTO grupos (nombre, codigo, especialidad_id, semestre, turno, capacidad, id_tutor)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    private static final String SQL_UPDATE = """
        UPDATE grupos
        SET nombre = ?, codigo = ?, especialidad_id = ?,
            semestre = ?, turno = ?, capacidad = ?, id_tutor = ?
        WHERE id = ?
        """;

    private static final String SQL_DELETE = "DELETE FROM grupos WHERE id = ?";

    // ---------------------------------------------------------------
    //  Operaciones CRUD
    // ---------------------------------------------------------------

    public boolean agregar(Grupo g) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            setParams(ps, g);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    g.setId(rs.getInt("id"));
                    return true;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al agregar grupo: " + ex.getMessage());
        }
        return false;
    }

    public boolean actualizar(Grupo g) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setParams(ps, g);
            ps.setInt(8, g.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Error al actualizar grupo: " + ex.getMessage());
            return false;
        }
    }

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

    public List<Grupo> obtenerPorMateria(int materiaId) {
        List<Grupo> lista = new ArrayList<>();
        String sql = """
            SELECT g.*, 
                   e.nombre AS especialidad_nombre,
                   TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS tutor_nombre
            FROM grupos g
            LEFT JOIN especialidades e ON g.especialidad_id = e.id
            LEFT JOIN profesor p ON g.id_tutor = p.id_profesor
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
    
    public List<Grupo> buscarPorNombre(String nombre) {
        List<Grupo> lista = new ArrayList<>();
        String sql = """
            SELECT g.*, 
                   e.nombre AS especialidad_nombre,
                   TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS tutor_nombre
            FROM grupos g
            LEFT JOIN especialidades e ON g.especialidad_id = e.id
            LEFT JOIN profesor p ON g.id_tutor = p.id_profesor
            WHERE g.nombre ILIKE ?
            ORDER BY g.semestre ASC, g.nombre ASC
            """;           
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
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
     * Busca un grupo por su código único (utilizado para el Módulo de Horarios y Excel).
     */
    public Grupo obtenerPorCodigo(String codigo) {
        String sql = """
            SELECT g.*, 
                   e.nombre AS especialidad_nombre,
                   TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS tutor_nombre
            FROM grupos g
            LEFT JOIN especialidades e ON g.especialidad_id = e.id
            LEFT JOIN profesor p ON g.id_tutor = p.id_profesor
            WHERE g.codigo = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error al obtener grupo por código: " + ex.getMessage());
        }
        return null;
    }

    private void setParams(PreparedStatement ps, Grupo g) throws SQLException {
        ps.setString(1, g.getNombre());
        ps.setString(2, g.getCodigo());
        ps.setInt(3, g.getEspecialidadId());
        ps.setInt(4, g.getSemestre());
        ps.setString(5, g.getTurno());
        ps.setInt(6, g.getCapacidad() > 0 ? g.getCapacidad() : 30);
        ps.setObject(7, g.getIdTutor(), Types.INTEGER);
    }

    private Grupo mapear(ResultSet rs) throws SQLException {
        Grupo g = new Grupo();
        g.setId(rs.getInt("id"));
        g.setNombre(rs.getString("nombre"));
        g.setCodigo(rs.getString("codigo"));
        g.setEspecialidadId(rs.getInt("especialidad_id"));
        g.setEspecialidadNombre(rs.getString("especialidad_nombre"));
        g.setSemestre(rs.getInt("semestre"));
        g.setTurno(rs.getString("turno"));
        g.setCapacidad(rs.getInt("capacidad"));
        g.setIdTutor(rs.getObject("id_tutor") != null ? rs.getInt("id_tutor") : null);
        
        // Mapea el nombre del tutor
        String tNombre = rs.getString("tutor_nombre");
        if (tNombre != null && !tNombre.isBlank()) {
            g.setTutorNombre(tNombre);
        } else {
            g.setTutorNombre("Sin Tutor");
        }
        
        return g;
    }
}