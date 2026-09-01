/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;

import horarios.controlador.CategoriaDocenteController;
import horarios.modelo.CategoriaDocente;
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
 *  VISTA: CategoriasDocenteWindow
 *  CAPA:  Vista (MVC)
 * ============================================================
 *  Administracion del catalogo de categorias del docente
 *  (Tiempo Completo, Tres Cuartos de Tiempo, Medio Tiempo,
 *  Por Asignatura, etc.).
 *
 *  La PK real es "clave" (tipo clave presupuestal SEP, ej. "E4853"),
 *  no un id autoincremental: puede haber varias filas con la misma
 *  descripcion pero distinta clave. Cada categoria trae sus propias
 *  horas_frente_grupo y horas_actividades_complementarias, que usa
 *  directamente el generador de horarios (HorarioDAO).
 *
 *  Ventana escrita a mano (sin editor grafico de NetBeans),
 *  usando las fabricas de componentes de SwingUtils para
 *  mantener el estilo institucional del resto del sistema.
 * ============================================================
 */
public class CategoriasDocenteWindow extends JFrame {

    private final MainWindow mainWindow;
    private final CategoriaDocenteController controller = new CategoriaDocenteController();

    private JTextField txtClave;
    private JTextField txtDescripcion;
    private JSpinner spnHorasFrenteGrupo;
    private JSpinner spnHorasActividades;
    private JTable tabla;
    private DefaultTableModel tableModel;

    public CategoriasDocenteWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        construirUI();
        cargarDatos();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    public CategoriasDocenteWindow() {
        this(null);
    }

    private void construirUI() {
        setTitle("Gestión de Categorías del Docente");
        SwingUtils.aplicarLAF();

        JPanel bg = new JPanel(new BorderLayout());
        bg.setBackground(Colores.GRIS);
        bg.setPreferredSize(new Dimension(640, 540));

        bg.add(SwingUtils.crearHeader("CATEGORÍAS DEL DOCENTE", 48), BorderLayout.NORTH);

        // ── Formulario ──
        JPanel formPanel = new JPanel();
        formPanel.setBackground(Colores.GRIS);
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        JLabel lblClave = SwingUtils.crearEtiqueta("Clave (Ej. E4853):");
        lblClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtClave = SwingUtils.crearCampo();
        txtClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtClave.setMaximumSize(new Dimension(2000, 32));

        JLabel lblDescripcion = SwingUtils.crearEtiqueta("Descripción (Ej. Tiempo Completo):");
        lblDescripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtDescripcion = SwingUtils.crearCampo();
        txtDescripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtDescripcion.setMaximumSize(new Dimension(2000, 32));

        JLabel lblHorasGrupo = SwingUtils.crearEtiqueta("Horas frente a grupo (por semana):");
        lblHorasGrupo.setAlignmentX(Component.LEFT_ALIGNMENT);
        spnHorasFrenteGrupo = new JSpinner(new SpinnerNumberModel(20, 0, 60, 1));
        spnHorasFrenteGrupo.setAlignmentX(Component.LEFT_ALIGNMENT);
        spnHorasFrenteGrupo.setMaximumSize(new Dimension(120, 32));

        JLabel lblHorasAct = SwingUtils.crearEtiqueta("Tope de horas para actividades complementarias:");
        lblHorasAct.setAlignmentX(Component.LEFT_ALIGNMENT);
        spnHorasActividades = new JSpinner(new SpinnerNumberModel(0, 0, 40, 1));
        spnHorasActividades.setAlignmentX(Component.LEFT_ALIGNMENT);
        spnHorasActividades.setMaximumSize(new Dimension(120, 32));

        formPanel.add(lblClave);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtClave);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(lblDescripcion);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtDescripcion);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(lblHorasGrupo);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(spnHorasFrenteGrupo);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(lblHorasAct);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(spnHorasActividades);

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
        tableModel = new DefaultTableModel(new Object[]{"Clave", "Descripción", "Hrs. frente a grupo", "Hrs. actividades"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tableModel);
        SwingUtils.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(0).setMaxWidth(90);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Colores.GRIS);
        centro.add(formYBotones, BorderLayout.NORTH);
        centro.add(scroll,       BorderLayout.CENTER);

        bg.add(centro, BorderLayout.CENTER);
        bg.add(SwingUtils.crearBarraEstado("Selecciona una categoría de la tabla o completa el formulario"), BorderLayout.SOUTH);

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

    private void agregar() {
        String error = controller.agregar(
            txtClave.getText(), txtDescripcion.getText(),
            (Integer) spnHorasFrenteGrupo.getValue(), (Integer) spnHorasActividades.getValue());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Categoría agregada correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona una categoría para actualizar."); return; }
        String clave = (String) tableModel.getValueAt(row, 0);
        String error = controller.actualizar(
            clave, txtDescripcion.getText(),
            (Integer) spnHorasFrenteGrupo.getValue(), (Integer) spnHorasActividades.getValue());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Categoría actualizada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona una categoría para eliminar."); return; }
        String clave = (String) tableModel.getValueAt(row, 0);
        String descripcion = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la categoría \"" + descripcion + "\" (" + clave + ")?\n" +
            "Los profesores que la tengan asignada quedarán sin categoría.",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String error = controller.eliminar(clave);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Categoría eliminada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
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
        txtDescripcion.setText("");
        spnHorasFrenteGrupo.setValue(20);
        spnHorasActividades.setValue(0);
        tabla.clearSelection();
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        for (CategoriaDocente c : controller.obtenerTodas()) {
            tableModel.addRow(new Object[]{
                c.getClave(), c.getDescripcion(),
                c.getHorasFrenteGrupo(), c.getHorasActividadesComplementarias()
            });
        }
    }

    private void cargarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return;
        txtClave.setText((String) tableModel.getValueAt(row, 0));
        txtDescripcion.setText((String) tableModel.getValueAt(row, 1));
        spnHorasFrenteGrupo.setValue(tableModel.getValueAt(row, 2));
        spnHorasActividades.setValue(tableModel.getValueAt(row, 3));
    }

    private void avisar(String msg) {
        SoundManager.playError();
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
