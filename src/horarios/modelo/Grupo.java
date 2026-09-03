package horarios.modelo;

/**
 * ============================================================
 *  MODELO: Grupo
 *  CAPA:   Modelo (POJO)
 * ============================================================
 */
public class Grupo {

    // --- Campos que mapean con las columnas de la tabla "grupos" ---
    private int     id;                  // PRIMARY KEY
    private String  nombre;              // Ej: "1A"
    private String  codigo;              // Ej: "1A-INF" (unico en BD)
    private int     especialidadId;      // FK -> especialidades.id
    private String  especialidadNombre;  // JOIN: nombre de la especialidad
    private int     semestre;            // 1 a 6
    private String  turno;               // "Matutino" o "Vespertino"
    private int     capacidad;           // max alumnos (default 30)
    private Integer idTutor;             // FK opcional -> profesor.id_profesor (puede ser NULL)
    
    // --- Campo adicional para traer el nombre del Tutor mediante JOIN ---
    private String  tutorNombre;         // JOIN: nombre completo del profesor tutor

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

    public String getEspecialidadNombre()                         { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre)  { this.especialidadNombre = especialidadNombre; }

    public int getSemestre()              { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public String getTurno()              { return turno; }
    public void setTurno(String turno)    { this.turno = turno; }

    public int getCapacidad()                 { return capacidad; }
    public void setCapacidad(int capacidad)   { this.capacidad = capacidad; }

    public Integer getIdTutor()                   { return idTutor; }
    public void setIdTutor(Integer idTutor)       { this.idTutor = idTutor; }

    // GETTER Y SETTER PARA EL TUTOR
    public String getTutorNombre()                        { return tutorNombre; }
    public void setTutorNombre(String tutorNombre)        { this.tutorNombre = tutorNombre; }

    @Override
    public String toString() {
        return nombre != null ? nombre : "";
    }
}