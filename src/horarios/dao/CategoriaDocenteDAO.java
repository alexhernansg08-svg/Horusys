/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.CategoriaDocente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: CategoriaDocenteDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "categoria_docente" (singular).
 *  PK real: "clave" (varchar, tipo clave presupuestal SEP), no un id
 *  autoincremental. Implementa el CRUD completo.
 * ============================================================
 */
public class CategoriaDocenteDAO {

    private static final String SQL_SELECT_ALL =
        "SELECT clave, descripcion, horas_frente_grupo, horas_actividades_complementarias " +
        "FROM categoria_docente ORDER BY descripcion, clave";

    private static final String SQL_SELECT_CLAVE =
        "SELECT clave, descripcion, horas_frente_grupo, horas_actividades_complementarias " +
        "FROM categoria_docente WHERE clave = ?";

    private static final String SQL_INSERT =
        "INSERT INTO categoria_docente (clave, descripcion, horas_frente_grupo, horas_actividades_complementarias) " +
        "VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE categoria_docente SET descripcion = ?, horas_frente_grupo = ?, " +
        "horas_actividades_complementarias = ? WHERE clave = ?";

    private static final String SQL_DELETE =
        "DELETE FROM categoria_docente WHERE clave = ?";

    /** Inserta una nueva categoria. La clave la define quien la crea (no es autoincremental). */
    public boolean agregar(CategoriaDocente c) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, c.getClave());
            ps.setString(2, c.getDescripcion());
            ps.setInt(3, c.getHorasFrenteGrupo());
            ps.setInt(4, c.getHorasActividadesComplementarias());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: clave duplicada (unique/primary key)
            return false;
        }
    }

    public boolean actualizar(CategoriaDocente c) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, c.getDescripcion());
            ps.setInt(2, c.getHorasFrenteGrupo());
            ps.setInt(3, c.getHorasActividadesComplementarias());
            ps.setString(4, c.getClave());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una categoria por clave.
     * Si algun profesor la tiene asignada, no truena: profesor.categoria_docente_clave
     * esta definida ON DELETE SET NULL, asi que solo se desvincula.
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

    public List<CategoriaDocente> obtenerTodas() {
        List<CategoriaDocente> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    public CategoriaDocente buscarPorClave(String clave) {
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

    private CategoriaDocente mapear(ResultSet rs) throws SQLException {
        CategoriaDocente c = new CategoriaDocente();
        c.setClave(rs.getString("clave"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setHorasFrenteGrupo(rs.getInt("horas_frente_grupo"));
        c.setHorasActividadesComplementarias(rs.getInt("horas_actividades_complementarias"));
        return c;
    }
}
