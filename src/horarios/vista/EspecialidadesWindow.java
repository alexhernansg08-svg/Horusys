/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;
import horarios.controlador.EspecialidadController;
import horarios.util.SoundManager;
import horarios.modelo.Especialidad;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/**
 *
 * @author axelp
 */
public class EspecialidadesWindow extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(EspecialidadesWindow.class.getName());

    /**
     * Creates new form EspecialidadesWindow
     */
    private MainWindow mainWindow;
    private int userId;
    private final EspecialidadController controller = new EspecialidadController();
    private DefaultTableModel tableModel;

    public EspecialidadesWindow(MainWindow mainWindow, int userId) {
        this.mainWindow = mainWindow;
        this.userId = userId;
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    public EspecialidadesWindow() {
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    private void initCustomListeners() {
        tableModel = (DefaultTableModel) jTable1.getModel();
        
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccionado();
        });

        jTextField2.setText("");
        jTextField1.setText("");

        jLabel5.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { agregar(); }
        });

        jLabel6.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { actualizar(); }
        });

        jLabel7.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { eliminar(); }
        });

        jLabel8.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { limpiar(); }
        });

        jLabel9.addMouseListener(new MouseAdapter() {
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
            "Ingrese el nombre de la especialidad a buscar:", txtBusqueda
        };
        
        // Personalizamos los botones
        Object[] opciones = {"Buscar", "Cancelar"};

        // Mostramos la ventana emergente
        int opcionElegida = JOptionPane.showOptionDialog(this, mensaje, "Buscar Especialidad",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones);

        // Si el usuario presionó "Buscar" (la opción 0)
        if (opcionElegida == 0) {
            String termino = txtBusqueda.getText().trim();
            
            if (!termino.isEmpty()) {
                // Llamamos al controlador para buscar
                java.util.List<Especialidad> resultados = controller.buscarPorNombre(termino);
                cargarResultadosBusqueda(resultados);
                
                // Si no hay resultados, avisamos al usuario
                if (resultados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron especialidades con ese nombre.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // Si dejó el texto vacío y le dio a buscar, cargamos toda la tabla de nuevo
                cargarDatos();
            }
        }
    }

    // Este método limpia la tabla y mete solo los que encontramos en la búsqueda
    private void cargarResultadosBusqueda(java.util.List<Especialidad> lista) {
        tableModel.setRowCount(0); // Limpiar la tabla
        for (Especialidad e : lista) {
            tableModel.addRow(new Object[]{e.getId(), e.getNombre(), e.getCodigo()});
        }
    }

    private void agregar() {
        String error = controller.agregar(jTextField2.getText().trim(), jTextField1.getText().trim());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Especialidad agregada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona una especialidad para actualizar."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        String error = controller.actualizar(id, jTextField2.getText().trim(), jTextField1.getText().trim());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Especialidad actualizada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona una especialidad para eliminar."); return; }
        String nombre = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la especialidad \"" + nombre + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) tableModel.getValueAt(row, 0);
            String error = controller.eliminar(id);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Especialidad eliminada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                cargarDatos();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        for (Especialidad e : controller.obtenerTodas()) {
            tableModel.addRow(new Object[]{e.getId(), e.getNombre(), e.getCodigo()});
        }
    }

    private void cargarSeleccionado() {
        int row = jTable1.getSelectedRow();
        if (row >= 0) {
            jTextField2.setText((String) tableModel.getValueAt(row, 1)); // Nombre
            jTextField1.setText((String) tableModel.getValueAt(row, 2)); // Código
        }
    }

    private void limpiar() {
        jTextField2.setText("");
        jTextField1.setText("");
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
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel10 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        BtnBuscar = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(null);
        setMinimumSize(null);
        setResizable(false);

        Bg.setMaximumSize(new java.awt.Dimension(910, 656));
        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(100, 0, 25));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 160, 0));
        jLabel1.setText("GESTION DE ESPECIALIDADES");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(92, 92, 92)
                .addComponent(jLabel1)
                .addContainerGap(613, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(21, 21, 21))
        );

        Bg.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 960, 70));

        jPanel2.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 960, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 960, 4));

        jPanel3.setBackground(new java.awt.Color(100, 0, 25));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(204, 160, 0));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("AGREGAR");
        jLabel5.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, 140, 40));

        jPanel4.setBackground(new java.awt.Color(100, 0, 25));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(204, 160, 0));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("ACTUALIZAR");
        jLabel6.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 90, -1, -1));

        jPanel5.setBackground(new java.awt.Color(204, 0, 0));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("ELIMINAR");
        jLabel7.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 90, -1, -1));

        jPanel6.setBackground(new java.awt.Color(153, 153, 153));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("LIMPIAR");
        jLabel8.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 90, -1, -1));

        jPanel7.setBackground(new java.awt.Color(70, 0, 15));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("REGRESAR AL MENU");
        jLabel9.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 90, -1, -1));

        jPanel8.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 940, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 940, 4));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(100, 0, 25));
        jLabel2.setText("Código:");
        Bg.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, -1, -1));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(100, 0, 25));
        jLabel3.setText("Nombre de la especialidad:");
        Bg.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 160, -1, -1));

        jTextField1.setText("jTextField1");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        Bg.add(jTextField1, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 190, 390, -1));

        jTextField2.setText("jTextField1");
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });
        Bg.add(jTextField2, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 160, 390, -1));

        jPanel9.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 940, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 274, 940, 4));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "ID", "Nombre", "Código"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        Bg.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 290, 940, 310));

        jPanel10.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 950, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 614, 950, 4));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(100, 0, 25));
        jLabel4.setText("Seleccione una especialidad de la tabla o complete el formulario");
        Bg.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 640, -1, -1));

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
            .addComponent(BtnBuscar, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(BtnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        Bg.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 90, 130, 40));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(Bg, javax.swing.GroupLayout.PREFERRED_SIZE, 960, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Bg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new EspecialidadesWindow().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bg;
    private javax.swing.JLabel BtnBuscar;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
