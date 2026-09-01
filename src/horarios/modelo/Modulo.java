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
 *  MODELO: Modulo
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa un Modulo de Competencias laborales del plan de
 *  estudios de una especialidad (Modulo I a V).
 *
 *  Aplica UNICAMENTE al area de "Competencias laborales" de la
 *  reticula (no a Lengua y comunicacion, Pensamiento matematico, etc).
 *  Cada especialidad tiene sus propios 5 modulos, uno por semestre
 *  del 2do al 6to (Modulo I=2do ... Modulo V=6to).
 *
 *  Los Submodulos (las materias reales que agenda el generador)
 *  son filas normales de la tabla "materias" que apuntan a este
 *  modulo via materias.id_modulo.
 *
 *  Tabla en BD: modulos (id_modulo, id_especialidad, id_semestre,
 *                         numero, nombre, clave)
 * ============================================================
 */
public class Modulo {

    private int    idModulo;           // PRIMARY KEY
    private int    idEspecialidad;     // FK -> especialidades.id
    private String especialidadNombre; // JOIN, solo para mostrar
    private int    idSemestre;         // semestre en que se cursa (2 a 6)
    private int    numero;             // 1 a 5 (Modulo I..V)
    private String nombre;             // Ej: "Desarrolla software de sistemas informaticos"
    private String clave;              // opcional

    public Modulo() {}

    public Modulo(int idEspecialidad, int idSemestre, int numero, String nombre, String clave) {
        this.idEspecialidad = idEspecialidad;
        this.idSemestre = idSemestre;
        this.numero = numero;
        this.nombre = nombre;
        this.clave = clave;
    }

    public int getIdModulo()                { return idModulo; }
    public void setIdModulo(int idModulo)   { this.idModulo = idModulo; }

    public int getIdEspecialidad()                     { return idEspecialidad; }
    public void setIdEspecialidad(int idEspecialidad)  { this.idEspecialidad = idEspecialidad; }

    public String getEspecialidadNombre()                         { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre)  { this.especialidadNombre = especialidadNombre; }

    public int getIdSemestre()                { return idSemestre; }
    public void setIdSemestre(int idSemestre) { this.idSemestre = idSemestre; }

    public int getNumero()             { return numero; }
    public void setNumero(int numero)  { this.numero = numero; }

    public String getNombre()             { return nombre; }
    public void setNombre(String nombre)  { this.nombre = nombre; }

    public String getClave()            { return clave; }
    public void setClave(String clave)  { this.clave = clave; }

    /** Ej: "Módulo I — Desarrolla software de sistemas informáticos". Si es el
     *  centinela "sin módulo" (idModulo=0), devuelve solo el nombre tal cual. */
    public String getEtiquetaCompleta() {
        if (idModulo == 0) return nombre;
        String[] romanos = {"", "I", "II", "III", "IV", "V"};
        String num = (numero >= 1 && numero <= 5) ? romanos[numero] : String.valueOf(numero);
        return "Módulo " + num + " — " + nombre;
    }

    @Override
    public String toString() {
        return getEtiquetaCompleta();
    }
}
