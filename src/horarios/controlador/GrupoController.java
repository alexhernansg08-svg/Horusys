/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.GrupoDAO;
import horarios.modelo.Grupo;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: GrupoController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Reglas de negocio que aplica:
 *    - nombre y codigo del grupo son obligatorios.
 *    - Debe haberse seleccionado una especialidad valida (id > 0).
 * ============================================================
 */
public class GrupoController {

    private final GrupoDAO dao = new GrupoDAO();

    /** Retorna todos los grupos con nombre de especialidad, ordenados por semestre y nombre. */
    public List<Grupo> obtenerTodos() {
        return dao.obtenerTodos();
    }

    /**
     * Retorna solo los grupos que tienen asignada la materia indicada.
     * Consulta la tabla materia_grupos para filtrar.
     *
     * @param materiaId ID de la materia
     * @return lista de grupos que cursan esa materia
     */
    public List<Grupo> obtenerPorMateria(int materiaId) {
        return dao.obtenerPorMateria(materiaId);
    }

    /**
     * Valida y agrega un nuevo grupo.
     */
    public String agregar(Grupo grupo) {
        String error = validar(grupo); // validacion primero
        if (error != null) return error;
        return dao.agregar(grupo) ? null
            : "Error al agregar grupo. Verifica que el codigo no este repetido.";
    }

    /**
     * Valida y actualiza un grupo existente.
     */
    public String actualizar(Grupo grupo) {
        String error = validar(grupo);
        if (error != null) return error;
        return dao.actualizar(grupo) ? null : "Error al actualizar el grupo.";
    }

    /**
     * Elimina un grupo por ID.
     * Si el grupo tiene asignaciones en horarios, la BD puede rechazarlo.
     *
     * @param id id del grupo a eliminar
     * @return null si exito, o mensaje de error
     */
    public String eliminar(int id) {
        return dao.eliminar(id) ? null : "No se pudo eliminar el grupo.";
    }

    // ---------------------------------------------------------------
    //  Metodo privado de validacion (reutilizado en agregar y actualizar)
    // ---------------------------------------------------------------

    /**
     * Verifica que el objeto Grupo tenga los campos minimos requeridos.
     *
     * @param grupo objeto a validar
     * @return mensaje de error, o null si todo esta correcto
     */
    private String validar(Grupo grupo) {
        // Regla 1: nombre y codigo no pueden estar vacios
        if (grupo.getNombre().isBlank() || grupo.getCodigo().isBlank())
            return "Nombre y Codigo son obligatorios.";

        // Regla 2: debe tener una especialidad seleccionada (id valido > 0)
        if (grupo.getEspecialidadId() <= 0)
            return "Debe seleccionar una especialidad.";

        return null; // sin errores
    }

    /**
     * Busca grupos cuyo nombre coincida parcialmente (insensible a mayusculas).
     * Usado por GruposWindow para el dialogo de busqueda.
     *
     * @param nombre termino de busqueda parcial
     * @return lista de grupos que coinciden
     */
    public java.util.List<Grupo> buscarPorNombre(String nombre) {
        return dao.buscarPorNombre(nombre);
    }
}
