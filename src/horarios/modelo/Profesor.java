/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.modelo;
/**
 *
 * @author axelp
 */
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  MODELO: Profesor
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa a un docente del CBTIS 22.
 *
 *  Datos clave para el generador de horarios:
 *    - categoriaHorasFrenteGrupo: maximo de horas de clase por semana (viene de su categoria).
 *    - actividadComplementaria: relleno de horas cuando le sobra tiempo respecto a su categoria.
 *    - disponibilidad: lista de bloques dia+hora en que puede dar clase.
 *  La prioridad de asignacion ya NO es del profesor: la decide HorarioDAO
 *  ordenando las MATERIAS de cada grupo por horas_semanales (mayor primero).
 *
 *  Relaciones en BD:
 *    - Tabla principal:           profesor
 *    - Disponibilidad horaria:    profesor_disponibilidad
 *    - Materias que puede impartir: profesor_materia
 *
 *  Contiene una clase interna estatica "DisponibilidadBloque"
 *  que representa un rango horario en un dia especifico.
 * ============================================================
 */
public class Profesor {

    // --- Datos personales y academicos (columnas de la tabla "profesor") ---
    private int    id;             // id_profesor (PRIMARY KEY autoincremental)
    private String rfc;            // RFC (identificador unico del profesor)
    private String nombre;         // nombre(s)
    private String apellidos;      // apellido(s)
    private String email;          // correo electronico
    private String telefono;       // numero de contacto
    private String pregrado;       // grado academico / titulo (Ej: "Ing. en Sistemas")

    // --- Categoria del docente (columna "profesor.categoria_docente_clave") ---
    // Ej: Tiempo Completo, Tres Cuartos de Tiempo, Medio Tiempo, Por Asignatura.
    // La categoria trae sus propias horas (horas_frente_grupo y horas_actividades_complementarias),
    // por lo que ya no existen horasAcademicas/prioridad/fechaIngreso en el profesor: el maximo
    // de horas de clase ahora lo determina la categoria elegida.
    private String categoriaClave;                       // FK hacia categoria_docente.clave (null = sin categoria)
    private String categoriaDescripcion;                  // solo para mostrar (JOIN, no se persiste directo)
    private Integer categoriaHorasFrenteGrupo;             // JOIN: categoria_docente.horas_frente_grupo
    private Integer categoriaHorasActividadesComplementarias; // JOIN: categoria_docente.horas_actividades_complementarias

    // --- Actividad complementaria (columna "profesor.actividad_complementaria_clave") ---
    // Una sola actividad por profesor (tutorias, asesorias, coordinacion de academia, etc.)
    // que se usa para completar sus horas cuando le sobra tiempo respecto a su categoria.
    private String actividadComplementariaClave;   // FK hacia actividades_complementarias.clave (null = sin actividad)
    private String actividadComplementariaNombre;  // JOIN: actividades_complementarias.actividad
    private Integer actividadComplementariaHoras;  // JOIN: actividades_complementarias.horas

    /**
     * Lista de bloques de disponibilidad horaria.
     * Se carga desde la tabla "profesor_disponibilidad".
     * Ejemplo: [Lunes 07:00-09:00, Martes 10:00-12:00]
     */
    private List<DisponibilidadBloque> disponibilidad = new ArrayList<>();

    /** Constructor vacio requerido por ProfesorDAO. */
    public Profesor() {}

    // ---------------------------------------------------------------
    //  Getters y Setters de datos personales
    // ---------------------------------------------------------------

    public int getId()          { return id; }
    public void setId(int id)   { this.id = id; }

    public String getRfc()              { return rfc; }
    public void setRfc(String rfc)      { this.rfc = rfc; }

    public String getNombre()              { return nombre; }
    public void setNombre(String nombre)   { this.nombre = nombre; }

    public String getApellidos()                  { return apellidos; }
    public void setApellidos(String apellidos)     { this.apellidos = apellidos; }

    public String getEmail()              { return email; }
    public void setEmail(String email)    { this.email = email; }

    public String getTelefono()                 { return telefono; }
    public void setTelefono(String telefono)    { this.telefono = telefono; }

    public String getPregrado()                 { return pregrado; }
    public void setPregrado(String pregrado)    { this.pregrado = pregrado; }

    public String getCategoriaClave()                       { return categoriaClave; }
    public void setCategoriaClave(String categoriaClave)    { this.categoriaClave = categoriaClave; }

    public String getCategoriaDescripcion()                          { return categoriaDescripcion; }
    public void setCategoriaDescripcion(String categoriaDescripcion) { this.categoriaDescripcion = categoriaDescripcion; }

    public Integer getCategoriaHorasFrenteGrupo()                       { return categoriaHorasFrenteGrupo; }
    public void setCategoriaHorasFrenteGrupo(Integer horas)             { this.categoriaHorasFrenteGrupo = horas; }

    public Integer getCategoriaHorasActividadesComplementarias()        { return categoriaHorasActividadesComplementarias; }
    public void setCategoriaHorasActividadesComplementarias(Integer h)  { this.categoriaHorasActividadesComplementarias = h; }

    public String getActividadComplementariaClave()                        { return actividadComplementariaClave; }
    public void setActividadComplementariaClave(String clave)              { this.actividadComplementariaClave = clave; }

    public String getActividadComplementariaNombre()                       { return actividadComplementariaNombre; }
    public void setActividadComplementariaNombre(String nombre)            { this.actividadComplementariaNombre = nombre; }

    public Integer getActividadComplementariaHoras()                       { return actividadComplementariaHoras; }
    public void setActividadComplementariaHoras(Integer horas)             { this.actividadComplementariaHoras = horas; }

    // ---------------------------------------------------------------
    //  Getters y Setters de disponibilidad
    // ---------------------------------------------------------------

    public List<DisponibilidadBloque> getDisponibilidad() { return disponibilidad; }

    /**
     * Asigna la lista de disponibilidad, garantizando que nunca sea null.
     * Si se pasa null, queda como lista vacia en lugar de fallar con NullPointerException.
     */
    public void setDisponibilidad(List<DisponibilidadBloque> disponibilidad) {
        this.disponibilidad = disponibilidad != null ? disponibilidad : new ArrayList<>();
    }

    /**
     * Calcula el total de horas disponibles del profesor sumando
     * la duracion de cada bloque de disponibilidad.
     * Ejemplo: [Lunes 07-09] + [Martes 10-12] = 4 horas totales.
     *
     * @return suma de horas de todos los bloques en la lista
     */
    public int getTotalHorasDisponibles() {
        // Stream: itera la lista, extrae la duracion de cada bloque,
        // y los suma con mapToInt + sum
        return disponibilidad.stream()
                             .mapToInt(DisponibilidadBloque::getDuracionHoras)
                             .sum();
    }

    // ===============================================================
    //  CLASE INTERNA: DisponibilidadBloque
    // ===============================================================

    /**
     * Representa un bloque horario en que el profesor PUEDE dar clase.
     * Ejemplo: dia="Lunes", horaInicio="07:00", horaFin="09:00"
     *
     * Se usa en dos contextos:
     *  1. Para mostrar/editar en la pantalla de ProfesoresWindow.
     *  2. Para que el generador de horarios (HorarioDAO) sepa
     *     en que slots puede asignar a este profesor.
     *
     * Es estatica para poder usarla sin instanciar Profesor primero.
     */
    public static class DisponibilidadBloque {

        private String dia;         // "Lunes", "Martes", ... "Viernes"
        private String horaInicio;  // Formato "HH:00" (ej: "07:00")
        private String horaFin;     // Formato "HH:00" (ej: "09:00")

        /**
         * Constructor unico: todos los campos son obligatorios.
         */
        public DisponibilidadBloque(String dia, String horaInicio, String horaFin) {
            this.dia        = dia;
            this.horaInicio = horaInicio;
            this.horaFin    = horaFin;
        }

        public String getDia()          { return dia; }
        public String getHoraInicio()   { return horaInicio; }
        public String getHoraFin()      { return horaFin; }

        /**
         * Calcula cuantas horas dura este bloque.
         * Ejemplo: inicio="07:00", fin="09:00" -> duracion = 2 horas.
         *
         * Solo toma la parte de horas (antes de ":"), ignora minutos.
         * Si el formato es invalido, retorna 0 para no romper el calculo.
         *
         * @return numero de horas enteras del bloque
         */
        public int getDuracionHoras() {
            try {
                int inicio = Integer.parseInt(horaInicio.split(":")[0]);
                int fin    = Integer.parseInt(horaFin.split(":")[0]);
                return fin - inicio; // ej: 9 - 7 = 2
            } catch (Exception e) {
                return 0; // dato malformado: no contribuye al total
            }
        }
    }
}
