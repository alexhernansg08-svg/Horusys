/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.Especialidad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: EspecialidadDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "especialidades".
 *  Implementa el CRUD completo (Create, Read, Update, Delete).
 *
 *  Patron usado:
 *    - Todas las queries SQL son constantes estaticas (arriba del todo)
 *      para que sean faciles de leer y modificar sin tocar la logica.
 *    - Metodo privado mapear(ResultSet) centraliza la conversion de
 *      fila SQL -> objeto Especialidad (evita repetir el mismo codigo).
 *    - try-with-resources garantiza que Connection, PreparedStatement
 *      y ResultSet se cierren automaticamente aunque haya excepciones.
 *
 *  Quien la llama: EspecialidadController (nunca la vista directamente).
 * ============================================================
 */
public class EspecialidadDAO {

    // ---------------------------------------------------------------
    //  Constantes SQL
    // ---------------------------------------------------------------

    /** Trae todas las especialidades ordenadas por nombre. */
    private static final String SQL_SELECT_ALL =
        "SELECT id, nombre, codigo FROM especialidades ORDER BY nombre";

    /** Busca una especialidad por su ID (para buscarPorId). */
    private static final String SQL_SELECT_ID =
        "SELECT id, nombre, codigo FROM especialidades WHERE id = ?";

    /**
     * Inserta una nueva especialidad.
     * RETURNING id: PostgreSQL retorna el ID autogenerado en el mismo INSERT,
     * sin necesidad de hacer una query extra (getGeneratedKeys).
     */
    private static final String SQL_INSERT =
        "INSERT INTO especialidades (nombre, codigo) VALUES (?, ?) RETURNING id";

    /** Actualiza nombre y codigo de una especialidad existente por su ID. */
    private static final String SQL_UPDATE =
        "UPDATE especialidades SET nombre = ?, codigo = ? WHERE id = ?";

    /** Elimina una especialidad por ID. Puede fallar si tiene grupos/materias asociados. */
    private static final String SQL_DELETE =
        "DELETE FROM especialidades WHERE id = ?";

    // ---------------------------------------------------------------
    //  Operaciones CRUD
    // ---------------------------------------------------------------

    /**
     * Inserta una nueva especialidad en la BD.
     * Usa RETURNING id para obtener el ID generado sin query adicional
     * y se lo asigna al objeto (e.setId), que queda actualizado.
     *
     * e especialidad a insertar (id sera ignorado, lo asigna la BD)
     * @return true si se inserto correctamente
     */
    public boolean agregar(Especialidad e) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getCodigo());

            // executeQuery() en lugar de executeUpdate() porque usamos RETURNING
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    e.setId(rs.getInt("id")); // actualizar el objeto con el ID real de BD
                    return true;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: codigo duplicado (unique constraint)
        }
        return false;
    }

    /**
     * Actualiza nombre y codigo de una especialidad existente.
     * executeUpdate() retorna el numero de filas afectadas; si es > 0, exito.
     *
     * e especialidad con id, nombre y codigo actualizados
     * @return true si se modifico al menos una fila
     */
    public boolean actualizar(Especialidad e) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getCodigo());
            ps.setInt(3, e.getId());    // clausula WHERE id = ?
            return ps.executeUpdate() > 0; // > 0 significa que se actualizo algo
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una especialidad por su ID.
     * Si tiene grupos o materias relacionados, la BD lanzara
     * un error de FK constraint y se retornara false.
     *
     * id ID de la especialidad a eliminar
     * @return true si se elimino correctamente
     */
    public boolean eliminar(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: FK violation si tiene grupos asociados
            return false;
        }
    }

    /**
     * Retorna todas las especialidades en una lista ordenada por nombre.
     * Usado para poblar tablas y JComboBox en la UI.
     *
     * @return lista de Especialidad (puede estar vacia si la tabla esta vacia)
     */
    public List<Especialidad> obtenerTodas() {
        List<Especialidad> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            // Recorrer cada fila y convertirla en objeto Especialidad
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    /**
     * Busca una especialidad especifica por su ID.
     * Retorna null si no existe (el controlador debe manejar este caso).
     *
     * id ID a buscar
     * @return Especialidad encontrada, o null si no existe
     */
    public Especialidad buscarPorId(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs); // fila encontrada: mapear y retornar
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null; // no encontrado
    }
    
    /**
     * Busca especialidades cuyo nombre contenga el texto indicado (ignora mayúsculas/minúsculas).
     */
    public List<Especialidad> buscarPorNombre(String nombre) {
        List<Especialidad> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, codigo FROM especialidades WHERE nombre ILIKE ? ORDER BY nombre";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            // El comodín % permite buscar coincidencias parciales
            ps.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error al buscar especialidad por nombre: " + ex.getMessage());
        }
        return lista;
    }

    // ---------------------------------------------------------------
    //  Helper de mapeo (ResultSet -> objeto Especialidad)
    // ---------------------------------------------------------------

    /**
     * Convierte la fila actual de un ResultSet en un objeto Especialidad.
     * Se reutiliza en todos los metodos de lectura para no repetir codigo.
     *
     * Precondicion: el ResultSet debe estar posicionado en una fila valida
     * (rs.next() debe haber retornado true antes de llamar este metodo).
     *
     * rs ResultSet posicionado en la fila a convertir
     * @return objeto Especialidad con los datos de la fila
     */
    private Especialidad mapear(ResultSet rs) throws SQLException {
        Especialidad e = new Especialidad();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setCodigo(rs.getString("codigo"));
        return e;
    }
}
