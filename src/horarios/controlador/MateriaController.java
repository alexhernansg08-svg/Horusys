/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.MateriaDAO;
import horarios.modelo.Materia;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: MateriaController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Intermediario entre MateriasWindow (vista) y MateriaDAO (datos).
 *
 *  Reglas de negocio:
 *    - clave y nombre son obligatorios.
 *    - Debe tener una especialidad seleccionada (id > 0).
 * ============================================================
 */
public class MateriaController {

    private final MateriaDAO dao = new MateriaDAO();
    
    /**
     * Llama al DAO para buscar materias que coincidan con el nombre.
     */
    public List<Materia> buscarPorNombre(String nombre) {
        return dao.buscarPorNombre(nombre);
    }

    /** Retorna todas las materias con nombre de especialidad via JOIN. */
    public List<Materia> obtenerTodas() {
        return dao.obtenerTodas();
    }

    /** Busca una materia por su clave (incluye idModulo). Retorna null si no existe. */
    public Materia buscarPorClave(String clave) {
        return dao.buscarPorClave(clave);
    }

    /**
     * Valida y agrega una nueva materia al catalogo.
     * @return null si exito, mensaje de error si falla
     */
    public String agregar(Materia materia) {
        String error = validar(materia);
        if (error != null) return error;
        return dao.agregar(materia) ? null : "Error al agregar materia.";
    }

    /**
     * Valida y actualiza una materia existente.
     * @return null si exito, mensaje de error si falla
     */
    public String actualizar(Materia materia) {
        String error = validar(materia);
        if (error != null) return error;
        return dao.actualizar(materia) ? null : "Error al actualizar materia.";
    }

    /**
     * Elimina una materia por su ID.
     * Si profesores o horarios la referencian, la BD puede rechazarlo.
     * @return null si exito, mensaje de error si falla
     */
    public String eliminar(int idMateria) {
        return dao.eliminar(idMateria) ? null : "No se pudo eliminar la materia.";
    }

    // ---------------------------------------------------------------
    //  Validacion interna compartida
    // ---------------------------------------------------------------

    /**
     * Verifica que la materia tenga los datos minimos requeridos.
     * @return mensaje de error o null si todo esta correcto
     */
    private String validar(Materia materia) {
        // Regla 1: clave y nombre son requeridos
        if (materia.getClave().isBlank() || materia.getNombre().isBlank())
            return "Clave y Nombre son obligatorios.";

        // Regla 2: especialidad valida seleccionada
        if (materia.getIdEspecialidad() <= 0)
            return "Debe seleccionar una especialidad.";

        return null;
    }
}
