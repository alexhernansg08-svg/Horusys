/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.CategoriaDocenteDAO;
import horarios.modelo.CategoriaDocente;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: CategoriaDocenteController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Regla de negocio: clave y descripcion son obligatorias,
 *  y ambas horas deben ser >= 0 (la tabla ya tiene un CHECK,
 *  pero se valida antes para dar un mensaje claro).
 * ============================================================
 */
public class CategoriaDocenteController {

    private final CategoriaDocenteDAO dao = new CategoriaDocenteDAO();

    /** Retorna todas las categorias ordenadas por descripcion. */
    public List<CategoriaDocente> obtenerTodas() {
        return dao.obtenerTodas();
    }

    public String agregar(String clave, String descripcion, Integer horasFrenteGrupo, Integer horasActividades) {
        String error = validar(clave, descripcion, horasFrenteGrupo, horasActividades);
        if (error != null) return error;

        CategoriaDocente c = new CategoriaDocente(clave.trim(), descripcion.trim(), horasFrenteGrupo, horasActividades);
        return dao.agregar(c) ? null : "Error al agregar la categoría (¿la clave ya existe?).";
    }

    public String actualizar(String clave, String descripcion, Integer horasFrenteGrupo, Integer horasActividades) {
        String error = validar(clave, descripcion, horasFrenteGrupo, horasActividades);
        if (error != null) return error;

        CategoriaDocente c = new CategoriaDocente(clave.trim(), descripcion.trim(), horasFrenteGrupo, horasActividades);
        return dao.actualizar(c) ? null : "Error al actualizar la categoría.";
    }

    public String eliminar(String clave) {
        return dao.eliminar(clave) ? null : "No se pudo eliminar la categoría.";
    }

    private String validar(String clave, String descripcion, Integer horasFrenteGrupo, Integer horasActividades) {
        if (clave == null || clave.isBlank()) return "La clave es obligatoria.";
        if (descripcion == null || descripcion.isBlank()) return "La descripción de la categoría es obligatoria.";
        if (horasFrenteGrupo == null || horasFrenteGrupo < 0) return "Las horas frente a grupo deben ser un número >= 0.";
        if (horasActividades == null || horasActividades < 0) return "Las horas de actividades deben ser un número >= 0.";
        return null;
    }
}
