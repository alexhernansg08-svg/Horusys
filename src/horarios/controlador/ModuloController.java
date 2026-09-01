/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;

import horarios.dao.ModuloDAO;
import horarios.modelo.Modulo;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: ModuloController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Regla de negocio: numero de modulo debe ser 1-5, semestre debe
 *  ser 2-6 (los modulos de Competencias laborales solo existen a
 *  partir del 2do semestre), y nombre es obligatorio.
 * ============================================================
 */
public class ModuloController {

    private final ModuloDAO dao = new ModuloDAO();

    public List<Modulo> obtenerTodos() {
        return dao.obtenerTodos();
    }

    public List<Modulo> obtenerPorEspecialidad(int idEspecialidad) {
        return dao.obtenerPorEspecialidad(idEspecialidad);
    }

    public String agregar(Modulo m) {
        String error = validar(m);
        if (error != null) return error;
        return dao.agregar(m) ? null : "Error al agregar el módulo (¿ya existe ese número para esta especialidad?).";
    }

    public String actualizar(Modulo m) {
        String error = validar(m);
        if (error != null) return error;
        return dao.actualizar(m) ? null : "Error al actualizar el módulo.";
    }

    public String eliminar(int idModulo) {
        return dao.eliminar(idModulo) ? null : "No se pudo eliminar el módulo.";
    }

    private String validar(Modulo m) {
        if (m.getNombre() == null || m.getNombre().isBlank()) return "El nombre del módulo es obligatorio.";
        if (m.getNumero() < 1 || m.getNumero() > 5) return "El número de módulo debe estar entre 1 y 5.";
        if (m.getIdSemestre() < 2 || m.getIdSemestre() > 6)
            return "Los módulos de Competencias laborales solo aplican del 2do al 6to semestre.";
        if (m.getIdEspecialidad() <= 0) return "Selecciona una especialidad.";
        return null;
    }
}
