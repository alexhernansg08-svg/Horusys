/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.util;

import java.awt.Color;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  UTILIDAD: Colores
 * ============================================================
 *  Paleta de colores institucional DGETI / CBTIS 22.
 *
 *  Centraliza TODOS los colores de la UI en un solo lugar.
 *  Beneficio: si hay que cambiar un color, se modifica aqui
 *  y se refleja automaticamente en todas las ventanas.
 *
 *  Patron de diseno: Utility Class de constantes.
 *    - Constructor privado: no se instancia.
 *    - Constantes publicas estaticas: acceso directo con Colores.VINO
 *
 *  Los colores siguen la identidad visual del CBTIS:
 *    - Vino/guinda: color principal de la institucion
 *    - Amarillo/dorado: color secundario institucional
 *    - Grises: fondos y barras neutras
 *    - Verde/Rojo: estados de exito/error
 * ============================================================
 */
public final class Colores {

    /** Constructor privado: esta clase solo tiene constantes, no se instancia. */
    private Colores() {}

    // ---- Colores institucionales principales ----

    /** Vino institucional: usado en headers, barras de titulo y botones principales. */
    public static final Color VINO        = new Color(100, 0, 25);

    /** Vino oscuro: usado en hover/focus de elementos vino para dar profundidad. */
    public static final Color VINO_OSCURO = new Color(70,  0, 15);

    /** Amarillo/dorado institucional: textos sobre fondos vino, acentos. */
    public static final Color AMARILLO    = new Color(204, 160, 0);

    // ---- Colores de fondo y estructura ----

    /** Gris claro: fondo general de paneles y ventanas. */
    public static final Color GRIS        = new Color(245, 245, 245);

    /** Gris de barra: para barras de herramientas y separadores. */
    public static final Color GRIS_BARRA  = new Color(235, 235, 235);

    // ---- Colores de estado ----

    /** Verde: mensajes de exito, operaciones correctas, iconos de confirmacion. */
    public static final Color VERDE       = new Color(0,   120, 0);

    /** Rojo: mensajes de error, validaciones fallidas, alertas criticas. */
    public static final Color ROJO        = new Color(160,   0, 0);

    // ---- Colores para filas alternas en JTable ----

    /** Filas pares de tablas: blanco puro. */
    public static final Color FILA_PAR    = Color.WHITE;

    /** Filas impares de tablas: crema muy suave para facilitar la lectura. */
    public static final Color FILA_IMPAR  = new Color(250, 245, 240);
}
