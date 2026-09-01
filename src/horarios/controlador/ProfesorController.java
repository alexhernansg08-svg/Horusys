/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.ProfesorDAO;
import horarios.modelo.Profesor;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: ProfesorController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Intermediario entre ProfesoresWindow (vista) y ProfesorDAO (datos).
 *
 *  Ademas del CRUD basico, expone operaciones especializadas:
 *    - guardarDisponibilidad: guarda los bloques horarios del profesor.
 *    - asignarMaterias: relaciona al profesor con las materias que imparte.
 *    - obtenerMateriasAsignadas: lee que materias ya tiene asignadas.
 * ============================================================
 */
public class ProfesorController {

    private final ProfesorDAO dao = new ProfesorDAO();
    
    /**
     * Llama al DAO para buscar profesores que coincidan con el nombre o apellidos.
     */
    public List<Profesor> buscarPorNombre(String nombre) {
        return dao.buscarPorNombre(nombre);
    }

    /**
     * Retorna todos los profesores con sus disponibilidades precargadas.
     * El DAO usa una sola query adicional para evitar el problema N+1.
     */
    public List<Profesor> obtenerTodos() {
        return dao.obtenerTodos();
    }

    /**
     * Busca un profesor especifico por su RFC.
     * Retorna null si no existe.
     */
    public Profesor buscarPorRfc(String rfc) {
        return dao.buscarPorRfc(rfc);
    }

    /**
     * Valida y agrega un nuevo profesor.
     * En modo agregar (validarRfc=true), el RFC es obligatorio.
     *
     * @param profesor objeto con los datos del formulario
     * @return null si exito, o mensaje de error
     */
    public String agregar(Profesor profesor) {
        String error = validar(profesor, true); // true = validar RFC tambien
        if (error != null) return error;
        return dao.agregar(profesor) ? null
            : "Error al agregar profesor. Verifica que el RFC no este repetido.";
    }

    /**
     * Valida y actualiza un profesor existente.
     * El RFC ya existe en BD (no se re-valida como obligatorio).
     *
     * @param profesor objeto con los datos actualizados
     * @return null si exito, o mensaje de error
     */
    public String actualizar(Profesor profesor) {
        String error = validar(profesor, false); // false = no re-validar RFC
        if (error != null) return error;
        return dao.actualizar(profesor) ? null : "Error al actualizar profesor.";
    }

    /**
     * Elimina un profesor y reajusta la secuencia de ID en BD.
     * Si el profesor tiene asignaciones en horarios activos, puede fallar.
     *
     * @param rfc RFC del profesor a eliminar
     * @return null si exito, o mensaje de error
     */
    public String eliminar(String rfc) {
        return dao.eliminar(rfc) ? null : "No se pudo eliminar el profesor.";
    }

    /**
     * Guarda (reemplaza) la disponibilidad horaria de un profesor.
     * El DAO borra los bloques anteriores e inserta los nuevos en batch.
     *
     * @param profesorId  ID interno del profesor
     * @param bloques     lista de bloques dia+horaInicio+horaFin
     * @return true si se guardaron correctamente
     */
    public boolean guardarDisponibilidad(int profesorId, List<Profesor.DisponibilidadBloque> bloques) {
        return dao.guardarDisponibilidad(profesorId, bloques);
    }

    /**
     * Asigna las materias que este profesor puede impartir.
     * Reemplaza la asignacion anterior (DELETE + INSERT batch).
     *
     * @param profesorId  ID interno del profesor
     * @param materiaIds  lista de IDs de materias seleccionadas
     * @return true si se guardaron correctamente
     */
    public boolean asignarMaterias(int profesorId, List<Integer> materiaIds) {
        return dao.asignarMaterias(profesorId, materiaIds);
    }

    // ---------------------------------------------------------------
    //  NOTA: ya no existen asignarMateriasConGrupos()/obtenerGruposAsignados().
    //  La tabla real profesor_materia no tiene columna grupo_id: un profesor
    //  asignado a una materia puede impartirla a cualquier grupo elegible
    //  (asignarMaterias() de arriba ya cubre esto).
    // ---------------------------------------------------------------

    /**
     * Retorna los IDs de las materias actualmente asignadas a un profesor.
     * Usado al abrir el dialogo de edicion para pre-marcar los checkboxes.
     *
     * @param profesorId ID del profesor
     * @return lista de IDs de materias
     */
    public List<Integer> obtenerMateriasAsignadas(int profesorId) {
        return dao.obtenerMateriasAsignadas(profesorId);
    }

    // ---------------------------------------------------------------
    //  NOTA: ya no existen obtenerActividadesAsignadas()/guardarActividades().
    //  La actividad complementaria de un profesor es un solo campo directo
    //  (profesor.getActividadComplementariaClave()/setActividadComplementariaClave())
    //  que viaja junto con el resto de sus datos en agregar()/actualizar() de arriba.
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    //  Validacion interna
    // ---------------------------------------------------------------

    /**
     * Valida los campos minimos de un Profesor.
     *
     * @param p          objeto a validar
     * @param validarRfc si es true, tambien verifica que el RFC no este vacio
     * @return mensaje de error o null si todo esta correcto
     */
    private String validar(Profesor p, boolean validarRfc) {
        if (validarRfc && p.getRfc().isBlank()) return "El RFC es obligatorio.";
        if (p.getNombre().isBlank()) return "El nombre es obligatorio.";
        return null;
    }
    
    
}
