/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;

import horarios.controlador.EspecialidadController;
import horarios.controlador.ModuloController;
import horarios.modelo.Especialidad;
import horarios.modelo.Modulo;
import horarios.util.Colores;
import horarios.util.SoundManager;
import horarios.util.SwingUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  VISTA: ModulosWindow
 *  CAPA:  Vista (MVC)
 * ============================================================
 *  Administracion del catalogo de Modulos de Competencias
 *  laborales (Modulo I a V) de cada especialidad.
 *
 *  Cada especialidad tiene sus propios 5 modulos, uno por
 *  semestre del 2do al 6to (Modulo I=2do ... Modulo V=6to).
 *  Los submodulos (las materias reales que agenda el generador)
 *  se asignan a un modulo desde la seccion de Materias
 *  (Materia.idModulo), no desde aqui.
 *
 *  Ventana escrita a mano (sin editor grafico de NetBeans),
 *  usando las fabricas de componentes de SwingUtils para
 *  mantener el estilo institucional del resto del sistema.
 * ============================================================
 */
public class ModulosWindow extends JFrame {

    private final MainWindow mainWindow;
    private final ModuloController controller = new ModuloController();
    private final EspecialidadController especialidadController = new EspecialidadController();

    private JComboBox<Especialidad> comboEspecialidad;
    private JComboBox<Integer> comboNumero;
    private JComboBox<Integer> comboSemestre;
    private JTextField txtNombre;
    private JTextField txtClave;
    private JTable tabla;
    private DefaultTableModel tableModel;

    public ModulosWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        construirUI();
        cargarEspecialidades();
        cargarDatos();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    public ModulosWindow() {
        this(null);
    }

    private void construirUI() {
        setTitle("Gestión de Módulos — Competencias Laborales");
        SwingUtils.aplicarLAF();

        JPanel bg = new JPanel(new BorderLayout());
        bg.setBackground(Colores.GRIS);
        bg.setPreferredSize(new Dimension(760, 580));

        bg.add(SwingUtils.crearHeader("MÓDULOS DE COMPETENCIAS LABORALES", 48), BorderLayout.NORTH);

        // ── Formulario ──
        JPanel formPanel = new JPanel();
        formPanel.setBackground(Colores.GRIS);
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));
        formPanel.setLayout(new GridLayout(0, 2, 12, 8));

        comboEspecialidad = new JComboBox<>();
        comboNumero = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        comboSemestre = new JComboBox<>(new Integer[]{2, 3, 4, 5, 6});
        txtNombre = SwingUtils.crearCampo();
        txtClave = SwingUtils.crearCampo();

        // Al elegir el numero de modulo, sugerir automaticamente su semestre
        // (Modulo I=2do, II=3ro, III=4to, IV=5to, V=6to), como en el plan de estudios.
        comboNumero.addActionListener(e -> {
            Integer numero = (Integer) comboNumero.getSelectedItem();
            if (numero != null) comboSemestre.setSelectedItem(numero + 1);
        });

        formPanel.add(SwingUtils.crearEtiqueta("Especialidad:"));
        formPanel.add(comboEspecialidad);
        formPanel.add(SwingUtils.crearEtiqueta("Número de módulo (I–V):"));
        formPanel.add(comboNumero);
        formPanel.add(SwingUtils.crearEtiqueta("Semestre en que se cursa:"));
        formPanel.add(comboSemestre);
        formPanel.add(SwingUtils.crearEtiqueta("Nombre del módulo:"));
        formPanel.add(txtNombre);
        formPanel.add(SwingUtils.crearEtiqueta("Clave (opcional):"));
        formPanel.add(txtClave);

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
        tableModel = new DefaultTableModel(
            new Object[]{"ID", "Especialidad", "Módulo", "Semestre", "Nombre", "Clave"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(tableModel);
        SwingUtils.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(0).setMaxWidth(50);
        tabla.getColumnModel().getColumn(2).setMaxWidth(70);
        tabla.getColumnModel().getColumn(3).setMaxWidth(80);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Colores.GRIS);
        centro.add(formYBotones, BorderLayout.NORTH);
        centro.add(scroll,       BorderLayout.CENTER);

        bg.add(centro, BorderLayout.CENTER);
        bg.add(SwingUtils.crearBarraEstado(
            "Cada especialidad tiene 5 módulos (I–V), del 2do al 6to semestre"), BorderLayout.SOUTH);

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

    private void cargarEspecialidades() {
        DefaultComboBoxModel<Especialidad> modelo = new DefaultComboBoxModel<>();
        for (Especialidad esp : especialidadController.obtenerTodas()) modelo.addElement(esp);
        comboEspecialidad.setModel(modelo);
    }

    private Modulo leerFormulario() {
        Especialidad esp = (Especialidad) comboEspecialidad.getSelectedItem();
        Modulo m = new Modulo();
        m.setIdEspecialidad(esp != null ? esp.getId() : 0);
        m.setNumero((Integer) comboNumero.getSelectedItem());
        m.setIdSemestre((Integer) comboSemestre.getSelectedItem());
        m.setNombre(txtNombre.getText().trim());
        String clave = txtClave.getText().trim();
        m.setClave(clave.isEmpty() ? null : clave);
        return m;
    }

    private void agregar() {
        String error = controller.agregar(leerFormulario());
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Módulo agregado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona un módulo para actualizar."); return; }
        Modulo m = leerFormulario();
        m.setIdModulo((Integer) tableModel.getValueAt(row, 0));
        String error = controller.actualizar(m);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Módulo actualizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = tabla.getSelectedRow();
        if (row == -1) { avisar("Selecciona un módulo para eliminar."); return; }
        int idModulo = (Integer) tableModel.getValueAt(row, 0);
        String nombre = (String) tableModel.getValueAt(row, 4);
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el módulo \"" + nombre + "\"?\n" +
            "Las materias (submódulos) que lo tengan asignado quedarán sin módulo, no se borran.",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String error = controller.eliminar(idModulo);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Módulo eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                cargarDatos();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiar() {
        if (comboEspecialidad.getItemCount() > 0) comboEspecialidad.setSelectedIndex(0);
        comboNumero.setSelectedIndex(0);
        comboSemestre.setSelectedIndex(0);
        txtNombre.setText("");
        txtClave.setText("");
        tabla.clearSelection();
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        List<Modulo> todos = controller.obtenerTodos();
        for (Modulo m : todos) {
            String[] romanos = {"", "I", "II", "III", "IV", "V"};
            String numRomano = (m.getNumero() >= 1 && m.getNumero() <= 5) ? romanos[m.getNumero()] : String.valueOf(m.getNumero());
            tableModel.addRow(new Object[]{
                m.getIdModulo(), m.getEspecialidadNombre(), numRomano, m.getIdSemestre(), m.getNombre(), m.getClave()
            });
        }
    }

    private void cargarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return;
        String especialidadNombre = (String) tableModel.getValueAt(row, 1);
        for (int i = 0; i < comboEspecialidad.getItemCount(); i++) {
            if (comboEspecialidad.getItemAt(i).getNombre().equals(especialidadNombre)) {
                comboEspecialidad.setSelectedIndex(i);
                break;
            }
        }
        String numRomano = (String) tableModel.getValueAt(row, 2);
        String[] romanos = {"", "I", "II", "III", "IV", "V"};
        for (int i = 1; i <= 5; i++) {
            if (romanos[i].equals(numRomano)) { comboNumero.setSelectedItem(i); break; }
        }
        comboSemestre.setSelectedItem(tableModel.getValueAt(row, 3));
        txtNombre.setText((String) tableModel.getValueAt(row, 4));
        Object clave = tableModel.getValueAt(row, 5);
        txtClave.setText(clave != null ? clave.toString() : "");
    }

    private void avisar(String msg) {
        SoundManager.playError();
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
