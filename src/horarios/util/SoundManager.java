/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.util;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  UTILIDAD: SoundManager
 * ============================================================
 *  Reproduccion de sonidos de retroalimentacion para el usuario.
 *
 *  Dos sonidos disponibles:
 *    - sound_success.wav: operacion exitosa (guardar, agregar, etc.)
 *    - sound_error.wav:   operacion rechazada o con error
 *
 *  Caracteristica clave: reproduccion ASINCRONA.
 *    La reproduccion ocurre en un hilo separado (Thread) para que
 *    la interfaz grafica no se congele mientras se reproduce el audio.
 *    Si el audio falla (ej: sin tarjeta de sonido), la excepcion se
 *    captura silenciosamente y la app sigue funcionando normal.
 *
 *  Los archivos .wav estan dentro del JAR como recursos del classpath.
 * ============================================================
 */
public class SoundManager {

    /** Ruta del recurso de sonido de exito dentro del classpath/JAR. */
    private static final String SUCCESS_SOUND = "/horarios/sound_success.wav";

    /** Ruta del recurso de sonido de error dentro del classpath/JAR. */
    private static final String ERROR_SOUND   = "/horarios/sound_error.wav";

    /**
     * Reproduce el sonido de exito de forma asincrona.
     * Llamar desde la vista cuando una operacion CRUD tiene exito.
     */
    public static void playSuccess() {
        play(SUCCESS_SOUND);
    }

    /**
     * Reproduce el sonido de error de forma asincrona.
     * Llamar desde la vista cuando una operacion falla o una validacion rechaza.
     */
    public static void playError() {
        play(ERROR_SOUND);
    }

    /**
     * Metodo interno que carga y reproduce un archivo WAV en un hilo separado.
     *
     * Flujo:
     *   1. Abrir el archivo WAV como stream del classpath.
     *   2. Decodificarlo con AudioSystem.
     *   3. Reproducirlo con un Clip de Java Sound.
     *   4. Esperar a que termine (duracion + 200ms de margen).
     *   5. Cerrar el clip para liberar recursos de audio.
     *
     * Todo esto ocurre en un Thread separado llamado "SoundThread"
     * para no bloquear el Event Dispatch Thread (hilo de la UI).
     *
     * @param resourcePath ruta del .wav en el classpath
     */
    private static void play(String resourcePath) {
        new Thread(() -> {
            try {
                // Obtener el archivo WAV como stream del classpath (dentro del JAR)
                InputStream raw = SoundManager.class.getResourceAsStream(resourcePath);
                if (raw == null) return; // archivo no encontrado: nada que reproducir

                // BufferedInputStream mejora el rendimiento de lectura del stream
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(raw))) {

                    // Clip: representacion de un audio corto que se puede reproducir
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais); // cargar el audio en memoria
                    clip.start();   // reproducir (no bloqueante)

                    // Esperar a que termine la reproduccion antes de cerrar el clip
                    // getMicrosecondLength() da la duracion en microsegundos -> /1000 = ms
                    // +200ms de margen para evitar cortar el audio antes de tiempo
                    Thread.sleep(clip.getMicrosecondLength() / 1000 + 200);

                    clip.close(); // liberar recursos de audio del sistema
                }
            } catch (Exception ex) {
                // Captura silenciosa: si no hay audio disponible en el sistema,
                // la excepcion se ignora y la app sigue funcionando normalmente.
                // No se loguea para no llenar la consola en entornos sin audio.
            }
        }, "SoundThread").start(); // nombre del hilo para facilitar el debug
    }
}
