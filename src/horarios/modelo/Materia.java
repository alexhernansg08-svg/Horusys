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
 *  MODELO: Materia
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa una materia o asignatura del plan de estudios.
 *  Ejemplo: clave="INF-101", nombre="Algoritmos",
 *           especialidad=Informatica, semestre=1, horas=5.
 *
 *  Una Materia pertenece a una Especialidad y a un Semestre.
 *  Los Profesores pueden tener N materias asignadas (tabla profesor_materia).
 *  El Generador de Horarios usa horas_semanales para saber cuantos
 *  bloques de 1 hora debe programar por semana para esta materia.
 *
 *  Tabla en BD: materias (id_materia, clave, nombre, id_especialidad,
 *                          id_semestre, horas_semanales)
 * ============================================================
 */
public class Materia {

    // --- Campos que mapean con la tabla "materias" ---
    private int    idMateria;           // PRIMARY KEY
    private String clave;               // Ej: "INF-101" (debe ser unica)
    private String nombre;              // Ej: "Algoritmos y Programacion"
    private int    idEspecialidad;      // FK -> especialidades.id
    private String especialidadNombre;  // JOIN: no esta en BD, viene del LEFT JOIN
    private int    idSemestre;          // 1 a 6
    private int    horasSemanales;      // cuantas horas por semana tiene esta materia

    // --- Modulo de Competencias laborales (columna "materias.id_modulo") ---
    // Solo aplica a materias del area de Competencias laborales (submodulos);
    // el resto de la reticula (Lengua y comunicacion, Pensamiento matematico, etc.)
    // deja este campo en null.
    private Integer idModulo;           // FK -> modulos.id_modulo (null = sin modulo)
    private String  moduloEtiqueta;     // JOIN: "Módulo I — Desarrolla software..." (solo para mostrar)

    // ---------------------------------------------------------------
    //  Getters y Setters
    // ---------------------------------------------------------------

    public int getIdMateria()                 { return idMateria; }
    public void setIdMateria(int idMateria)   { this.idMateria = idMateria; }

    public String getClave()              { return clave; }
    public void setClave(String clave)    { this.clave = clave; }

    public String getNombre()              { return nombre; }
    public void setNombre(String nombre)   { this.nombre = nombre; }

    public int getIdEspecialidad()                      { return idEspecialidad; }
    public void setIdEspecialidad(int idEspecialidad)   { this.idEspecialidad = idEspecialidad; }

    // No viene de la tabla materias; se llena con el JOIN en MateriaDAO.SQL_SELECT_ALL
    public String getEspecialidadNombre()                         { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre)  { this.especialidadNombre = especialidadNombre; }

    public int getIdSemestre()                  { return idSemestre; }
    public void setIdSemestre(int idSemestre)   { this.idSemestre = idSemestre; }

    public int getHorasSemanales()                    { return horasSemanales; }
    public void setHorasSemanales(int horasSemanales) { this.horasSemanales = horasSemanales; }

    public Integer getIdModulo()                { return idModulo; }
    public void setIdModulo(Integer idModulo)   { this.idModulo = idModulo; }

    // No viene de la tabla materias; se llena con el JOIN a modulos en MateriaDAO.SQL_SELECT_ALL
    public String getModuloEtiqueta()                     { return moduloEtiqueta; }
    public void setModuloEtiqueta(String moduloEtiqueta)  { this.moduloEtiqueta = moduloEtiqueta; }
}
