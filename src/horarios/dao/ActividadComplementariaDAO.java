/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.ActividadComplementaria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: ActividadComplementariaDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "actividades_complementarias"
 *  (clave PK varchar, tipo_actividad, actividad, horas).
 *  Esta tabla YA EXISTIA en la base de datos con 15 registros
 *  precargados; este DAO se adapto a su estructura real
 *  (en vez de crear una tabla nueva y duplicada).
 * ============================================================
 */
public class ActividadComplementariaDAO {

    private static final String SQL_SELECT_ALL =
        "SELECT clave, tipo_actividad, actividad, horas FROM actividades_complementarias " +
        "ORDER BY tipo_actividad, clave";

    private static final String SQL_SELECT_CLAVE =
        "SELECT clave, tipo_actividad, actividad, horas FROM actividades_complementarias WHERE clave = ?";

    private static final String SQL_INSERT =
        "INSERT INTO actividades_complementarias (clave, tipo_actividad, actividad, horas) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE actividades_complementarias SET tipo_actividad = ?, actividad = ?, horas = ? WHERE clave = ?";

    private static final String SQL_DELETE =
        "DELETE FROM actividades_complementarias WHERE clave = ?";

    /**
     * Inserta una nueva actividad. A diferencia de un catalogo con ID autoincremental,
     * aqui la "clave" la escribe el usuario (asi esta definida la PK de esta tabla).
     */
    public boolean agregar(ActividadComplementaria a) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, a.getClave());
            ps.setString(2, a.getTipoActividad());
            ps.setString(3, a.getActividad());
            ps.setInt(4, a.getHoras());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: clave duplicada, o horas <= 0 (viola el CHECK de la tabla)
            return false;
        }
    }

    public boolean actualizar(ActividadComplementaria a) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, a.getTipoActividad());
            ps.setString(2, a.getActividad());
            ps.setInt(3, a.getHoras());
            ps.setString(4, a.getClave());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una actividad por clave.
     * profesor.actividad_complementaria_clave esta definida ON DELETE SET NULL,
     * asi que los profesores que la tuvieran asignada solo se desvinculan.
     */
    public boolean eliminar(String clave) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setString(1, clave);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<ActividadComplementaria> obtenerTodas() {
        List<ActividadComplementaria> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    public ActividadComplementaria buscarPorClave(String clave) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_CLAVE)) {

            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private ActividadComplementaria mapear(ResultSet rs) throws SQLException {
        ActividadComplementaria a = new ActividadComplementaria();
        a.setClave(rs.getString("clave"));
        a.setTipoActividad(rs.getString("tipo_actividad"));
        a.setActividad(rs.getString("actividad"));
        a.setHoras(rs.getInt("horas"));
        return a;
    }
}
