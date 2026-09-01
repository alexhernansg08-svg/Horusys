/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.EspecialidadDAO;
import horarios.modelo.Especialidad;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: EspecialidadController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Aplicamos dos reglas de negocio:
 *    1. nombre y codigo son OBLIGATORIOS (no pueden estar vacios).
 *    2. El codigo se guarda siempre en MAYUSCULAS.
 * ============================================================
 */
public class EspecialidadController {

    private final EspecialidadDAO dao = new EspecialidadDAO();

    /**
     * Retorna todas las especialidades ordenadas por nombre.
     * Usado para llenar la tabla en EspecialidadesWindow.
     */
    public List<Especialidad> obtenerTodas() {
        return dao.obtenerTodas();
    }

    public String agregar(String nombre, String codigo) {
        // Validacion de negocio: ambos campos son requeridos
        if (nombre.isBlank() || codigo.isBlank()) return "Nombre y Codigo son obligatorios.";

        // Construir el objeto modelo con los datos limpios
        Especialidad e = new Especialidad();
        e.setNombre(nombre.trim());                   // quitar espacios extremos
        e.setCodigo(codigo.trim().toUpperCase());      // forzar mayusculas

        // Delegar al DAO; si falla (ej: codigo duplicado), retorna mensaje de error
        return dao.agregar(e) ? null : "Error al agregar especialidad.";
    }

    /**
     * Valida y actualiza una especialidad existente.
     * Mismas reglas que agregar(), mas el ID del registro a modificar.
     */
    public String actualizar(int id, String nombre, String codigo) {
        if (nombre.isBlank() || codigo.isBlank()) return "Nombre y Codigo son obligatorios.";

        // Construir objeto con los tres datos completos para el UPDATE
        Especialidad e = new Especialidad(id, nombre.trim(), codigo.trim().toUpperCase());
        return dao.actualizar(e) ? null : "Error al actualizar especialidad.";
    }

    /**
     * Elimina una especialidad por su ID.
     * Si tiene grupos o materias asociados, la BD rechazara la operacion
     * (FK constraint) y el DAO retornara false.
     */
    public String eliminar(int id) {
        return dao.eliminar(id) ? null : "No se pudo eliminar la especialidad.";
    }

    /**
     * Busca especialidades cuyo nombre coincida parcialmente (insensible a mayusculas).
     * Usado por EspecialidadesWindow para el dialogo de busqueda.
     *
     * @param nombre termino de busqueda parcial
     * @return lista de especialidades que coinciden
     */
    public java.util.List<Especialidad> buscarPorNombre(String nombre) {
        return dao.buscarPorNombre(nombre);
    }
}


