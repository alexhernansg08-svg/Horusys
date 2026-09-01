/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.HorarioDAO;
import horarios.dao.HorarioConsultaDAO;
import java.util.List;
import java.util.Map;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: HorarioController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Intermediario entre HorariosWindow (vista) y los DAOs de horario.
 *
 *  Usa dos DAOs con responsabilidad separada:
 *    - HorarioDAO:         genera el horario (escribe en BD)
 *    - HorarioConsultaDAO: lee y valida el horario generado
 *
 *  La vista solo conoce este controlador; nunca toca los DAOs
 *  directamente.
 * ============================================================
 */
public class HorarioController {

    private final HorarioDAO         generador = new HorarioDAO();
    private final HorarioConsultaDAO consulta  = new HorarioConsultaDAO();

    // ---------------------------------------------------------------
    //  Generacion
    // ---------------------------------------------------------------

    /**
     * Ejecuta la generacion completa del horario.
     * El algoritmo pre-carga todos los datos en memoria y asigna
     * profesores a materias/grupos usando un enfoque greedy (bin-packing).
     *
     * @param usuarioId ID del usuario (director) que disparo la generacion
     * @return true si el horario se genero y persistio correctamente
     */
    public boolean generarHorario(int usuarioId) {
        return generador.generarHorario(usuarioId);
    }

    // ---------------------------------------------------------------
    //  Validacion
    // ---------------------------------------------------------------

    /**
     * Valida el horario generado y retorna la lista de problemas encontrados.
     * Revisa choques de grupo, choques de profesor, grupos sin clases,
     * y materias con horas incompletas.
     *
     * @return lista de strings con problemas, o ["SIN PROBLEMAS..."] si todo esta bien
     */
    public List<String> validarDetallado() {
        return consulta.validarHorarioDetallado();
    }

    /**
     * Version simplificada: true si el horario no tiene choques ni problemas.
     */
    public boolean validarSinChoques() {
        return consulta.validarHorarioSinChoques();
    }

    // ---------------------------------------------------------------
    //  Consultas (para poblar HorariosWindow)
    // ---------------------------------------------------------------

    /** Retorna el horario completo (todas las horas x todos los dias). */
    public List<Map<String, Object>> obtenerHorarioGeneral() {
        return consulta.obtenerHorarioGeneral();
    }

    /**
     * Retorna el horario de un grupo en formato pivote (hora x dias).
     * @param grupo codigo del grupo (Ej: "1A-INF")
     */
    public List<Map<String, Object>> obtenerHorarioPorGrupo(String grupo) {
        return consulta.obtenerHorarioPorGrupo(grupo);
    }

    /**
     * Retorna el horario de un profesor en formato pivote (hora x dias).
     * @param rfc RFC del profesor
     */
    public List<Map<String, Object>> obtenerHorarioPorProfesor(String rfc) {
        return consulta.obtenerHorarioPorProfesor(rfc);
    }

    /**
     * Busca el horario de un profesor por coincidencia parcial de nombre.
     * @param nombreProfesor termino de busqueda parcial
     */
    public List<Map<String, Object>> buscarHorarioPorNombreProfesor(String nombreProfesor) {
        return consulta.buscarHorarioPorNombreProfesor(nombreProfesor);
    }

    /** Retorna los codigos de grupos con al menos una clase asignada. */
    public List<String> obtenerCodigosGrupos() {
        return consulta.obtenerCodigosGrupos();
    }

    /** Retorna pares [rfc, nombreCompleto] de todos los profesores con clases. */
    public List<String[]> obtenerProfesoresConNombre() {
        return consulta.obtenerProfesoresConNombre();
    }
}
