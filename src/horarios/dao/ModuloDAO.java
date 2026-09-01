/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.Modulo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: ModuloDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "modulos" (Modulo I a V de
 *  Competencias laborales, uno por especialidad+semestre).
 * ============================================================
 */
public class ModuloDAO {

    private static final String SQL_SELECT_ALL = """
        SELECT m.id_modulo, m.id_especialidad, m.id_semestre, m.numero, m.nombre, m.clave,
               COALESCE(e.nombre, 'Sin especialidad') AS especialidad_nombre
        FROM modulos m
        LEFT JOIN especialidades e ON m.id_especialidad = e.id
        ORDER BY m.id_especialidad, m.numero
        """;

    private static final String SQL_SELECT_POR_ESPECIALIDAD = """
        SELECT m.id_modulo, m.id_especialidad, m.id_semestre, m.numero, m.nombre, m.clave,
               COALESCE(e.nombre, 'Sin especialidad') AS especialidad_nombre
        FROM modulos m
        LEFT JOIN especialidades e ON m.id_especialidad = e.id
        WHERE m.id_especialidad = ?
        ORDER BY m.numero
        """;

    private static final String SQL_INSERT =
        "INSERT INTO modulos (id_especialidad, id_semestre, numero, nombre, clave) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE modulos SET id_especialidad=?, id_semestre=?, numero=?, nombre=?, clave=? WHERE id_modulo=?";

    private static final String SQL_DELETE =
        "DELETE FROM modulos WHERE id_modulo = ?";

    public boolean agregar(Modulo m) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            setParams(ps, m);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: ya existe ese numero de modulo para esa especialidad
            return false;
        }
    }

    public boolean actualizar(Modulo m) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            setParams(ps, m);
            ps.setInt(6, m.getIdModulo());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un modulo. Las materias que lo tuvieran asignado quedan con
     * id_modulo = NULL (ON DELETE SET NULL en la FK), no se borran.
     */
    public boolean eliminar(int idModulo) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idModulo);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<Modulo> obtenerTodos() {
        List<Modulo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /** Trae los 5 (o menos) modulos de una especialidad especifica, en orden. */
    public List<Modulo> obtenerPorEspecialidad(int idEspecialidad) {
        List<Modulo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_POR_ESPECIALIDAD)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    private void setParams(PreparedStatement ps, Modulo m) throws SQLException {
        ps.setInt(1, m.getIdEspecialidad());
        ps.setInt(2, m.getIdSemestre());
        ps.setInt(3, m.getNumero());
        ps.setString(4, m.getNombre());
        ps.setString(5, m.getClave());
    }

    private Modulo mapear(ResultSet rs) throws SQLException {
        Modulo m = new Modulo();
        m.setIdModulo(rs.getInt("id_modulo"));
        m.setIdEspecialidad(rs.getInt("id_especialidad"));
        m.setIdSemestre(rs.getInt("id_semestre"));
        m.setNumero(rs.getInt("numero"));
        m.setNombre(rs.getString("nombre"));
        m.setClave(rs.getString("clave"));
        m.setEspecialidadNombre(rs.getString("especialidad_nombre"));
        return m;
    }
}
