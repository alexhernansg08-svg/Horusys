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
 *  MODELO: CategoriaDocente
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa la categoria laboral de un docente.
 *  Ejemplos: "Tiempo Completo", "Tres Cuartos de Tiempo",
 *            "Medio Tiempo", "Por Asignatura".
 *
 *  Tabla en BD: categoria_docente (clave, descripcion,
 *               horas_frente_grupo, horas_actividades_complementarias)
 *
 *  La PK es "clave" (tipo clave presupuestal SEP, ej. "E4853"),
 *  no un id autoincremental: puede haber varias filas con la misma
 *  "descripcion" (varios profesores de Tiempo Completo, cada uno
 *  con su propia clave), pero comparten las mismas horas.
 *
 *  Un Profesor tiene una sola categoria (columna
 *  profesor.categoria_docente_clave -> categoria_docente.clave).
 * ============================================================
 */
public class CategoriaDocente {

    private String clave;                          // PRIMARY KEY (ej. "E4853")
    private String descripcion;                    // Ej: "Tiempo Completo"
    private int    horasFrenteGrupo;                // max horas de clase por semana
    private int    horasActividadesComplementarias; // tope de horas para actividades (tutorias, etc.)

    /** Constructor vacio: requerido por el DAO para crear instancias al leer ResultSet. */
    public CategoriaDocente() {}

    public CategoriaDocente(String clave, String descripcion,
                             int horasFrenteGrupo, int horasActividadesComplementarias) {
        this.clave = clave;
        this.descripcion = descripcion;
        this.horasFrenteGrupo = horasFrenteGrupo;
        this.horasActividadesComplementarias = horasActividadesComplementarias;
    }
    
    public CategoriaDocente(int id, String descripcion) {
    this.clave = String.valueOf(id);
    this.descripcion = descripcion;
    this.horasFrenteGrupo = 0;
    this.horasActividadesComplementarias = 0;
}
    
    

    public String getClave()               { return clave; }
    public void setClave(String clave)     { this.clave = clave; }

    public String getDescripcion()                  { return descripcion; }
    public void setDescripcion(String descripcion)  { this.descripcion = descripcion; }

    public int getHorasFrenteGrupo()                       { return horasFrenteGrupo; }
    public void setHorasFrenteGrupo(int horasFrenteGrupo)  { this.horasFrenteGrupo = horasFrenteGrupo; }

    public int getHorasActividadesComplementarias()                      { return horasActividadesComplementarias; }
    public void setHorasActividadesComplementarias(int horas)            { this.horasActividadesComplementarias = horas; }

    /**
     * Representacion legible para mostrar en JComboBox o listas.
     * Incluye la clave para diferenciar filas con la misma descripcion.
     */
    @Override
    public String toString() {
        return descripcion + " (" + clave + ")";
    }
}
