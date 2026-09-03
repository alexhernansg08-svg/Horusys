/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;

import horarios.controlador.EspecialidadController;
import horarios.controlador.GrupoController;
import horarios.controlador.ProfesorController; // <-- IMPORT AGREGADO
import horarios.modelo.Especialidad;
import horarios.modelo.Grupo;
import horarios.modelo.Profesor; // <-- IMPORT AGREGADO
import horarios.util.SoundManager;

import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList; // <-- IMPORT AGREGADO
import java.util.List; // <-- IMPORT AGREGADO

public class GruposWindow extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GruposWindow.class.getName());

    // ── Variables ──
    private MainWindow mainWindow;
    private int userId;
    private final GrupoController grupoController = new GrupoController();
    private final EspecialidadController especialidadController = new EspecialidadController();
    private DefaultTableModel tableModel;
    
    // <-- NUEVA VARIABLE PARA LOS PROFESORES -->
    private List<Profesor> listaProfesoresTutor = new ArrayList<>();
    private List<Grupo> listaGruposActuales = new ArrayList<>();

    public GruposWindow(MainWindow mainWindow, int userId) {
        this.mainWindow = mainWindow;
        this.userId = userId;
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarEspecialidades();
        cargarComboTutores(); // <-- LLAMADA AGREGADA
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    public GruposWindow() {
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarEspecialidades();
        cargarComboTutores(); // <-- LLAMADA AGREGADA
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    // <-- NUEVO MÉTODO PARA LLENAR EL COMBO -->
    private void cargarComboTutores() {
        Tutor.removeAllItems();
        ProfesorController profController = new ProfesorController();
        listaProfesoresTutor = profController.obtenerTodos(); 
        
        Tutor.addItem("Sin Tutor"); 
        if (listaProfesoresTutor != null) {
            for (Profesor p : listaProfesoresTutor) {
                Tutor.addItem(p.getNombre() + " " + p.getApellidos()); 
            }
        }
    }

    private void initCustomListeners() {
        tableModel = (DefaultTableModel) jTable1.getModel();
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccionado();
        });

        Semestre.setModel(new SpinnerNumberModel(1, 1, 12, 1));
        Capacidad.setModel(new SpinnerNumberModel(30, 15, 100, 5));

        // CORRECCIÓN: El combo de los horarios es Turno1, no Tutor
        Turno1.setModel(new DefaultComboBoxModel<>(new String[]{"Matutino", "Vespertino"}));

        NombreGrupo.setText("");
        Codigo.setText("");

        jLabel9.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { agregar(); } });
        jLabel10.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { actualizar(); } });
        jLabel11.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { eliminar(); } });
        jLabel12.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { limpiar(); } });
        jLabel13.addMouseListener(new MouseAdapter() { 
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
            "Ingrese el nombre del grupo a buscar (ej. A):", txtBusqueda
        };
        
        // Personalizamos los botones
        Object[] opciones = {"Buscar", "Cancelar"};

        // Mostramos la ventana emergente
        int opcionElegida = JOptionPane.showOptionDialog(this, mensaje, "Buscar Grupo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones);

        // Si el usuario presionó "Buscar" (la opción 0)
        if (opcionElegida == 0) {
            String termino = txtBusqueda.getText().trim();
            
            if (!termino.isEmpty()) {
                // Llamamos al controlador para buscar
                java.util.List<Grupo> resultados = grupoController.buscarPorNombre(termino);
                cargarResultadosBusqueda(resultados);
                
                // Si no hay resultados, avisamos al usuario
                if (resultados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron grupos con ese nombre.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // Si dejó el texto vacío y le dio a buscar, cargamos toda la tabla de nuevo
                cargarDatos();
            }
        }
    }

    // Este método limpia la tabla y mete solo los grupos encontrados
    private void cargarResultadosBusqueda(java.util.List<Grupo> lista) {
    tableModel.setRowCount(0); 
    listaGruposActuales = lista; // Guardar la lista filtrada
    if (listaGruposActuales != null) {
        for (Grupo g : listaGruposActuales) {
            tableModel.addRow(new Object[]{
                g.getId(), g.getNombre(), g.getCodigo(),
                g.getEspecialidadNombre(), g.getSemestre(), g.getTurno(), g.getCapacidad()
            });
        }
    }
}

    private void agregar() {
        Grupo grupo = leerFormulario(0);
        String error = grupoController.agregar(grupo);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Grupo agregado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
            limpiar();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona un grupo para actualizar."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        Grupo grupo = leerFormulario(id);
        String error = grupoController.actualizar(grupo);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Grupo actualizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
            limpiar();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona un grupo para eliminar."); return; }
        String codigo = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar el grupo \"" + codigo + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) tableModel.getValueAt(row, 0);
            String error = grupoController.eliminar(id);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Grupo eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatos();
                limpiar();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // <-- MÉTODO CORREGIDO PARA EXTRAER TURNO Y TUTOR -->
    private Grupo leerFormulario(int id) {
        Grupo g = new Grupo();
        g.setId(id);
        g.setNombre(NombreGrupo.getText().trim());
        g.setCodigo(Codigo.getText().trim());
        
        Especialidad esp = (Especialidad) (Object) this.Especialidad.getSelectedItem();
        g.setEspecialidadId(esp != null ? esp.getId() : 0);
        
        g.setSemestre((Integer) Semestre.getValue());
        g.setCapacidad((Integer) Capacidad.getValue());
        
        g.setTurno((String) Turno1.getSelectedItem()); // Turno1 para el horario

        int idx = Tutor.getSelectedIndex();
        if (idx > 0 && listaProfesoresTutor != null) {
            try {
                int idProf = listaProfesoresTutor.get(idx - 1).getId(); 
                g.setIdTutor(idProf);
            } catch (Exception e) {
                g.setIdTutor(null);
            }
        } else {
            g.setIdTutor(null);
        }
        
        return g;
    }

    @SuppressWarnings("unchecked")
    private void cargarEspecialidades() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Especialidad e : especialidadController.obtenerTodas()) {
            model.addElement(e);
        }
        this.Especialidad.setModel(model);
    }

    private void cargarDatos() {
    tableModel.setRowCount(0);
    listaGruposActuales = grupoController.obtenerTodos(); // Guardar copia de los objetos Grupo
    if (listaGruposActuales != null) {
        for (Grupo g : listaGruposActuales) {
            tableModel.addRow(new Object[]{
                g.getId(), g.getNombre(), g.getCodigo(),
                g.getEspecialidadNombre(), g.getSemestre(), g.getTurno(), g.getCapacidad()
            });
        }
    }
}

    private void cargarSeleccionado() {
    int row = jTable1.getSelectedRow();
    if (row == -1 || row >= listaGruposActuales.size()) return;

    // Obtener el objeto Grupo directamente
    Grupo g = listaGruposActuales.get(row);

    NombreGrupo.setText(g.getNombre());
    Codigo.setText(g.getCodigo());
    Semestre.setValue(g.getSemestre());
    Turno1.setSelectedItem(g.getTurno());
    Capacidad.setValue(g.getCapacidad());

    // 1. Seleccionar Especialidad
    String nombreEsp = g.getEspecialidadNombre();
    if (nombreEsp != null) {
        for (int i = 0; i < this.Especialidad.getItemCount(); i++) {
            Especialidad esp = (Especialidad) (Object) this.Especialidad.getItemAt(i);
            if (esp != null && nombreEsp.equals(esp.getNombre())) {
                this.Especialidad.setSelectedIndex(i);
                break;
            }
        }
    }

    // 2. Seleccionar Tutor en el JComboBox mediante el idTutor
    if (g.getIdTutor() != null && listaProfesoresTutor != null) {
        boolean encontrado = false;
        for (int i = 0; i < listaProfesoresTutor.size(); i++) {
            Profesor p = listaProfesoresTutor.get(i);
            if (p.getId() == g.getIdTutor()) {
                // Se suma 1 porque el índice 0 del JComboBox es "Sin Tutor"
                Tutor.setSelectedIndex(i + 1); 
                encontrado = true;
                break;
            }
        }
        if (!encontrado) Tutor.setSelectedIndex(0);
    } else {
        Tutor.setSelectedIndex(0); // Default: "Sin Tutor"
    }
}

    private void limpiar() {
        NombreGrupo.setText("");
        Codigo.setText("");
        Semestre.setValue(1);
        Capacidad.setValue(30);
        if (Turno1.getItemCount() > 0) Turno1.setSelectedIndex(0); // CORRECCIÓN AQUÍ
        if (Tutor.getItemCount() > 0) Tutor.setSelectedIndex(0);
        if (this.Especialidad.getItemCount() > 0) this.Especialidad.setSelectedIndex(0);
        jTable1.clearSelection();
    }

    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Advertencia", JOptionPane.WARNING_MESSAGE);
    }

// NO BORRES NADA DE AQUÍ PARA ABAJO (Aquí va el @SuppressWarnings y el initComponents)
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
        jLabel9 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        NombreGrupo = new javax.swing.JTextField();
        Especialidad = new javax.swing.JComboBox<>();
        Tutor = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        Codigo = new javax.swing.JTextField();
        Capacidad = new javax.swing.JSpinner();
        Semestre = new javax.swing.JSpinner();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel10 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        Turno1 = new javax.swing.JComboBox<>();
        jPanel11 = new javax.swing.JPanel();
        BtnBuscar = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(100, 0, 25));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 160, 0));
        jLabel1.setText("GESTION DE GRUPOS");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(102, 102, 102)
                .addComponent(jLabel1)
                .addContainerGap(765, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(19, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(16, 16, 16))
        );

        Bg.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1050, 60));

        jPanel2.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1050, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 60, 1050, 4));

        jPanel3.setBackground(new java.awt.Color(100, 0, 25));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(204, 160, 0));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("AGREGAR GRUPO");
        jLabel9.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 150, 40));

        jPanel4.setBackground(new java.awt.Color(100, 0, 25));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(204, 160, 0));
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("ACTUALIZAR");
        jLabel10.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 80, -1, -1));

        jPanel5.setBackground(new java.awt.Color(204, 0, 0));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("ELIMINAR");
        jLabel11.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 80, -1, -1));

        jPanel6.setBackground(new java.awt.Color(153, 153, 153));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("LIMPIAR");
        jLabel12.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 80, -1, -1));

        jPanel7.setBackground(new java.awt.Color(70, 0, 15));

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("REGRESAR AL MENU");
        jLabel13.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 80, -1, -1));

        jPanel8.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1030, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 1030, 4));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(100, 0, 25));
        jLabel2.setText("Tutor:");
        Bg.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, -1, -1));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(100, 0, 25));
        jLabel3.setText("Capacidad:");
        Bg.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 210, -1, -1));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(100, 0, 25));
        jLabel4.setText("Especialidad:");
        Bg.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, -1, -1));

        NombreGrupo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NombreGrupoActionPerformed(evt);
            }
        });
        Bg.add(NombreGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 150, 200, -1));

        Especialidad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Especialidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EspecialidadActionPerformed(evt);
            }
        });
        Bg.add(Especialidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 180, 200, -1));

        Tutor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Tutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TutorActionPerformed(evt);
            }
        });
        Bg.add(Tutor, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 240, 200, -1));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(100, 0, 25));
        jLabel5.setText("Nombre del grupo:");
        Bg.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(100, 0, 25));
        jLabel6.setText("Codigo:");
        Bg.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 150, -1, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(100, 0, 25));
        jLabel7.setText("Semestre:");
        Bg.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 180, -1, -1));

        Codigo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CodigoActionPerformed(evt);
            }
        });
        Bg.add(Codigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 150, 290, -1));
        Bg.add(Capacidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 210, 290, -1));
        Bg.add(Semestre, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 180, 290, -1));

        jPanel9.setBackground(new java.awt.Color(204, 160, 0));
        jPanel9.setForeground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1030, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        Bg.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 270, 1030, 4));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Nombre", "Código", "Especialidad", "Semestre", "Turno", "Capacidad"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        Bg.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, 1010, 350));

        jPanel10.setBackground(new java.awt.Color(204, 160, 0));
        jPanel10.setForeground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1050, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 650, 1050, 4));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(100, 0, 25));
        jLabel8.setText("Seleccione un GRUPO de la tabla o complete el formulario");
        Bg.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 660, -1, -1));

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(100, 0, 25));
        jLabel14.setText("Turno:");
        Bg.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, -1, -1));

        Turno1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        Turno1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Turno1ActionPerformed(evt);
            }
        });
        Bg.add(Turno1, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 210, 200, -1));

        jPanel11.setBackground(new java.awt.Color(100, 0, 25));

        BtnBuscar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnBuscar.setForeground(new java.awt.Color(204, 160, 0));
        BtnBuscar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnBuscar.setText("BUSCAR");
        BtnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 80, 150, 40));

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

    private void NombreGrupoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NombreGrupoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NombreGrupoActionPerformed

    private void EspecialidadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EspecialidadActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EspecialidadActionPerformed

    private void TutorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TutorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TutorActionPerformed

    private void CodigoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CodigoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CodigoActionPerformed

    private void Turno1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Turno1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_Turno1ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new GruposWindow().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bg;
    private javax.swing.JLabel BtnBuscar;
    private javax.swing.JSpinner Capacidad;
    private javax.swing.JTextField Codigo;
    private javax.swing.JComboBox<String> Especialidad;
    private javax.swing.JTextField NombreGrupo;
    private javax.swing.JSpinner Semestre;
    private javax.swing.JComboBox<String> Turno1;
    private javax.swing.JComboBox<String> Tutor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
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
