/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;
import horarios.controlador.HorarioController;
import horarios.util.Colores;
import horarios.util.SoundManager;
import horarios.util.SwingUtils;

import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.awt.Color;
/**
 *
 * @author axelp
 */
public class HorariosWindow extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HorariosWindow.class.getName());

    private MainWindow mainWindow;
    private int usuarioId;
    private final HorarioController controller = new HorarioController();
    private DefaultTableModel tableModel;
    private JDialog dialogValidacion;
    private List<String[]> listaProfesores = new java.util.ArrayList<>();

    public HorariosWindow(int usuarioId, MainWindow mainWindow) {
        this.usuarioId = usuarioId;
        this.mainWindow = mainWindow;
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        actualizarComboSeleccion();
        pack();
        setLocationRelativeTo(null);
    }

    public HorariosWindow() {
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        actualizarComboSeleccion();
        pack();
        setLocationRelativeTo(null);
        
        
        JTableHeader header = jTable1.getTableHeader();
        header.setBackground(new Color(100, 0, 25));
        header.setForeground(Color.white);
    }

    private void initCustomListeners() {
        VerHorario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"General", "Por Grupo", "Por Profesor"}));
        
        VerHorario.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                actualizarComboSeleccion();
                mostrarVista();
            }
        });
        
        Seleccionar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                mostrarVista();
            }
        });

        String[] cols = {"HORA", "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        jTable1.setModel(tableModel);
        jTable1.setRowHeight(52);
        jTable1.setFont(new Font("Arial", Font.PLAIN, 12));
        jTable1.setGridColor(new Color(210, 210, 210));
        jTable1.setSelectionBackground(new Color(204, 160, 0)); 
        jTable1.setSelectionForeground(new Color(100, 0, 25)); 

        // --- CONFIGURACIÓN DEL ENCABEZADO (Header) ---
JTableHeader th = jTable1.getTableHeader();
th.setPreferredSize(new Dimension(0, 45)); // Altura del encabezado

th.setDefaultRenderer(new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        setBackground(new Color(100, 0, 25));    // Fondo Vino
        setForeground(new Color(204, 160, 0));  // Letras Amarillas
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setHorizontalAlignment(javax.swing.JLabel.CENTER);
        
        // Borde amarillo sutil entre columnas del header
        setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(204, 160, 0, 50)));
        
        return this;
    }
});

// Pinta el rincón sobrante del scroll de color vino para que no se vea blanco
javax.swing.JPanel corner = new javax.swing.JPanel();
corner.setBackground(new Color(100, 0, 25));
jScrollPane1.setCorner(javax.swing.JScrollPane.UPPER_RIGHT_CORNER, corner);

// --- TU RENDERIZADOR DE CELDAS (El que ya tenías para el contenido) ---
jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(javax.swing.JTable t, Object val, boolean sel, boolean foc, int row, int col) {
        super.getTableCellRendererComponent(t, val, sel, foc, row, col);
        setHorizontalAlignment(javax.swing.JLabel.CENTER);
        
        String txt = val == null ? "" : val.toString();
        if (txt.contains("\n")) setText("<html>" + txt.replace("\n", "<br>") + "</html>");
        
        if (sel) {
            setBackground(new Color(204, 160, 0)); 
            setForeground(new Color(100, 0, 25));
        } else if (col == 0) {
            setBackground(new Color(100, 0, 25)); 
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD));
        } else {
            setBackground(row % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
            setForeground(Color.BLACK);
        }
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
        return this;
    }
});

        BtnGenerarHorario.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { generarHorario(); } });
        BtnValidarHorario.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { validarHorario(); } });
        BtnRegresarMenu.addMouseListener(new MouseAdapter() { 
            @Override public void mouseClicked(MouseEvent e) { 
                dispose(); 
                if (mainWindow != null) mainWindow.setVisible(true);
            } 
        });
        
        BtnBuscar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { buscar(); }
        });
    }
    
    private void buscar() {
        // Creamos un campo de texto para la mini-ventana
        javax.swing.JTextField txtBusqueda = new javax.swing.JTextField(20);
        Object[] mensaje = {
            "Ingrese el nombre (o apellidos) del profesor a buscar:", txtBusqueda
        };
        
        // Personalizamos los botones
        Object[] opciones = {"Buscar", "Cancelar"};

        // Mostramos la ventana emergente
        int opcionElegida = JOptionPane.showOptionDialog(this, mensaje, "Buscar Horario de Profesor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones);

        // Si el usuario presionó "Buscar"
        if (opcionElegida == 0) {
            String termino = txtBusqueda.getText().trim();
            
            if (!termino.isEmpty()) {
                // Cambiamos el combo a "Por Profesor" para que la UI tenga coherencia
                VerHorario.setSelectedItem("Por Profesor");
                
                // Llamamos al controlador para buscar el horario pivoteado
                List<Map<String, Object>> resultados = controller.buscarHorarioPorNombreProfesor(termino);
                
                // Limpiamos la tabla y la llenamos con los resultados
                tableModel.setRowCount(0);
                for (Map<String, Object> f : resultados) {
                    tableModel.addRow(new Object[]{
                        f.get("HORA"), f.get("LUNES"), f.get("MARTES"),
                        f.get("MIERCOLES"), f.get("JUEVES"), f.get("VIERNES")
                    });
                }
                
                // Quitamos la selección del combo de profesores para que quede claro que es una búsqueda manual
                Seleccionar.setSelectedIndex(-1);
                
                // Si la base de datos no arrojó nada, avisamos
                if (resultados.isEmpty()) {
                    tableModel.addRow(new Object[]{"—", "Sin datos para esta búsqueda.", "", "", "", ""});
                    JOptionPane.showMessageDialog(this, "No se encontró horario para un profesor con ese nombre.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // Si dejó el texto vacío y le dio a buscar, recarga la vista normal
                mostrarVista();
            }
        }
    }

    private void generarHorario() {
        actualizarEstado("Generando horario, por favor espere…", new Color(160, 100, 0));
        new Thread(() -> {
            boolean ok = controller.generarHorario(usuarioId);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    SoundManager.playSuccess();
                    actualizarEstado("¡Horario generado con éxito!", new Color(0, 150, 0)); 
                    actualizarComboSeleccion();
                    mostrarVista();
                } else {
                    SoundManager.playError();
                    actualizarEstado("Error al generar el horario. Revise la consola.", Color.RED);
                }
            });
        }).start();
    }

    private void validarHorario() {
        actualizarEstado("Validando horario…", new Color(160, 100, 0));
        new Thread(() -> {
            List<String> problemas = controller.validarDetallado();
            SwingUtilities.invokeLater(() -> mostrarDialogValidacion(problemas));
        }).start();
    }

    private void mostrarDialogValidacion(List<String> problemas) {
        if (dialogValidacion != null) dialogValidacion.dispose();

        boolean valido = problemas.size() == 1 && problemas.get(0).startsWith("SIN PROBLEMAS");
        if (valido) SoundManager.playSuccess(); else SoundManager.playError();
        actualizarEstado(
            valido ? "✅ Validación exitosa — Sin choques." : "❌ Se encontraron " + (problemas.size() - 1) + " problema(s).",
            valido ? new Color(0, 150, 0) : Color.RED
        );

        dialogValidacion = new JDialog(this, "Reporte de Validación", false);
        dialogValidacion.setSize(720, 500);
        dialogValidacion.setLocationRelativeTo(this);
        dialogValidacion.setLayout(new BorderLayout(0, 0));

        javax.swing.JPanel dHead = new javax.swing.JPanel(new BorderLayout());
        dHead.setBackground(new Color(100, 0, 25));
        dHead.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15));
        javax.swing.JLabel dTit = new javax.swing.JLabel("REPORTE DE VALIDACIÓN DE HORARIO");
        dTit.setFont(new Font("Arial", Font.BOLD, 16));
        dTit.setForeground(new Color(204, 160, 0));
        dHead.add(dTit, BorderLayout.WEST);
        
        javax.swing.JPanel dLine = new javax.swing.JPanel();
        dLine.setBackground(new Color(204, 160, 0));
        dLine.setPreferredSize(new Dimension(720, 3));
        
        javax.swing.JPanel dHW = new javax.swing.JPanel(new BorderLayout());
        dHW.setBackground(new Color(100, 0, 25));
        dHW.add(dHead, BorderLayout.CENTER);
        dHW.add(dLine, BorderLayout.SOUTH);
        dialogValidacion.add(dHW, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String p : problemas) listModel.addElement(p);
        JList<String> jList = new JList<>(listModel);
        jList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        jList.setBackground(new Color(252, 250, 248));
        jList.setSelectionBackground(new Color(204, 160, 0));
        jList.setSelectionForeground(new Color(100, 0, 25));

        JScrollPane sc = new JScrollPane(jList);
        sc.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(100, 0, 25), 1));

        javax.swing.JLabel lRes = new javax.swing.JLabel(valido
            ? "  ✅  El horario no presenta choques."
            : "  ❌  Se encontraron " + (problemas.size() - 1) + " problema(s). Revise la lista.");
        lRes.setFont(new Font("Arial", Font.BOLD, 14));
        lRes.setForeground(valido ? new Color(0, 150, 0) : Color.RED);
        lRes.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0));

        javax.swing.JPanel centro = new javax.swing.JPanel(new BorderLayout());
        centro.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));
        centro.setBackground(new Color(252, 250, 248));
        centro.add(lRes, BorderLayout.NORTH);
        centro.add(sc,   BorderLayout.CENTER);
        dialogValidacion.add(centro, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("CERRAR");
        btnCerrar.setBackground(new Color(100, 0, 25));
        btnCerrar.setForeground(new Color(204, 160, 0));
        btnCerrar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCerrar.setPreferredSize(new Dimension(120, 34));
        btnCerrar.addActionListener(e -> dialogValidacion.dispose());

        javax.swing.JPanel pie = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        pie.setBackground(Color.WHITE);
        pie.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(204, 160, 0)));
        pie.add(btnCerrar);
        dialogValidacion.add(pie, BorderLayout.SOUTH);

        dialogValidacion.setVisible(true);
    }

    private void actualizarComboSeleccion() {
        java.awt.event.ActionListener[] listeners = Seleccionar.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) {
            Seleccionar.removeActionListener(l);
        }
        Seleccionar.removeAllItems();

        String tipo = (String) VerHorario.getSelectedItem();
        if ("General".equals(tipo)) {
            Seleccionar.setEnabled(false);
            Seleccionar.addItem("(todos los grupos)");
        } else if ("Por Grupo".equals(tipo)) {
            Seleccionar.setEnabled(true);
            List<String> grupos = controller.obtenerCodigosGrupos();
            if (grupos.isEmpty()) Seleccionar.addItem("(vacio)");
            else { for (String g : grupos) Seleccionar.addItem(g); }
        } else {
            Seleccionar.setEnabled(true);
            listaProfesores = controller.obtenerProfesoresConNombre();
            if (listaProfesores.isEmpty()) Seleccionar.addItem("(vacio)");
            else { 
                for (String[] prof : listaProfesores) {
                    Seleccionar.addItem(prof[1]);
}
            }
        }

        for (java.awt.event.ActionListener l : listeners) {
            Seleccionar.addActionListener(l);
        }
        mostrarVista();
    }

    private void mostrarVista() {
        tableModel.setRowCount(0);
        String tipo = (String) VerHorario.getSelectedItem();
        String sel  = (String) Seleccionar.getSelectedItem();

        if (sel == null || sel.startsWith("(")) {
            tableModel.addRow(new Object[]{"—", "Presione GENERAR HORARIO para crear el horario.", "", "", "", ""});
            return;
        }

        List<Map<String, Object>> datos;
        if ("General".equals(tipo)) {
            datos = controller.obtenerHorarioGeneral();
        } else if ("Por Grupo".equals(tipo)) {
            datos = controller.obtenerHorarioPorGrupo(sel);
        } else {
            int idx = Seleccionar.getSelectedIndex();
            String rfc = (idx >= 0 && idx < listaProfesores.size()) ? listaProfesores.get(idx)[0] : sel;
            datos = controller.obtenerHorarioPorProfesor(rfc);
        }

        for (Map<String, Object> f : datos) {
            tableModel.addRow(new Object[]{
                f.get("HORA"), f.get("LUNES"), f.get("MARTES"),
                f.get("MIERCOLES"), f.get("JUEVES"), f.get("VIERNES")
            });
        }

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{"—", "Sin datos para esta selección.", "", "", "", ""});
        }
    }

    private void actualizarEstado(String msg, Color color) {
        jLabel2.setText(msg);
        jLabel2.setForeground(color);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel7 = new javax.swing.JPanel();
        Bg = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        BtnGenerarHorario = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        BtnValidarHorario = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        VerHorario = new javax.swing.JComboBox<>();
        Seleccionar = new javax.swing.JComboBox<>();
        jButtonGenerarExcel = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        BtnBuscar = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        BtnRegresarMenu = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(100, 0, 25));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 160, 0));
        jLabel1.setText("MODULO DE HORARIOS");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addComponent(jLabel1)
                .addContainerGap(772, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(15, 15, 15))
        );

        Bg.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1060, 60));

        jPanel2.setBackground(new java.awt.Color(204, 160, 0));
        jPanel2.setPreferredSize(new java.awt.Dimension(1060, 4));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1060, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 1060, 4));

        jPanel3.setBackground(new java.awt.Color(0, 120, 0));

        BtnGenerarHorario.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnGenerarHorario.setForeground(new java.awt.Color(255, 255, 255));
        BtnGenerarHorario.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnGenerarHorario.setText("GENERAR HORARIO");
        BtnGenerarHorario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnGenerarHorario, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnGenerarHorario, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 130, 40));

        jPanel4.setBackground(new java.awt.Color(100, 0, 25));

        BtnValidarHorario.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnValidarHorario.setForeground(new java.awt.Color(204, 160, 0));
        BtnValidarHorario.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnValidarHorario.setText("VALIDAR HORARIO");
        BtnValidarHorario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnValidarHorario, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnValidarHorario, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 90, 120, -1));

        jPanel6.setBackground(new java.awt.Color(204, 160, 0));
        jPanel6.setPreferredSize(new java.awt.Dimension(1040, 4));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1040, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 150, 1040, 4));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Hora", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes"
            }
        ));
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        Bg.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, 1020, 450));

        jPanel8.setBackground(new java.awt.Color(204, 160, 0));
        jPanel8.setPreferredSize(new java.awt.Dimension(1060, 4));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1060, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 630, 1060, 4));

        jLabel2.setBackground(new java.awt.Color(100, 0, 25));
        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(100, 0, 25));
        jLabel2.setText("Presione GENERAR HORARIO para comenzar");
        Bg.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 650, -1, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(100, 0, 25));
        jLabel6.setText("Seleccionar:");
        Bg.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 100, -1, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(100, 0, 25));
        jLabel7.setText("Ver horario:");
        Bg.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 100, -1, -1));

        VerHorario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        VerHorario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VerHorarioActionPerformed(evt);
            }
        });
        Bg.add(VerHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 100, 140, -1));

        Seleccionar.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Seleccionar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SeleccionarActionPerformed(evt);
            }
        });
        Bg.add(Seleccionar, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 100, 170, -1));

        jButtonGenerarExcel.setText("Generar Excel");
        jButtonGenerarExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerarExcelActionPerformed(evt);
            }
        });
        Bg.add(jButtonGenerarExcel, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 640, -1, -1));

        jPanel9.setBackground(new java.awt.Color(100, 0, 25));

        BtnBuscar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnBuscar.setForeground(new java.awt.Color(204, 160, 0));
        BtnBuscar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnBuscar.setText("BUSCAR");
        BtnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 90, 120, 40));

        jPanel5.setBackground(new java.awt.Color(70, 0, 15));

        BtnRegresarMenu.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnRegresarMenu.setForeground(new java.awt.Color(255, 255, 255));
        BtnRegresarMenu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnRegresarMenu.setText("REGRESAR AL MENU");
        BtnRegresarMenu.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 90, -1, 40));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Bg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Bg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void VerHorarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VerHorarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_VerHorarioActionPerformed

    private void SeleccionarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SeleccionarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SeleccionarActionPerformed

    private void jButtonGenerarExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerarExcelActionPerformed

        String tipo = (String) VerHorario.getSelectedItem();
        String sel  = (String) Seleccionar.getSelectedItem();

        if (sel == null || sel.startsWith("(")) {
            JOptionPane.showMessageDialog(this,
                "Primero genera el horario y selecciona una vista.",
                "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("General".equals(tipo)) {
            horarios.util.ExcelExporter.exportarHorarioGeneral(this);

        } else if ("Por Grupo".equals(tipo)) {
            horarios.util.ExcelExporter.exportarHorarioPorGrupo(
                this,
                sel,           // código del grupo
                "",            // tutor (puedes dejarlo vacío por ahora)
                "",            // aula
                ""             // especialidad
            );

        } else { // Por Profesor
            int idx = Seleccionar.getSelectedIndex();
            String rfc = (idx >= 0 && idx < listaProfesores.size())
                         ? listaProfesores.get(idx)[0] : sel;
            horarios.util.ExcelExporter.exportarHorarioPorProfesor(this, rfc, sel);
        }

    }//GEN-LAST:event_jButtonGenerarExcelActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new HorariosWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bg;
    private javax.swing.JLabel BtnBuscar;
    private javax.swing.JLabel BtnGenerarHorario;
    private javax.swing.JLabel BtnRegresarMenu;
    private javax.swing.JLabel BtnValidarHorario;
    private javax.swing.JComboBox<String> Seleccionar;
    private javax.swing.JComboBox<String> VerHorario;
    private javax.swing.JButton jButtonGenerarExcel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
