package horarios.vista;

import horarios.controlador.LoginController;
import horarios.util.SoundManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * ============================================================
 *  VISTA: LoginWindow
 *  CAPA:  Vista (MVC)
 * ============================================================
 *  Primera ventana del sistema: permite al director
 *  autenticarse o ir al registro de nueva cuenta.
 *
 *  Flujo de uso:
 *    1. Usuario escribe username + password y presiona "Iniciar sesion"
 *       (o tecla Enter desde cualquier campo).
 *    2. Se llama a LoginController.iniciarSesion() para validar.
 *    3. Si es correcto -> abre MainWindow y cierra esta ventana.
 *    4. Si es incorrecto -> muestra mensaje de error.
 *    5. El boton "Registrarse" abre RegisterWindow.
 *
 *  Usa LoginController como intermediario; jamas llama
 *  directamente al DAO ni a la base de datos.
 * ============================================================
 */
public class LoginWindow extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginWindow.class.getName());

    // ── PALETA INSTITUCIONAL DE COLORES ──────────────────────────────────────
    private static final Color COLOR_VINO   = new Color(100, 0, 25);
    private static final Color COLOR_DORADO = new Color(204, 160, 0);
    private static final Color COLOR_VERDE  = new Color(40, 140, 60);
    private static final Color COLOR_ROJO   = new Color(180, 40, 40);
    private static final Color COLOR_FONDO  = new Color(245, 245, 247);

    // ── INSTANCIA DEL CONTROLADOR ────────────────────────────────────────────
    private final LoginController controller = new LoginController();

    /**
     * Constructor principal de la ventana
     */
    public LoginWindow() {
        initComponents(); // Método auto-generado por NetBeans
        initCustomUI();   // Reestructuración responsiva e imagen cbtis22.jpg
    }

    // =========================================================================
    // CONFIGURACIÓN, RESPONSIVIDAD Y LOGO (PROGRAMADO ARRIBA DE initComponents)
    // =========================================================================

    /**
     * Inicialización dinámica de estilos, colores, imagen responsiva y layout fluido.
     */
    private void initCustomUI() {
        // 1. Pantalla completa al iniciar
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);

        // 2. Cargar imagen del escudo cbtis22.jpg
       ImageIcon logoEscudo = cargarEscudo("cbtis22.jpg", 65, 75);
        if (logoEscudo != null) {
            jLabel1.setIcon(logoEscudo);
        } else {
            jLabel1.setIcon(new EscudoInstitucionalIcon(50, 60)); // Respaldo si no encuentra la imagen
        }
        jLabel1.setIconTextGap(15);

        // 3. Iconos de campos y botones
        jLabel3.setIcon(new PersonaIcon(18, 18));
        jLabel3.setIconTextGap(8);

        jLabel4.setIcon(new CandadoIcon(18, 18));
        jLabel4.setIconTextGap(8);

        jLabelBtnIniciarSesion.setIcon(new PalomitaIcon(18, 18));
        jLabelBtnIniciarSesion.setIconTextGap(10);

        jLabelBtnRegistrarse.setIcon(new RegistroIcon(18, 18));
        jLabelBtnRegistrarse.setIconTextGap(10);

        jLabelBtnSalir.setIcon(new SalirIcon(18, 18));
        jLabelBtnSalir.setIconTextGap(10);

        // Limpiar campos
        FieldUsuario.setText("");
        FieldContraseña.setText("");

        // Colores de componentes
        Bg.setBackground(COLOR_FONDO);
        jPanel2.setBackground(COLOR_VINO);
        jPanel3.setBackground(COLOR_DORADO);
        jPanel4.setBackground(COLOR_VERDE);
        jPanel5.setBackground(COLOR_DORADO);
        jPanel6.setBackground(COLOR_ROJO);

        // ---------------------------------------------------------------------
        // REORGANIZACIÓN RESPONSIVA (Sustituye la disposición fija de NetBeans)
        // ---------------------------------------------------------------------
        Bg.removeAll();
        Bg.setLayout(new BorderLayout());

        // A) ENCABEZADO SUPERIOR (Abarca todo el ancho)
        jPanel2.removeAll();
        jPanel2.setLayout(new BorderLayout());

        JPanel topBarX = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        topBarX.setOpaque(false);
        topBarX.add(jLabelBtnCerrarVentana);

        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        centerHeader.setOpaque(false);
        jLabel1.setForeground(Color.WHITE);
        jLabel1.setFont(new Font("Arial", Font.BOLD, 26));
        centerHeader.add(jLabel1);

        jPanel3.setPreferredSize(new Dimension(0, 5));

        jPanel2.add(topBarX, BorderLayout.NORTH);
        jPanel2.add(centerHeader, BorderLayout.CENTER);
        jPanel2.add(jPanel3, BorderLayout.SOUTH);

        Bg.add(jPanel2, BorderLayout.NORTH);

        // B) TARJETA CENTRADA RESPONSIVA (Se adapta al centro de cualquier pantalla)
        JPanel centerPanelContainer = new JPanel(new GridBagLayout());
        centerPanelContainer.setOpaque(false);

        JPanel cardLogin = new JPanel();
        cardLogin.setLayout(new BoxLayout(cardLogin, BoxLayout.Y_AXIS));
        cardLogin.setBackground(Color.WHITE);
        cardLogin.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_DORADO, 2, true),
                new EmptyBorder(35, 45, 35, 45)
        ));

        // Dimensiones fluidas para inputs
        Dimension sizeFields = new Dimension(360, 38);
        FieldUsuario.setMaximumSize(sizeFields);
        FieldUsuario.setPreferredSize(sizeFields);
        FieldUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        FieldContraseña.setMaximumSize(sizeFields);
        FieldContraseña.setPreferredSize(sizeFields);
        FieldContraseña.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        jLabel3.setFont(new Font("Arial", Font.BOLD, 14));
        jLabel3.setForeground(COLOR_VINO);
        jLabel4.setFont(new Font("Arial", Font.BOLD, 14));
        jLabel4.setForeground(COLOR_VINO);

        // Alineaciones
        jLabel3.setAlignmentX(Component.LEFT_ALIGNMENT);
        FieldUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        jLabel4.setAlignmentX(Component.LEFT_ALIGNMENT);
        FieldContraseña.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardLogin.add(jLabel3);
        cardLogin.add(Box.createVerticalStrut(8));
        cardLogin.add(FieldUsuario);
        cardLogin.add(Box.createVerticalStrut(20));
        cardLogin.add(jLabel4);
        cardLogin.add(Box.createVerticalStrut(8));
        cardLogin.add(FieldContraseña);
        cardLogin.add(Box.createVerticalStrut(30));

        // Dimensiones fluidas para botones (jPanel4, jPanel5, jPanel6)
        Dimension sizeBtns = new Dimension(360, 42);

        jPanel4.setLayout(new BorderLayout());
        jPanel4.setMaximumSize(sizeBtns);
        jPanel4.setPreferredSize(sizeBtns);
        jPanel4.setAlignmentX(Component.LEFT_ALIGNMENT);
        jLabelBtnIniciarSesion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        jLabelBtnIniciarSesion.setForeground(Color.WHITE);

        jPanel5.setLayout(new BorderLayout());
        jPanel5.setMaximumSize(sizeBtns);
        jPanel5.setPreferredSize(sizeBtns);
        jPanel5.setAlignmentX(Component.LEFT_ALIGNMENT);
        jLabelBtnRegistrarse.setFont(new Font("Segoe UI", Font.BOLD, 13));
        jLabelBtnRegistrarse.setForeground(Color.BLACK);

        jPanel6.setLayout(new BorderLayout());
        jPanel6.setMaximumSize(sizeBtns);
        jPanel6.setPreferredSize(sizeBtns);
        jPanel6.setAlignmentX(Component.LEFT_ALIGNMENT);
        jLabelBtnSalir.setFont(new Font("Segoe UI", Font.BOLD, 13));
        jLabelBtnSalir.setForeground(Color.WHITE);

        cardLogin.add(jPanel4);
        cardLogin.add(Box.createVerticalStrut(12));
        cardLogin.add(jPanel5);
        cardLogin.add(Box.createVerticalStrut(12));
        cardLogin.add(jPanel6);

        centerPanelContainer.add(cardLogin, new GridBagConstraints());
        Bg.add(centerPanelContainer, BorderLayout.CENTER);

        Bg.revalidate();
        Bg.repaint();

        // 4. Asignar eventos
        initCustomListeners();
    }

    /**
     * Buscador de la imagen cbtis22.jpg en múltiples rutas posibles del proyecto
     */
    /**
     * Carga y escala automáticamente el archivo cbtis22.jpg buscando dentro del
     * paquete 'horarios.vista' y en las rutas del proyecto Horusys.
     */
    /**
     * Carga y escala cbtis22.jpg imprimiendo diagnósticos en la consola de NetBeans.
     */
    private ImageIcon cargarEscudo(String cbtis22jpg, int ancho, int alto) {
        String base = "cbtis22";
        String[] extensiones = {".jpg", ".JPG", ".jpeg", ".png", ".PNG"};

        System.out.println(">>> [DEBUG LOGO] Buscando imagen del escudo...");
        System.out.println(">>> [DEBUG LOGO] Directorio de ejecución: " + System.getProperty("user.dir"));

        // 1. Búsqueda por Classpath (Dentro de src/ y paquetes)
        for (String ext : extensiones) {
            String nombre = base + ext;
            String[] rutasResource = {
                "/horarios/vista/" + nombre,
                "/" + nombre,
                nombre
            };

            for (String res : rutasResource) {
                java.net.URL url = getClass().getResource(res);
                if (url != null) {
                    System.out.println("✅ ¡IMAGEN ENCONTRADA EN CLASSPATH!: " + url);
                    Image img = new ImageIcon(url).getImage();
                    return new ImageIcon(img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
                }
            }
        }

        // 2. Búsqueda en disco duro (Rutas relativas)
        for (String ext : extensiones) {
            String nombre = base + ext;
            String[] rutasDisco = {
                "src/horarios/vista/" + nombre,
                "src/" + nombre,
                nombre,
                "Horusys/src/horarios/vista/" + nombre
            };

            for (String ruta : rutasDisco) {
                java.io.File file = new java.io.File(ruta);
                if (file.exists()) {
                    System.out.println("✅ ¡IMAGEN ENCONTRADA EN DISCO!: " + file.getAbsolutePath());
                    Image img = new ImageIcon(file.getAbsolutePath()).getImage();
                    return new ImageIcon(img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
                }
            }
        }

        System.err.println("❌ [DEBUG LOGO] No se encontró ninguna imagen 'cbtis22' (.jpg/.png) en el proyecto.");
        return null;
    }

    private void initCustomListeners() {
        configurarHover(jPanel4, COLOR_VERDE, COLOR_VERDE.brighter(), this::intentarLogin);
        configurarHover(jPanel5, COLOR_DORADO, COLOR_DORADO.brighter(), this::abrirRegistro);
        configurarHover(jPanel6, COLOR_ROJO, COLOR_ROJO.brighter(), () -> System.exit(0));

        jLabelBtnCerrarVentana.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                jLabelBtnCerrarVentana.setForeground(COLOR_ROJO);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                jLabelBtnCerrarVentana.setForeground(Color.WHITE);
            }
        });

        KeyAdapter enterAction = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    intentarLogin();
                }
            }
        };
        FieldUsuario.addKeyListener(enterAction);
        FieldContraseña.addKeyListener(enterAction);
    }

    private void configurarHover(JPanel panel, Color bgNormal, Color bgHover, Runnable action) {
        panel.setBackground(bgNormal);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(bgHover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(bgNormal);
            }
        };

        panel.addMouseListener(adapter);
        for (Component child : panel.getComponents()) {
            child.addMouseListener(adapter);
        }
    }

    private void intentarLogin() {
        String usuario    = FieldUsuario.getText().trim();
        String contrasena = new String(FieldContraseña.getPassword()).trim(); 

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarMensaje("Todos los campos son obligatorios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            boolean ok = controller.iniciarSesion(usuario, contrasena);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    SoundManager.playSuccess();
                    mostrarMensaje("¡Inicio de sesión exitoso!", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    new MainWindow(usuario, 0).setVisible(true);
                } else {
                    SoundManager.playError();
                    mostrarMensaje("Credenciales incorrectas. Verifique usuario y contraseña.", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void abrirRegistro() {
        dispose();
        new RegisterWindow().setVisible(true);
    }

    private void mostrarMensaje(String texto, int tipoMensaje) {
        JOptionPane.showMessageDialog(this, texto, "Aviso del Sistema", tipoMensaje);
    }

    // =========================================================================
    // DIBUJO VECTORIAL (CLASES INTERNAS DE ICONOS PARA CAMPOS Y BOTONES)
    // =========================================================================

    private static class EscudoInstitucionalIcon implements Icon {
        private final int w, h;
        public EscudoInstitucionalIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Path2D shield = new Path2D.Double();
            shield.moveTo(x + 5, y + 5);
            shield.lineTo(x + w - 5, y + 5);
            shield.lineTo(x + w - 5, y + h * 0.55);
            shield.curveTo(x + w - 5, y + h - 5, x + w * 0.5, y + h, x + w * 0.5, y + h);
            shield.curveTo(x + w * 0.5, y + h, x + 5, y + h - 5, x + 5, y + h * 0.55);
            shield.closePath();

            g2.setColor(Color.WHITE);
            g2.fill(shield);
            g2.setColor(COLOR_DORADO);
            g2.setStroke(new BasicStroke(3));
            g2.draw(shield);

            Path2D stripe = new Path2D.Double();
            stripe.moveTo(x + 10, y + 10);
            stripe.lineTo(x + w - 10, y + h * 0.5);
            stripe.lineTo(x + w - 10, y + h * 0.65);
            stripe.lineTo(x + 10, y + 25);
            stripe.closePath();
            g2.setColor(COLOR_VINO);
            g2.fill(stripe);

            g2.setColor(COLOR_DORADO);
            g2.fillOval(x + w / 2 - 6, y + 12, 12, 12);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static class PersonaIcon implements Icon {
        private final int w, h;
        public PersonaIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_VINO);
            g2.fillOval(x + w / 4, y, w / 2, h / 2);
            g2.fillArc(x, y + h / 2 - 2, w, h, 0, 180);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static class CandadoIcon implements Icon {
        private final int w, h;
        public CandadoIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_VINO);
            g2.setStroke(new BasicStroke(2));
            g2.drawArc(x + w / 4, y, w / 2, h / 2 + 2, 0, 180);
            g2.fillRect(x + 2, y + h / 3 + 2, w - 4, h / 2 + 2);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static class PalomitaIcon implements Icon {
        private final int w, h;
        public PalomitaIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D check = new Path2D.Double();
            check.moveTo(x + 2, y + h * 0.55);
            check.lineTo(x + w * 0.4, y + h - 3);
            check.lineTo(x + w - 2, y + 3);
            g2.draw(check);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static class RegistroIcon implements Icon {
        private final int w, h;
        public RegistroIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.drawRect(x + 1, y + 1, w - 7, h - 3);
            g2.drawLine(x + 4, y + 5, x + w - 10, y + 5);
            g2.drawLine(x + 4, y + 9, x + w - 10, y + 9);

            g2.setColor(COLOR_VINO);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(x + w - 8, y + 3, x + w - 1, y + h - 2);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static class SalirIcon implements Icon {
        private final int w, h;
        public SalirIcon(int w, int h) { this.w = w; this.h = h; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(x + 1, y + 1, w / 2, h - 3);
            g2.drawLine(x + 4, y + h / 2, x + w - 1, y + h / 2);
            g2.drawLine(x + w - 5, y + 3, x + w - 1, y + h / 2);
            g2.drawLine(x + w - 5, y + h - 3, x + w - 1, y + h / 2);
            g2.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // </editor-fold>

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Bg = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabelBtnCerrarVentana = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        FieldUsuario = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabelBtnIniciarSesion = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabelBtnRegistrarse = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabelBtnSalir = new javax.swing.JLabel();
        FieldContraseña = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Bg.setBackground(new java.awt.Color(255, 255, 255));
        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(100, 0, 25));

        jPanel3.setBackground(new java.awt.Color(204, 160, 0));
        jPanel3.setPreferredSize(new java.awt.Dimension(850, 4));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        jLabel1.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("INICIO DE SESIÓN");

        jLabelBtnCerrarVentana.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabelBtnCerrarVentana.setForeground(new java.awt.Color(255, 255, 255));
        jLabelBtnCerrarVentana.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelBtnCerrarVentana.setText("X");
        jLabelBtnCerrarVentana.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(318, 318, 318)
                .addComponent(jLabel1)
                .addContainerGap(322, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabelBtnCerrarVentana, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabelBtnCerrarVentana, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addGap(14, 14, 14))
        );

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 850, 120));

        jLabel3.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(100, 0, 25));
        jLabel3.setText("Usuario:");
        Bg.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 210, -1, -1));

        FieldUsuario.setText("jTextField1");
        FieldUsuario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FieldUsuarioActionPerformed(evt);
            }
        });
        Bg.add(FieldUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 210, 340, -1));

        jLabel4.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(100, 0, 25));
        jLabel4.setText("Contraseña:");
        Bg.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 260, -1, -1));

        jPanel4.setBackground(new java.awt.Color(102, 0, 0));

        jLabelBtnIniciarSesion.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabelBtnIniciarSesion.setForeground(new java.awt.Color(204, 160, 0));
        jLabelBtnIniciarSesion.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelBtnIniciarSesion.setText("INICIAR SESIÓN");
        jLabelBtnIniciarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnIniciarSesion, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnIniciarSesion, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 390, 150, 40));

        jPanel5.setBackground(new java.awt.Color(204, 204, 0));

        jLabelBtnRegistrarse.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabelBtnRegistrarse.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelBtnRegistrarse.setText("REGISTRARME");
        jLabelBtnRegistrarse.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnRegistrarse, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnRegistrarse, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 390, -1, -1));

        jPanel6.setBackground(new java.awt.Color(51, 0, 0));

        jLabelBtnSalir.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabelBtnSalir.setForeground(new java.awt.Color(255, 255, 255));
        jLabelBtnSalir.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelBtnSalir.setText("SALIR");
        jLabelBtnSalir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnSalir, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelBtnSalir, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 390, -1, -1));

        FieldContraseña.setText("jPasswordField1");
        Bg.add(FieldContraseña, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 260, 340, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Bg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Bg, javax.swing.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void FieldUsuarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FieldUsuarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_FieldUsuarioActionPerformed
/**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new LoginWindow().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bg;
    private javax.swing.JPasswordField FieldContraseña;
    private javax.swing.JTextField FieldUsuario;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelBtnCerrarVentana;
    private javax.swing.JLabel jLabelBtnIniciarSesion;
    private javax.swing.JLabel jLabelBtnRegistrarse;
    private javax.swing.JLabel jLabelBtnSalir;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    // End of variables declaration//GEN-END:variables
}
