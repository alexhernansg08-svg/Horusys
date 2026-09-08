/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios;

import horarios.vista.LoginWindow;
import javax.swing.*;
import java.util.logging.Logger;

/**
 *
 * @author axelp
 */
/**AAAc
 * ============================================================
 *  CLASE PRINCIPAL - Punto de entrada de la aplicacion
 * ============================================================
 *  Es el "arranque" del sistema. Su unica responsabilidad es:
 *    1. Aplicar el tema visual (Look & Feel) de Swing.
 *    2. Lanzar la ventana de login en el hilo correcto de la UI.
 *
 *  Arquitectura general del proyecto:
 *    vista/       -> Pantallas Swing que el usuario ve (JFrames)
 *    controlador/ -> Logica de validacion entre vista y DAO
 *    dao/         -> Acceso a la base de datos PostgreSQL
 *    modelo/      -> Clases POJO (Especialidad, Grupo, Materia, Profesor)
 *    util/        -> Herramientas reutilizables (colores, sonidos, Excel)
 * ============================================================
 */
public class Main {

    // Logger de Java estandar: registra mensajes internos de esta clase
    // en el sistema de logs de la JVM (no en consola directamente)
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    /**
     * Metodo main: punto de entrada de la JVM.
     * @param args argumentos de linea de comandos (no se usan)
     */
    public static void main(String[] args) {
        // Paso 1: configurar el aspecto visual antes de crear cualquier ventana
        aplicarLookAndFeel();

        // Paso 2: lanzar la ventana de Login.
        //   SwingUtilities.invokeLater garantiza que la UI se construya
        //   en el "Event Dispatch Thread" (EDT), el unico hilo seguro
        //   para crear y manipular componentes Swing. Si se crea fuera
        //   del EDT pueden aparecer bugs visuales o condiciones de carrera.
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }

    /**
     * Intenta aplicar el Look & Feel nativo del sistema operativo.
     * Si no esta disponible, usa Nimbus (tema moderno de Java).
     * Esto hace que ventanas, botones y campos se vean igual que
     * otras aplicaciones del SO del usuario.
     */
    private static void aplicarLookAndFeel() {
        // Candidatos en orden de preferencia:
        //   [0] Look nativo del SO (Windows/Mac/Linux)
        //   [1] Nimbus: tema moderno como respaldo
        String[] candidatos = {
            UIManager.getSystemLookAndFeelClassName(),
            "javax.swing.plaf.nimbus.NimbusLookAndFeel"
        };

        for (String laf : candidatos) {
            try {
                UIManager.setLookAndFeel(laf); // intenta aplicar el tema
                return;                         // exito: no seguir probando
            } catch (Exception e) {
                // Tema no disponible en este entorno: LOG lo registra
                // en nivel FINE (baja prioridad) y el bucle sigue
                LOG.fine("LAF no disponible: " + laf);
            }
        }
        // Si todos los candidatos fallan, Swing usa Metal (tema por defecto)
        //  ACTUALIZACION MODELOS DE GRUPOS
        //casi terminaom
    }
    
}
