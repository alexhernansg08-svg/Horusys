/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.ActividadComplementariaDAO;
import horarios.modelo.ActividadComplementaria;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: ActividadComplementariaController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Reglas de negocio:
 *    - clave, tipo de actividad y nombre de actividad son obligatorios.
 *    - horas debe ser mayor a 0 (la tabla ya tiene un CHECK, pero se
 *      valida antes para dar un mensaje claro en vez de un error SQL).
 * ============================================================
 */
public class ActividadComplementariaController {

    private final ActividadComplementariaDAO dao = new ActividadComplementariaDAO();

    /** Retorna todas las actividades complementarias, agrupadas por tipo. */
    public List<ActividadComplementaria> obtenerTodas() {
        return dao.obtenerTodas();
    }

    public String agregar(String clave, String tipoActividad, String actividad, Integer horas) {
        String error = validar(clave, tipoActividad, actividad, horas);
        if (error != null) return error;

        ActividadComplementaria a = new ActividadComplementaria(clave.trim(), tipoActividad.trim(), actividad.trim(), horas);
        return dao.agregar(a) ? null : "Error al agregar la actividad (¿la clave ya existe?).";
    }

    public String actualizar(String clave, String tipoActividad, String actividad, Integer horas) {
        String error = validar(clave, tipoActividad, actividad, horas);
        if (error != null) return error;

        ActividadComplementaria a = new ActividadComplementaria(clave.trim(), tipoActividad.trim(), actividad.trim(), horas);
        return dao.actualizar(a) ? null : "Error al actualizar la actividad.";
    }

    public String eliminar(String clave) {
        return dao.eliminar(clave) ? null : "No se pudo eliminar la actividad.";
    }

    private String validar(String clave, String tipoActividad, String actividad, Integer horas) {
        if (clave == null || clave.isBlank()) return "La clave es obligatoria.";
        if (tipoActividad == null || tipoActividad.isBlank()) return "El tipo de actividad es obligatorio.";
        if (actividad == null || actividad.isBlank()) return "El nombre de la actividad es obligatorio.";
        if (horas == null || horas <= 0) return "Las horas deben ser un número mayor a 0.";
        return null;
    }
}
