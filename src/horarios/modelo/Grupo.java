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
 *  MODELO: Grupo
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa un grupo escolar del CBTIS 22.
 *  Ejemplo: Grupo "1A-INF", semestre 1, turno Matutino,
 *           especialidad Informatica, capacidad 35 alumnos.
 *
 *  Un Grupo pertenece a una Especialidad (relacion N:1).
 *  El campo "idTutor" es una FK opcional hacia la tabla profesor.
 *
 *  Tabla en BD: grupos (id, nombre, codigo, especialidad_id,
 *                        semestre, turno, capacidad, id_tutor)
 * ============================================================
 */
public class Grupo {

    // --- Campos que mapean con las columnas de la tabla "grupos" ---
    private int     id;                  // PRIMARY KEY
    private String  nombre;              // Ej: "1A"
    private String  codigo;              // Ej: "1A-INF" (unico en BD)
    private int     especialidadId;      // FK -> especialidades.id
    private String  especialidadNombre;  // JOIN: nombre de la especialidad (no esta en BD, se calcula en consulta)
    private int     semestre;            // 1 a 6
    private String  turno;               // "Matutino" o "Vespertino"
    private int     capacidad;           // max alumnos (default 30)
    private Integer idTutor;             // FK opcional -> profesor.id_profesor (puede ser NULL)

    /** Constructor vacio requerido por GrupoDAO al leer ResultSet. */
    public Grupo() {}

    // ---------------------------------------------------------------
    //  Getters y Setters
    // ---------------------------------------------------------------

    public int getId()          { return id; }
    public void setId(int id)   { this.id = id; }

    public String getNombre()              { return nombre; }
    public void setNombre(String nombre)   { this.nombre = nombre; }

    public String getCodigo()              { return codigo; }
    public void setCodigo(String codigo)   { this.codigo = codigo; }

    public int getEspecialidadId()                      { return especialidadId; }
    public void setEspecialidadId(int especialidadId)   { this.especialidadId = especialidadId; }

    // especialidadNombre no viene de "grupos" sino del JOIN con "especialidades"
    public String getEspecialidadNombre()                         { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre)  { this.especialidadNombre = especialidadNombre; }

    public int getSemestre()              { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public String getTurno()              { return turno; }
    public void setTurno(String turno)    { this.turno = turno; }

    public int getCapacidad()                 { return capacidad; }
    public void setCapacidad(int capacidad)   { this.capacidad = capacidad; }

    // Integer (con mayuscula) para permitir null cuando no hay tutor asignado
    public Integer getIdTutor()                   { return idTutor; }
    public void setIdTutor(Integer idTutor)       { this.idTutor = idTutor; }

    /**
     * Representacion del grupo para mostrar en JComboBox o JTable.
     * Retorna el nombre o cadena vacia si aun no tiene nombre asignado.
     */
    @Override
    public String toString() {
        return nombre != null ? nombre : "";
    }
}
