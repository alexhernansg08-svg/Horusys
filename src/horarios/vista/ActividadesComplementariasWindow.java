/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;

import horarios.controlador.ActividadComplementariaController;
import horarios.modelo.ActividadComplementaria;
import horarios.util.Colores;
import horarios.util.SoundManager;
import horarios.util.SwingUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  VISTA: ActividadesComplementariasWindow
 *  CAPA:  Vista (MVC)
 * ============================================================
 *  Administracion del catalogo de actividades complementarias
 *  con las que un docente completa sus horas frente a grupo.
 *  Catalogo real ya existente en BD: clave, tipo_actividad,
 *  actividad, horas (15 registros precargados: Tutorías,
 *  Junta de academia, Proyecto de investigación, etc.).
 *
 *  Ventana escrita a mano (sin editor grafico de NetBeans),
 *  usando las fabricas de componentes de SwingUtils.
 * ============================================================
 */
public class ActividadesComplementariasWindow extends JFrame {

    private final MainWindow mainWindow;
    private final ActividadComplementariaController controller = new ActividadComplementariaController();

    private JTextField txtClave;
    private JTextField txtTipoActividad;
    private JTextField txtActividad;
    private JSpinner spnHoras;
    private JTable tabla;
    private DefaultTableModel tableModel;

    public ActividadesComplementariasWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        construirUI();
        cargarDatos();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    public ActividadesComplementariasWindow() {
        this(null);
    }

    private void construirUI() {
        setTitle("Gestión de Actividades Complementarias");
        SwingUtils.aplicarLAF();

        JPanel bg = new JPanel(new BorderLayout());
        bg.setBackground(Colores.GRIS);
        bg.setPreferredSize(new Dimension(760, 560));

        bg.add(SwingUtils.crearHeader("ACTIVIDADES COMPLEMENTARIAS", 48), BorderLayout.NORTH);

        // ── Formulario (fila 1: clave + tipo, fila 2: actividad + horas) ──
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        fila1.setBackground(Colores.GRIS);
        txtClave = SwingUtils.crearCampo();
        txtClave.setPreferredSize(new Dimension(80, 30));
        txtTipoActividad = SwingUtils.crearCampo();
        txtTipoActividad.setPreferredSize(new Dimension(220, 30));
        fila1.add(etiquetaCon("Clave:", txtClave));
        fila1.add(etiquetaCon("Tipo de actividad:", txtTipoActividad));

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        fila2.setBackground(Colores.GRIS);
        txtActividad = SwingUtils.crearCampo();
        txtActividad.setPreferredSize(new Dimension(320, 30));
        spnHoras = new JSpinner(new SpinnerNumberModel(2, 1, 40, 1));
        spnHoras.setPreferredSize(new Dimension(70, 30));
        fila2.add(etiquetaCon("Actividad:", txtActividad));
        fila2.add(etiquetaCon("Horas:", spnHoras));

        JPanel formPanel = new JPanel();
        formPanel.setBackground(Colores.GRIS);
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(fila1);
        formPanel.add(fila2);

        // ── Botones ──
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        botones.setBackground(Colores.GRIS);
        JButton btnAgregar    = SwingUtils.crearBoton("AGREGAR",    Colores.VERDE, Color.WHITE);
        JButton btnActualizar = SwingUtils.crearBoton("ACTUALIZAR", Colores.VINO,  Color.WHITE);
        JButton btnEliminar   = SwingUtils.crearBoton("ELIMINAR",   Colores.ROJO,  Color.WHITE);
        JButton btnLimpiar    = SwingUtils.crearBoton("LIMPIAR",    new Color(153, 153, 153), Color.WHITE);
        botones.add(btnAgregar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);

        JPanel formYBotones = new JPanel(new BorderLayout());
        formYBotones.setBackground(Colores.GRIS);
        formYBotones.add(formPanel, BorderLayout.NORTH);
        formYBotones.add(botones,   BorderLayout.SOUTH);

        // ── Tabla ──
        tableModel = new DefaultTableModel(new Object[]{"Clave", "Tipo", "Actividad", "Horas"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tableModel);
        SwingUtils.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getColumnModel().getColumn(3).setMaxWidth(70);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Colores.GRIS);
        centro.add(formYBotones, BorderLayout.NORTH);
        centro.add(scroll,       BorderLayout.CENTER);

        bg.add(centro, BorderLayout.CENTER);
        bg.add(SwingUtils.crearBarraEstado("Selecciona una actividad de la tabla o completa el formulario"), BorderLayout.SOUTH);

        setContentPane(bg);

        // ── Listeners ──
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) cargarSeleccionado();
        });
        btnAgregar.addActionListener(e -> agregar());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> limpiar());
    }

    private JPanel etiquetaCon(String texto, JComponent campo) {
        JPanel p = new JPanel();
        p.setBackground(Colores.GRIS);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = SwingUtils.crearEtiqueta(texto);
        p.add(lbl);
        p.add(campo);
        return p;
    }

    private void agregar() {
        String error = controller.agregar(txtClave.getText(), txtTipoActividad.getText(),
                txtActividad.getText(), (Integer) spnHoras.getValue());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Actividad agregada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona una actividad para actualizar."); return; }
        String error = controller.actualizar(txtClave.getText(), txtTipoActividad.getText(),
                txtActividad.getText(), (Integer) spnHoras.getValue());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Actividad actualizada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona una actividad para eliminar."); return; }
        String actividad = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la actividad \"" + actividad + "\"?\n" +
            "Se quitará de todos los profesores que la tengan asignada.",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String clave = (String) tableModel.getValueAt(row, 0);
            String error = controller.eliminar(clave);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Actividad eliminada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                cargarDatos();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiar() {
        txtClave.setText("");
        txtClave.setEditable(true);
        txtTipoActividad.setText("");
        txtActividad.setText("");
        spnHoras.setValue(2);
        tabla.clearSelection();
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        for (ActividadComplementaria a : controller.obtenerTodas()) {
            tableModel.addRow(new Object[]{a.getClave(), a.getTipoActividad(), a.getActividad(), a.getHoras()});
        }
    }

    private void cargarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return;
        txtClave.setText((String) tableModel.getValueAt(row, 0));
        txtClave.setEditable(false); // la clave es la PK: no se cambia al editar, solo al agregar una nueva
        txtTipoActividad.setText((String) tableModel.getValueAt(row, 1));
        txtActividad.setText((String) tableModel.getValueAt(row, 2));
        spnHoras.setValue(tableModel.getValueAt(row, 3));
    }

    private void avisar(String msg) {
        SoundManager.playError();
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
