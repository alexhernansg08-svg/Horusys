/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.modelo;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  MODELO: ActividadComplementaria
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa una actividad con la que un docente puede completar
 *  sus horas frente a grupo (para que no le queden horas vacias).
 *  Ejemplos: "Tutorías" (Academica, 2h), "Junta de academia" (Institucional, 2h).
 *
 *  Tabla en BD: actividades_complementarias
 *  (clave PK varchar, tipo_actividad, actividad, horas)
 *  -- Esta tabla YA EXISTIA en la base de datos con 15 registros
 *  precargados; el modelo se adapto a su estructura real.
 *
 *  Un Profesor tiene A LO MAS UNA actividad complementaria
 *  (columna directa profesor.actividad_complementaria_clave,
 *  que referencia esta tabla por "clave" — no es una relacion N:M).
 * ============================================================
 */
public class ActividadComplementaria {

    private String clave;          // PRIMARY KEY (Ej: "1", "2"...)
    private String tipoActividad;  // Ej: "Academica", "Institucional", "Extraescolar"...
    private String actividad;      // Ej: "Tutorías", "Junta de academia"
    private int    horas;          // horas que cubre esta actividad (CHECK horas > 0 en BD)

    /** Constructor vacio: requerido por el DAO para crear instancias al leer ResultSet. */
    public ActividadComplementaria() {}

    public ActividadComplementaria(String clave, String tipoActividad, String actividad, int horas) {
        this.clave         = clave;
        this.tipoActividad = tipoActividad;
        this.actividad     = actividad;
        this.horas         = horas;
    }

    public String getClave()               { return clave; }
    public void setClave(String clave)     { this.clave = clave; }

    public String getTipoActividad()                     { return tipoActividad; }
    public void setTipoActividad(String tipoActividad)   { this.tipoActividad = tipoActividad; }

    public String getActividad()                 { return actividad; }
    public void setActividad(String actividad)   { this.actividad = actividad; }

    public int getHoras()             { return horas; }
    public void setHoras(int horas)   { this.horas = horas; }

    /**
     * Representacion legible para mostrar en checkboxes o listas.
     * Ej: "Tutorías (Academica) — 2 h"
     */
    @Override
    public String toString() {
        return actividad + " (" + tipoActividad + ") — " + horas + " h";
    }
}
