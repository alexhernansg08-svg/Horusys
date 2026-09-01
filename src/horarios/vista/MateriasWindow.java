/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;
import horarios.controlador.EspecialidadController;
import horarios.controlador.MateriaController;
import horarios.controlador.ModuloController;
import horarios.modelo.Especialidad;
import horarios.modelo.Materia;
import horarios.modelo.Modulo;
import horarios.util.SoundManager;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComboBox;
/**
 *
 * @author axelp
 */
public class MateriasWindow extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MateriasWindow.class.getName());

    /**
     * Creates new form MateriasWindow
     */
    private MainWindow mainWindow;
    private int userId;
    private final MateriaController materiaController = new MateriaController();
    private final EspecialidadController especialidadController = new EspecialidadController();
    private final ModuloController moduloController = new ModuloController();
    private DefaultTableModel tableModel;

    // --- Combo de Modulo (Competencias laborales) ---
    // Se agrega a mano (fuera del bloque que regenera el editor visual de
    // NetBeans) para no arriesgar el diseño existente. Se posiciona en el
    // mismo espacio libre que quedaba junto a "Horas semanales".
    // Solo aplica a materias que SI son submodulos de Competencias laborales;
    // el resto de las materias se deja en "(Ninguno)".
    private javax.swing.JLabel lblModulo;
    private javax.swing.JComboBox<Modulo> ComboModulo;

    public MateriasWindow(MainWindow mainWindow, int userId) {
        this.mainWindow = mainWindow;
        this.userId = userId;
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarEspecialidades();
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    public MateriasWindow() {
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarEspecialidades();
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    private void initCustomListeners() {
        tableModel = (DefaultTableModel) jTable1.getModel();
        
        ComboModulo = new JComboBox<>();
    lblModulo = new javax.swing.JLabel("Módulo:");
    
    lblModulo.setFont(new java.awt.Font("Segoe UI", 1, 12));
    lblModulo.setForeground(new java.awt.Color(153, 0, 0));
    Bg.add(lblModulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 230, -1, -1));
    Bg.add(ComboModulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 230, 280, -1));
        // Se agrega la columna "Módulo" al vuelo (no toca initComponents()).
        tableModel.setColumnIdentifiers(new Object[]{"ID", "Clave", "Nombre", "Especialidad", "Semestre", "Hora/Sem", "Módulo"});
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccionado();
        });

        Semestre.setModel(new SpinnerNumberModel(1, 1, 12, 1));
        HorasSemanales.setModel(new SpinnerNumberModel(3, 1, 10, 1));

        Clave.setText("");
        NombreDeMateria.setText("");

        BtnAgregarMateria.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { agregar(); } });
        BtnActualizar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { actualizar(); } });
        BtnEliminar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { eliminar(); } });
        BtnLimpiar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { limpiar(); } });
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
            "Ingrese el nombre de la materia a buscar:", txtBusqueda
        };
        
        // Personalizamos los botones
        Object[] opciones = {"Buscar", "Cancelar"};

        // Mostramos la ventana emergente
        int opcionElegida = JOptionPane.showOptionDialog(this, mensaje, "Buscar Materia",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones);

        // Si el usuario presionó "Buscar"
        if (opcionElegida == 0) {
            String termino = txtBusqueda.getText().trim();
            
            if (!termino.isEmpty()) {
                // Llamamos al controlador para buscar
                java.util.List<Materia> resultados = materiaController.buscarPorNombre(termino);
                cargarResultadosBusqueda(resultados);
                
                // Si no hay resultados, avisamos al usuario
                if (resultados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron materias con ese nombre.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // Si dejó el texto vacío y le dio a buscar, cargamos toda la tabla de nuevo
                cargarDatos();
            }
        }
    }

    // Este método limpia la tabla y mete solo las materias encontradas
    private void cargarResultadosBusqueda(java.util.List<Materia> lista) {
        tableModel.setRowCount(0); // Limpiar la tabla
        for (Materia m : lista) {
            tableModel.addRow(new Object[]{
                m.getIdMateria(), m.getClave(), m.getNombre(),
                m.getEspecialidadNombre(), m.getIdSemestre(), m.getHorasSemanales()
            });
        }
    }

    private void agregar() {
        Materia m = leerFormulario(0);
        String error = materiaController.agregar(m);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Materia agregada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona una materia para actualizar."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        Materia m = leerFormulario(id);
        String error = materiaController.actualizar(m);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Materia actualizada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona una materia para eliminar."); return; }
        String nombre = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la materia \"" + nombre + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) tableModel.getValueAt(row, 0);
            String error = materiaController.eliminar(id);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Materia eliminada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                cargarDatos();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Materia leerFormulario(int id) {
        Materia m = new Materia();
        m.setIdMateria(id);
        m.setClave(Clave.getText().trim());
        m.setNombre(NombreDeMateria.getText().trim());
        Especialidad esp = (Especialidad) (Object) this.Especialidad.getSelectedItem();
        m.setIdEspecialidad(esp != null ? esp.getId() : 0);
        
        m.setIdSemestre((Integer) Semestre.getValue());
        m.setHorasSemanales((Integer) HorasSemanales.getValue());

        Modulo mod = (Modulo) ComboModulo.getSelectedItem();
        m.setIdModulo(mod != null && mod.getIdModulo() != 0 ? mod.getIdModulo() : null);
        return m;
    }

    @SuppressWarnings("unchecked")
    private void cargarEspecialidades() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Especialidad e : especialidadController.obtenerTodas()) {
            model.addElement(e);
        }
        this.Especialidad.setModel(model);
        cargarModulos(); // refresca el combo de Modulo para la especialidad recien seleccionada
    }

    /**
     * Refresca el combo de Modulo segun la Especialidad seleccionada actualmente.
     * Siempre incluye "(Ninguno)" primero (idModulo=0 como centinela), ya que
     * la mayoria de las materias NO son submodulos de Competencias laborales.
     */
    private void cargarModulos() {
        Especialidad esp = (Especialidad) (Object) this.Especialidad.getSelectedItem();
        DefaultComboBoxModel<Modulo> modelo = new DefaultComboBoxModel<>();

        Modulo ninguno = new Modulo();
        ninguno.setIdModulo(0);
        ninguno.setNombre("(Ninguno — no es un submódulo)");
        modelo.addElement(ninguno);

        if (esp != null) {
            for (Modulo m : moduloController.obtenerPorEspecialidad(esp.getId())) {
                modelo.addElement(m);
            }
        }
        ComboModulo.setModel(modelo);
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        for (Materia m : materiaController.obtenerTodas()) {
            tableModel.addRow(new Object[]{
                m.getIdMateria(), m.getClave(), m.getNombre(),
                m.getEspecialidadNombre(), m.getIdSemestre(), m.getHorasSemanales(),
                m.getModuloEtiqueta() != null ? m.getModuloEtiqueta() : "—"
            });
        }
    }

    private void cargarSeleccionado() {
        int row = jTable1.getSelectedRow();
        if (row == -1) return;
        
        Clave.setText((String) tableModel.getValueAt(row, 1));
        NombreDeMateria.setText((String) tableModel.getValueAt(row, 2));
        Semestre.setValue(tableModel.getValueAt(row, 4));
        HorasSemanales.setValue(tableModel.getValueAt(row, 5));

        String nombreEsp = (String) tableModel.getValueAt(row, 3);
        for (int i = 0; i < this.Especialidad.getItemCount(); i++) {
            Especialidad esp = (Especialidad) (Object) this.Especialidad.getItemAt(i);
            if (esp != null && esp.getNombre().equals(nombreEsp)) {
                this.Especialidad.setSelectedIndex(i);
                break;
            }
        }
        cargarModulos(); // refresca el combo para la especialidad recien seleccionada

        // Buscar el modulo real de esta materia (la tabla solo trae su etiqueta de texto)
        String clave = (String) tableModel.getValueAt(row, 1);
        Materia completa = materiaController.buscarPorClave(clave);
        Integer idModulo = completa != null ? completa.getIdModulo() : null;
        for (int i = 0; i < ComboModulo.getItemCount(); i++) {
            Modulo mod = ComboModulo.getItemAt(i);
            boolean esElegido = (idModulo == null) ? mod.getIdModulo() == 0 : mod.getIdModulo() == idModulo;
            if (esElegido) { ComboModulo.setSelectedIndex(i); break; }
        }
    }

    private void limpiar() {
        Clave.setText("");
        NombreDeMateria.setText("");
        Semestre.setValue(1);
        HorasSemanales.setValue(3);
        if (this.Especialidad.getItemCount() > 0) this.Especialidad.setSelectedIndex(0);
        cargarModulos();
        if (ComboModulo.getItemCount() > 0) ComboModulo.setSelectedIndex(0); // "(Ninguno)"
        jTable1.clearSelection();
    }

    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Advertencia", JOptionPane.WARNING_MESSAGE);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Bg = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        BtnAgregarMateria = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        BtnActualizar = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        BtnEliminar = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        BtnLimpiar = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        BtnRegresarMenu = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        NombreDeMateria = new javax.swing.JTextField();
        Especialidad = new javax.swing.JComboBox<>();
        Semestre = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        Clave = new javax.swing.JTextField();
        HorasSemanales = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel9 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        BtnBuscar = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(100, 0, 25));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 160, 0));
        jLabel1.setText("GESTION DE MATERIAS");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(89, 89, 89)
                .addComponent(jLabel1)
                .addContainerGap(663, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(18, 18, 18))
        );

        Bg.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1020, 70));

        jPanel2.setBackground(new java.awt.Color(204, 160, 0));
        jPanel2.setPreferredSize(new java.awt.Dimension(1020, 4));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1020, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 1020, 4));

        jPanel3.setBackground(new java.awt.Color(100, 0, 25));

        BtnAgregarMateria.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnAgregarMateria.setForeground(new java.awt.Color(204, 160, 0));
        BtnAgregarMateria.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnAgregarMateria.setText("AGREGAR MATERIA");
        BtnAgregarMateria.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAgregarMateria, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAgregarMateria, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, 150, 40));

        jPanel4.setBackground(new java.awt.Color(100, 0, 25));

        BtnActualizar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnActualizar.setForeground(new java.awt.Color(204, 160, 0));
        BtnActualizar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnActualizar.setText("ACTUALIZAR");
        BtnActualizar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnActualizar, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnActualizar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 90, -1, -1));

        jPanel5.setBackground(new java.awt.Color(255, 0, 0));

        BtnEliminar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnEliminar.setForeground(new java.awt.Color(255, 255, 255));
        BtnEliminar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnEliminar.setText("ELIMINAR");
        BtnEliminar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnEliminar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnEliminar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 90, -1, -1));

        jPanel6.setBackground(new java.awt.Color(153, 153, 153));

        BtnLimpiar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        BtnLimpiar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnLimpiar.setText("LIMPIAR");
        BtnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnLimpiar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnLimpiar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 90, -1, -1));

        jPanel7.setBackground(new java.awt.Color(51, 0, 0));

        BtnRegresarMenu.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnRegresarMenu.setForeground(new java.awt.Color(204, 160, 0));
        BtnRegresarMenu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnRegresarMenu.setText("REGRESAR AL MENU");
        BtnRegresarMenu.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 90, -1, -1));

        jPanel8.setBackground(new java.awt.Color(204, 160, 0));
        jPanel8.setPreferredSize(new java.awt.Dimension(1000, 4));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 1000, 4));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(153, 0, 0));
        jLabel2.setText("Horas semanales:");
        Bg.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 230, -1, -1));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(153, 0, 0));
        jLabel3.setText("Semestre:");
        Bg.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 200, -1, -1));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(153, 0, 0));
        jLabel4.setText("Especialidad:");
        Bg.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 200, -1, -1));

        NombreDeMateria.setText("jTextField1");
        NombreDeMateria.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NombreDeMateriaActionPerformed(evt);
            }
        });
        Bg.add(NombreDeMateria, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 170, 280, -1));

        Especialidad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Especialidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EspecialidadActionPerformed(evt);
            }
        });
        Bg.add(Especialidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 200, 280, -1));
        Bg.add(Semestre, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 200, 280, -1));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(153, 0, 0));
        jLabel5.setText("Clave:");
        Bg.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 170, -1, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(153, 0, 0));
        jLabel6.setText("Nombre de la materia:");
        Bg.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 170, -1, -1));

        Clave.setText("jTextField1");
        Clave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClaveActionPerformed(evt);
            }
        });
        Bg.add(Clave, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 170, 280, -1));
        Bg.add(HorasSemanales, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 230, 280, -1));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Clave", "Nombre", "Especialidad", "Semestre", "Hora/Sem"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        Bg.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 960, 350));

        jPanel9.setBackground(new java.awt.Color(204, 160, 0));
        jPanel9.setPreferredSize(new java.awt.Dimension(1020, 4));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1020, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 650, 1020, 4));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(102, 0, 0));
        jLabel7.setText("Seleccione una materia de la tabla o complete el formulario");
        Bg.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 670, -1, -1));

        jPanel10.setBackground(new java.awt.Color(100, 0, 25));

        BtnBuscar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnBuscar.setForeground(new java.awt.Color(204, 160, 0));
        BtnBuscar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnBuscar.setText("BUSCAR");
        BtnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 90, 140, 40));

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

    private void ClaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClaveActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClaveActionPerformed

    private void EspecialidadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EspecialidadActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EspecialidadActionPerformed

    private void NombreDeMateriaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NombreDeMateriaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NombreDeMateriaActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new MateriasWindow().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bg;
    private javax.swing.JLabel BtnActualizar;
    private javax.swing.JLabel BtnAgregarMateria;
    private javax.swing.JLabel BtnBuscar;
    private javax.swing.JLabel BtnEliminar;
    private javax.swing.JLabel BtnLimpiar;
    private javax.swing.JLabel BtnRegresarMenu;
    private javax.swing.JTextField Clave;
    private javax.swing.JComboBox<String> Especialidad;
    private javax.swing.JSpinner HorasSemanales;
    private javax.swing.JTextField NombreDeMateria;
    private javax.swing.JSpinner Semestre;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
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
