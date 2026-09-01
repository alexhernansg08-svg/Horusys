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
 *  MODELO: Especialidad
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa una carrera tecnica del CBTIS 22.
 *  Ejemplos: "Informatica (INF)", "Contabilidad (CONT)".
 *
 *  Esta clase es un POJO (Plain Old Java Object):
 *    - Solo tiene atributos, constructor y getters/setters.
 *    - No contiene logica de negocio ni acceso a BD.
 *    - EspecialidadDAO es quien la lee/escribe en la base de datos.
 *    - EspecialidadController valida sus datos antes de persistirla.
 *
 *  Tabla en BD: especialidades (id, nombre, codigo)
 * ============================================================
 */
public class Especialidad {

    // Atributos que mapean 1:1 con las columnas de la tabla "especialidades"
    private int    id;      // PRIMARY KEY autoincremental
    private String nombre;  // Ej: "Informatica"
    private String codigo;  // Ej: "INF" (se guarda en MAYUSCULAS)

    /** Constructor vacio: requerido por el DAO para crear instancias al leer ResultSet. */
    public Especialidad() {}

    /**
     * Constructor completo: util cuando ya se conocen todos los datos,
     * por ejemplo al crear un objeto para un UPDATE.
     */
    public Especialidad(int id, String nombre, String codigo) {
        this.id     = id;
        this.nombre = nombre;
        this.codigo = codigo;
    }

    // ---------------------------------------------------------------
    //  Getters y Setters (acceso controlado a los atributos privados)
    // ---------------------------------------------------------------

    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }

    public String getNombre()              { return nombre; }
    public void setNombre(String nombre)   { this.nombre = nombre; }

    public String getCodigo()              { return codigo; }
    public void setCodigo(String codigo)   { this.codigo = codigo; }

    /**
     * Representacion legible para mostrar en JComboBox o listas.
     * Ejemplo: "Informatica (INF)"
     */
    @Override
    public String toString() {
        return nombre + " (" + codigo + ")";
    }
}
