/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.net.URL;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  UTILIDAD: SwingUtils
 * ============================================================
 *  Fabrica de componentes Swing con el estilo institucional del CBTIS 22.
 *  Evita la duplicacion de codigo de estilo en cada ventana.
 *
 *  Que contiene:
 *    - crearBoton / crearBotonGrande: botones con hover effect y cursor de mano
 *    - crearEtiqueta:                 JLabel con fuente Arial Bold en color vino
 *    - crearCampo / crearCampoGrande: JTextField con borde y fuente estandar
 *    - crearCombo:                    JComboBox con estilo institucional
 *    - crearTabla:                    JTable con filas alternas y header vino
 *    - crearPanelBarra:               panel superior con fondo vino y logo
 *    - crearScroll:                   JScrollPane sin borde exterior visible
 *    - mostrarError / mostrarInfo:    dialogos de mensaje estandar
 *
 *  Patron de diseno: Utility Class.
 *    - Constructor privado: no se instancia.
 *    - Metodos estaticos: se llaman como SwingUtils.crearBoton(...)
 *    - Fuentes, colores y bordes como constantes estaticas: no se crean
 *      nuevos objetos Font/Color en cada llamada (mejor rendimiento).
 * ============================================================
 */
public final class SwingUtils {
    private SwingUtils() {}

    // ── Fuentes estaticas (no crear new Font() en cada llamada) ──────────
    private static final Font FONT_BOLD_13  = new Font("Arial", Font.BOLD,  13);
    private static final Font FONT_BOLD_14  = new Font("Arial", Font.BOLD,  14);
    private static final Font FONT_BOLD_20  = new Font("Arial", Font.BOLD,  20);
    private static final Font FONT_PLAIN_13 = new Font("Arial", Font.PLAIN, 13);
    private static final Font FONT_PLAIN_17 = new Font("Arial", Font.PLAIN, 17);

    // ── Colores y bordes estaticos ────────────────────────────────────────
    private static final Color  BORDER_CLR  = new Color(180, 180, 180);
    private static final Border CAMPO_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BORDER_CLR),
        BorderFactory.createEmptyBorder(3, 6, 3, 6));

    // ── Botones ──────────────────────────────────────────────────────────

    /**
     * Crea un boton estilizado con el color institucional dado.
     * Incluye efecto hover (aclara el fondo al pasar el mouse)
     * y cursor de mano para mejor experiencia de usuario.
     *
     * @param texto      texto del boton
     * @param fondo      color de fondo (use Colores.VINO, Colores.VERDE, etc.)
     * @param textoColor color del texto (normalmente Color.WHITE)
     */
    public static JButton crearBoton(String texto, Color fondo, Color textoColor) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_BOLD_13);
        btn.setBackground(fondo);
        btn.setForeground(textoColor);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(170, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(fondo.brighter()); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(fondo); }
        });
        return btn;
    }

    /**
     * Variante grande del boton: fuente mas grande y area mayor (220x70px).
     * Usado en MainWindow para los accesos rapidos de cada modulo.
     */
    public static JButton crearBotonGrande(String texto, Color fondo, Color textoColor) {
        JButton btn = crearBoton(texto, fondo, textoColor);
        btn.setFont(FONT_BOLD_14);
        btn.setPreferredSize(new Dimension(220, 70));
        return btn;
    }

    // ── Campos y etiquetas ───────────────────────────────────────────────

    /**
     * Crea una etiqueta (label) con fuente Arial Bold 13pt en color vino institucional.
     * Usado para los labels de los campos en todos los formularios.
     */
    public static JLabel crearEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(FONT_BOLD_13);
        lbl.setForeground(Colores.VINO);
        return lbl;
    }

    /**
     * Crea un campo de texto con borde gris y fuente Arial 13pt.
     * Borde compuesto: linea exterior + padding interior (3px arriba/abajo, 6px lados).
     */
    public static JTextField crearCampo() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_PLAIN_13);
        tf.setBorder(CAMPO_BORDER);
        return tf;
    }

    public static JTextField crearCampoGrande() {
        JTextField tf = crearCampo();
        tf.setFont(FONT_PLAIN_17);
        tf.setPreferredSize(new Dimension(380, 50));
        return tf;
    }

    public static JPasswordField crearCampoPassword() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_PLAIN_17);
        pf.setPreferredSize(new Dimension(380, 50));
        pf.setBorder(CAMPO_BORDER);
        return pf;
    }

    public static void estilizarCombo(JComboBox<?> combo) {
        combo.setFont(FONT_PLAIN_13);
        combo.setBackground(Color.WHITE);
        combo.setForeground(Colores.VINO);
    }

    // ── Tablas ───────────────────────────────────────────────────────────

    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(38);
        tabla.setFont(FONT_PLAIN_13);
        tabla.setGridColor(new Color(200, 200, 200));
        tabla.setSelectionBackground(Colores.AMARILLO);
        tabla.setSelectionForeground(Colores.VINO);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(col == 0 ? JLabel.CENTER : JLabel.LEFT);
                if (isSelected) {
                    setBackground(Colores.AMARILLO);
                    setForeground(Colores.VINO);
                } else {
                    setBackground(row % 2 == 0 ? Colores.FILA_PAR : Colores.FILA_IMPAR);
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(Colores.VINO);
        header.setForeground(Color.WHITE);
        header.setFont(FONT_BOLD_13);
        header.setPreferredSize(new Dimension(0, 38));
    }

    // ── Header institucional ─────────────────────────────────────────────

    public static JPanel crearHeader(String titulo, int tamanoLogo) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Colores.VINO);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JLabel lblLogo = new JLabel();
        try {
            URL logoURL = SwingUtils.class.getResource("/horarios/cbtis22.jpg");
            if (logoURL == null) logoURL = SwingUtils.class.getResource("/cbtis22.jpg");
            if (logoURL != null) {
                Image img = new ImageIcon(logoURL).getImage()
                        .getScaledInstance(tamanoLogo, tamanoLogo, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}
        lblLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FONT_BOLD_20);
        lblTitulo.setForeground(Colores.AMARILLO);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(Colores.VINO);
        left.add(lblLogo);
        left.add(lblTitulo);
        headerPanel.add(left, BorderLayout.WEST);

        JPanel lineaAmarilla = new JPanel();
        lineaAmarilla.setBackground(Colores.AMARILLO);
        lineaAmarilla.setPreferredSize(new Dimension(1400, 4));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Colores.VINO);
        wrapper.add(headerPanel,   BorderLayout.CENTER);
        wrapper.add(lineaAmarilla, BorderLayout.SOUTH);
        return wrapper;
    }

    // ── Barra de estado inferior ─────────────────────────────────────────

    public static JLabel crearBarraEstado(String mensaje) {
        JLabel lbl = new JLabel(mensaje, JLabel.CENTER);
        lbl.setFont(FONT_BOLD_13);
        lbl.setForeground(Colores.VINO);
        lbl.setBackground(Colores.GRIS_BARRA);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, Colores.AMARILLO),
            BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        return lbl;
    }

    // ── Look and Feel ────────────────────────────────────────────────────

    public static void aplicarLAF() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
    }
}
