/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.vista;
import horarios.controlador.ActividadComplementariaController;
import horarios.controlador.CategoriaDocenteController;
import horarios.controlador.GrupoController;
import horarios.controlador.MateriaController;
import horarios.controlador.ProfesorController;
import horarios.modelo.ActividadComplementaria;
import horarios.modelo.CategoriaDocente;
import horarios.modelo.Grupo;
import horarios.modelo.Materia;
import horarios.modelo.Profesor;
import horarios.util.Colores;
import horarios.util.SoundManager;
import horarios.util.SwingUtils;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author axelp
 */
public class ProfesoresWindow extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ProfesoresWindow.class.getName());

    /**
     * Creates new form ProfesoresWindow
     */
// ── Variables ──
    private MainWindow mainWindow;
    private int userId;
    private final ProfesorController controller = new ProfesorController();
    private final MateriaController materiaController = new MateriaController();
    private final GrupoController grupoController = new GrupoController();
    private final CategoriaDocenteController categoriaController = new CategoriaDocenteController();
    private final ActividadComplementariaController actividadController = new ActividadComplementariaController();
    private DefaultTableModel tableModel;
    
    private List<Profesor.DisponibilidadBloque> disponibilidadActual = new ArrayList<>();

    // ── Actividad complementaria seleccionada para el profesor actual en el formulario.
    //    Ya no es una lista de checkboxes (el profesor solo puede tener UNA actividad,
    //    columna directa profesor.actividad_complementaria_clave): se guarda su clave aqui. ──
    private String actividadComplementariaClaveActual = null;

    public ProfesoresWindow(MainWindow mainWindow, int userId) {
        this.mainWindow = mainWindow;
        this.userId = userId;
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        initCustomListeners();
        cargarDatos();
        pack();
        setLocationRelativeTo(null);
    }

    public ProfesoresWindow() {
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
            if (!e.getValueIsAdjusting() && jTable1.getSelectedRow() >= 0) cargarSeleccionado();
        });

        PregradoOTitulo.setModel(new DefaultComboBoxModel<>(new String[]{"Licenciatura","Maestría","Doctorado","Ingeniero","Otro"}));
        cargarCategorias();

        RFC.setText("");
        Nombre.setText("");
        Apellidos.setText("");
        Email.setText("");
        Telefono.setText("");

        BtnAgregar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { agregar(); } });
        BtnActualizar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { actualizar(); } });
        BtnEliminar.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { eliminar(); } });
        BtnDisponibilidad.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { abrirDialogoDisponibilidad(); } });
        BtnAsignarMateria.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { asignarMaterias(); } });
        BtnActividades.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { abrirDialogoActividades(); } });
        LinkAdminCategorias.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new CategoriasDocenteWindow(mainWindow).setVisible(true);
            }
        });
        LinkAdminActividades.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new ActividadesComplementariasWindow(mainWindow).setVisible(true);
            }
        });
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

    /**
     * Carga el catalogo de categorias del docente en el combo.
     * Se llama al iniciar la ventana y cada vez que se guarda un profesor,
     * por si se agrego una categoria nueva desde la ventana de administracion.
     */
    private void cargarCategorias() {
        CategoriaDocente seleccionActual = (CategoriaDocente) ComboCategoria.getSelectedItem();
        DefaultComboBoxModel<CategoriaDocente> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(new CategoriaDocente(null, "(Sin categoría)", 0, 0));
        for (CategoriaDocente c : categoriaController.obtenerTodas()) modelo.addElement(c);
        ComboCategoria.setModel(modelo);
        if (seleccionActual != null) {
            for (int i = 0; i < modelo.getSize(); i++) {
                if (java.util.Objects.equals(modelo.getElementAt(i).getClave(), seleccionActual.getClave())) {
                    ComboCategoria.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /** Selecciona en el combo la categoria con la clave dada (null = "Sin categoría"). */
    private void seleccionarCategoria(String categoriaClave) {
        for (int i = 0; i < ComboCategoria.getItemCount(); i++) {
            if (java.util.Objects.equals(ComboCategoria.getItemAt(i).getClave(), categoriaClave)) {
                ComboCategoria.setSelectedIndex(i);
                return;
            }
        }
        ComboCategoria.setSelectedIndex(0);
    }
    
    private void buscar() {
        // Creamos un campo de texto para la mini-ventana
        javax.swing.JTextField txtBusqueda = new javax.swing.JTextField(20);
        Object[] mensaje = {
            "Ingrese el nombre o apellidos del profesor a buscar:", txtBusqueda
        };
        
        // Personalizamos los botones
        Object[] opciones = {"Buscar", "Cancelar"};

        // Mostramos la ventana emergente
        int opcionElegida = JOptionPane.showOptionDialog(this, mensaje, "Buscar Profesor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones);

        // Si el usuario presionó "Buscar"
        if (opcionElegida == 0) {
            String termino = txtBusqueda.getText().trim();
            
            if (!termino.isEmpty()) {
                // Llamamos al controlador para buscar
                java.util.List<Profesor> resultados = controller.buscarPorNombre(termino);
                cargarResultadosBusqueda(resultados);
                
                // Si no hay resultados, avisamos al usuario
                if (resultados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron profesores con ese nombre o apellido.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // Si dejó el texto vacío y le dio a buscar, cargamos toda la tabla de nuevo
                cargarDatos();
            }
        }
    }

    // Este método limpia la tabla y mete solo los profesores encontrados
    private void cargarResultadosBusqueda(java.util.List<Profesor> lista) {
        tableModel.setRowCount(0); // Limpiar la tabla
        for (Profesor p : lista) {
            tableModel.addRow(new Object[]{
                p.getId(), p.getRfc(),
                p.getNombre() + " " + (p.getApellidos() != null ? p.getApellidos() : ""),
                p.getEmail(),
                p.getCategoriaDescripcion() != null ? p.getCategoriaDescripcion() : "—"
            });
        }
    }

    private void agregar() {
        Profesor p = leerFormulario();
        String error = controller.agregar(p);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Profesor agregado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        Profesor p = leerFormulario();
        String error = controller.actualizar(p);
        if (error == null) {
            SoundManager.playSuccess();
            JOptionPane.showMessageDialog(this, "Profesor actualizado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiar();
            cargarDatos();
        } else {
            SoundManager.playError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        String rfc = RFC.getText().trim();
        if (rfc.isEmpty()) { avisar("Ingrese un RFC para eliminar."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar profesor con RFC " + rfc + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String error = controller.eliminar(rfc);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Profesor eliminado", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiar();
                cargarDatos();
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Profesor leerFormulario() {
        Profesor p = new Profesor();
        p.setRfc(RFC.getText().trim());
        p.setNombre(Nombre.getText().trim());
        p.setApellidos(Apellidos.getText().trim());
        p.setEmail(Email.getText().trim());
        p.setTelefono(Telefono.getText().trim());
        p.setPregrado((String) PregradoOTitulo.getSelectedItem());

        CategoriaDocente cat = (CategoriaDocente) ComboCategoria.getSelectedItem();
        p.setCategoriaClave(cat != null ? cat.getClave() : null);
        p.setActividadComplementariaClave(actividadComplementariaClaveActual);

        p.setDisponibilidad(new ArrayList<>(disponibilidadActual));
        return p;
    }

    private void cargarDatos() {
        tableModel.setRowCount(0);
        for (Profesor p : controller.obtenerTodos()) {
            tableModel.addRow(new Object[]{
                p.getId(), p.getRfc(),
                p.getNombre() + " " + (p.getApellidos() != null ? p.getApellidos() : ""),
                p.getEmail(),
                p.getCategoriaDescripcion() != null ? p.getCategoriaDescripcion() : "—"
            });
        }
    }

    private void cargarSeleccionado() {
        int row = jTable1.getSelectedRow();
        if (row < 0) return;
        String rfc = (String) tableModel.getValueAt(row, 1);
        Profesor p = controller.buscarPorRfc(rfc);
        if (p == null) return;
        
        RFC.setText(p.getRfc() != null ? p.getRfc() : "");
        Nombre.setText(p.getNombre() != null ? p.getNombre() : "");
        Apellidos.setText(p.getApellidos() != null ? p.getApellidos() : "");
        Email.setText(p.getEmail() != null ? p.getEmail() : "");
        Telefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
        PregradoOTitulo.setSelectedItem(p.getPregrado() != null ? p.getPregrado() : "Licenciatura");
        seleccionarCategoria(p.getCategoriaClave());
        disponibilidadActual = new ArrayList<>(p.getDisponibilidad());
        actividadComplementariaClaveActual = p.getActividadComplementariaClave();
    }

    private void limpiar() {
        RFC.setText(""); Nombre.setText(""); Apellidos.setText("");
        Email.setText(""); Telefono.setText("");
        if (PregradoOTitulo.getItemCount() > 0) PregradoOTitulo.setSelectedIndex(0);
        if (ComboCategoria.getItemCount() > 0) ComboCategoria.setSelectedIndex(0);
        disponibilidadActual.clear();
        actividadComplementariaClaveActual = null;
        jTable1.clearSelection();
    }

    private void abrirDialogoDisponibilidad() {
        JDialog dialog = new JDialog(this, "Gestionar Disponibilidad", true);
        dialog.setSize(880, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));

        javax.swing.JPanel dHeader = new javax.swing.JPanel(new BorderLayout());
        dHeader.setBackground(Colores.VINO);
        dHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 15, 8, 15));
        javax.swing.JLabel titulo = new javax.swing.JLabel("DISPONIBILIDAD DEL PROFESOR");
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        titulo.setForeground(Colores.AMARILLO);
        dHeader.add(titulo, BorderLayout.WEST);
        javax.swing.JPanel linea = new javax.swing.JPanel();
        linea.setBackground(Colores.AMARILLO);
        linea.setPreferredSize(new Dimension(780, 3));
        javax.swing.JPanel headerWrap = new javax.swing.JPanel(new BorderLayout());
        headerWrap.add(dHeader, BorderLayout.CENTER);
        headerWrap.add(linea,   BorderLayout.SOUTH);

        javax.swing.JPanel addPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addPanel.setBackground(Colores.GRIS);
        addPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, Colores.AMARILLO));

        JComboBox<String> comboDia    = new JComboBox<>(new String[]{"Lunes","Martes","Miércoles","Jueves","Viernes"});
        JComboBox<String> comboInicio = new JComboBox<>(new String[]{"07:00","08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00"});
        JComboBox<String> comboFin    = new JComboBox<>(new String[]{"08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"});
        SwingUtils.estilizarCombo(comboDia);
        SwingUtils.estilizarCombo(comboInicio);
        SwingUtils.estilizarCombo(comboFin);

        addPanel.add(SwingUtils.crearEtiqueta("Día:"));     addPanel.add(comboDia);
        addPanel.add(SwingUtils.crearEtiqueta("Inicio:"));  addPanel.add(comboInicio);
        addPanel.add(SwingUtils.crearEtiqueta("Fin:"));     addPanel.add(comboFin);

        JButton btnAgregarBloque = SwingUtils.crearBoton("AGREGAR BLOQUE", Colores.VERDE, Color.WHITE);
        JButton btnEliminarBloque = SwingUtils.crearBoton("ELIMINAR SELECCIONADO", new Color(180, 0, 0), Color.WHITE);
        addPanel.add(btnAgregarBloque);
        addPanel.add(btnEliminarBloque);

        String[] cols = {"Día","Hora Inicio","Hora Fin","Duración"};
        DefaultTableModel modeloDisp = new DefaultTableModel(cols, 0);
        javax.swing.JTable tablaDisp = new javax.swing.JTable(modeloDisp);
        tablaDisp.setRowHeight(32);
        tablaDisp.setFont(new Font("Arial", Font.PLAIN, 13));
        JTableHeader dh = tablaDisp.getTableHeader();
        dh.setBackground(Colores.VINO);
        dh.setForeground(Color.WHITE);
        dh.setFont(new Font("Arial", Font.BOLD, 13));
        tablaDisp.setSelectionBackground(Colores.AMARILLO);
        tablaDisp.setSelectionForeground(Colores.VINO);
        refrescarTablaDisponibilidad(modeloDisp);

        JScrollPane scroll = new JScrollPane(tablaDisp);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Colores.VINO, 2));

        JButton btnGuardar = SwingUtils.crearBoton("GUARDAR EN BASE DE DATOS", Colores.VERDE, Color.WHITE);
        btnGuardar.setPreferredSize(new Dimension(230, 34));
        JButton btnCerrar  = SwingUtils.crearBoton("CERRAR", new Color(80,80,80), Color.WHITE);

        javax.swing.JPanel bottom = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottom.setBackground(Colores.GRIS);
        bottom.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 0, 0, 0, Colores.AMARILLO));
        bottom.add(btnGuardar);
        bottom.add(btnCerrar);

        javax.swing.JPanel norte = new javax.swing.JPanel(new BorderLayout());
        norte.add(headerWrap, BorderLayout.NORTH);
        norte.add(addPanel,   BorderLayout.SOUTH);
        dialog.add(norte,  BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        btnAgregarBloque.addActionListener(e -> {
            String dia    = (String) comboDia.getSelectedItem();
            String inicio = (String) comboInicio.getSelectedItem();
            String fin    = (String) comboFin.getSelectedItem();
            if (inicio.compareTo(fin) >= 0) {
                SoundManager.playError();
                JOptionPane.showMessageDialog(dialog, "La hora de inicio debe ser menor que la hora de fin", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            disponibilidadActual.add(new Profesor.DisponibilidadBloque(dia, inicio, fin));
            refrescarTablaDisponibilidad(modeloDisp);
        });

        btnGuardar.addActionListener(e -> {
            int row = jTable1.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Selecciona un profesor en la tabla principal primero."); return; }
            String rfc = (String) tableModel.getValueAt(row, 1);
            Profesor p = controller.buscarPorRfc(rfc);
            if (p != null && controller.guardarDisponibilidad(p.getId(), disponibilidadActual)) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(dialog, "¡Disponibilidad guardada correctamente!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(dialog, "Error al guardar la disponibilidad", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
//Btn agregado de eliminar
        btnEliminarBloque.addActionListener(e -> {
            int fila = tablaDisp.getSelectedRow();
            if (fila == -1) {
            SoundManager.playError();
            JOptionPane.showMessageDialog(dialog, "Selecciona un bloque de la tabla para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
    

            disponibilidadActual.remove(fila);
    

            refrescarTablaDisponibilidad(modeloDisp);
        });

        btnCerrar.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void refrescarTablaDisponibilidad(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        for (Profesor.DisponibilidadBloque b : disponibilidadActual)
            modelo.addRow(new Object[]{b.getDia(), b.getHoraInicio(), b.getHoraFin(), b.getDuracionHoras() + " horas"});
    }

    /**
     * Abre un dialogo de checkboxes para asignar al profesor las materias que puede
     * impartir. Ya NO permite restringir a grupos especificos por materia: la tabla
     * real profesor_materia solo tiene (profesor_id, materia_id) — un profesor asignado
     * a una materia puede impartirla a CUALQUIER grupo elegible (el generador de
     * horarios decide a cuales, repartiendo la carga entre los profesores disponibles).
     */
    private void asignarMaterias() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona un profesor primero."); return; }
        String rfc = (String) tableModel.getValueAt(row, 1);
        Profesor profesor = controller.buscarPorRfc(rfc);
        if (profesor == null) return;

        List<Materia> todas = materiaController.obtenerTodas();
        List<Integer> asignadas = controller.obtenerMateriasAsignadas(profesor.getId());

        // Calcular horas ya asignadas
        int horasYaAsignadas = 0;
        for (Materia m : todas) {
            if (asignadas.contains(m.getIdMateria())) {
                horasYaAsignadas += m.getHorasSemanales();
            }
        }

        int horasMax = profesor.getCategoriaHorasFrenteGrupo() != null ? profesor.getCategoriaHorasFrenteGrupo() : 0;
        int[] horasRestantes = { horasMax - horasYaAsignadas };

        javax.swing.JLabel lblHoras = new javax.swing.JLabel(
            "Horas disponibles: " + horasRestantes[0] + " / " + horasMax
        );
        lblHoras.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHoras.setForeground(Colores.VINO);
        lblHoras.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));

        javax.swing.JPanel panel = new javax.swing.JPanel(new GridLayout(0, 1, 5, 5));
        JCheckBox[] checks = new JCheckBox[todas.size()];

        for (int i = 0; i < todas.size(); i++) {
            Materia m = todas.get(i);
            final int horas = m.getHorasSemanales();

            checks[i] = new JCheckBox(m.getNombre() + " (" + m.getClave() + ") - " + horas + " hrs");
            checks[i].setFont(new Font("Arial", Font.PLAIN, 13));
            checks[i].setSelected(asignadas.contains(m.getIdMateria()));

            JCheckBox chkActual = checks[i];
            chkActual.addActionListener(e -> {
                if (chkActual.isSelected()) {
                    if (horasRestantes[0] - horas < 0) {
                        SoundManager.playError();
                        JOptionPane.showMessageDialog(null,
                            "Límite de horas superado.\n\n" +
                            "Horas de la materia: " + horas + "\n" +
                            "Horas disponibles: " + horasRestantes[0],
                            "Límite Alcanzado", JOptionPane.WARNING_MESSAGE);
                        chkActual.setSelected(false);
                        return;
                    }
                    horasRestantes[0] -= horas;
                } else {
                    horasRestantes[0] += horas;
                }
                lblHoras.setText("Horas disponibles: " + horasRestantes[0] + " / " + horasMax);
            });

            panel.add(checks[i]);
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(450, 370));
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        javax.swing.JPanel mainPanel = new javax.swing.JPanel(new BorderLayout());
        mainPanel.add(lblHoras, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, mainPanel,
            "Asignar Materias a " + profesor.getNombre() + " " + profesor.getApellidos(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            List<Integer> aGuardar = new ArrayList<>();
            for (int i = 0; i < checks.length; i++) {
                if (checks[i].isSelected()) aGuardar.add(todas.get(i).getIdMateria());
            }
            if (controller.asignarMaterias(profesor.getId(), aGuardar)) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Materias asignadas correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, "Error al asignar las materias", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Abre un dialogo de seleccion UNICA (radio buttons) para asignar al profesor
     * seleccionado la actividad complementaria con la que completa sus horas frente
     * a grupo (tutorías, gestión escolar, etc.), para que no le queden horas vacías.
     *
     * Ya no es una lista de checkboxes: un profesor tiene A LO MAS UNA actividad
     * (columna directa profesor.actividad_complementaria_clave), asi que se guarda
     * con el mismo actualizar() del profesor, no con un metodo aparte.
     */
    private void abrirDialogoActividades() {
        int row = jTable1.getSelectedRow();
        if (row == -1) { avisar("Selecciona un profesor primero."); return; }
        String rfc = (String) tableModel.getValueAt(row, 1);
        Profesor profesor = controller.buscarPorRfc(rfc);
        if (profesor == null) return;

        List<ActividadComplementaria> todas = actividadController.obtenerTodas();

        if (todas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aún no hay actividades complementarias registradas en el catálogo.\n" +
                "Puedes agregarlas desde \"Administrar actividades\".",
                "Sin actividades disponibles", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        javax.swing.JPanel panel = new javax.swing.JPanel(new GridLayout(0, 1, 5, 5));
        javax.swing.ButtonGroup grupoRadios = new javax.swing.ButtonGroup();

        javax.swing.JRadioButton radioNinguna = new javax.swing.JRadioButton("(Sin actividad complementaria)");
        radioNinguna.setFont(new Font("Arial", Font.PLAIN, 13));
        radioNinguna.setSelected(profesor.getActividadComplementariaClave() == null);
        grupoRadios.add(radioNinguna);
        panel.add(radioNinguna);

        javax.swing.JRadioButton[] radios = new javax.swing.JRadioButton[todas.size()];
        for (int i = 0; i < todas.size(); i++) {
            ActividadComplementaria a = todas.get(i);
            String texto = a.getActividad() + "  —  " + a.getTipoActividad() + "  (" + a.getHoras() + " h)";
            radios[i] = new javax.swing.JRadioButton(texto);
            radios[i].setFont(new Font("Arial", Font.PLAIN, 13));
            radios[i].setSelected(a.getClave().equals(profesor.getActividadComplementariaClave()));
            grupoRadios.add(radios[i]);
            panel.add(radios[i]);
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(450, 250));
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());

        int result = JOptionPane.showConfirmDialog(this, scroll,
            "Actividad Complementaria — " + profesor.getNombre() + " " + profesor.getApellidos(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String claveElegida = null;
            for (int i = 0; i < radios.length; i++) {
                if (radios[i].isSelected()) { claveElegida = todas.get(i).getClave(); break; }
            }
            profesor.setActividadComplementariaClave(claveElegida);
            String error = controller.actualizar(profesor);
            if (error == null) {
                SoundManager.playSuccess();
                JOptionPane.showMessageDialog(this, "Actividad complementaria guardada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                if (rfc.equals(RFC.getText().trim())) actividadComplementariaClaveActual = claveElegida;
            } else {
                SoundManager.playError();
                JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        BtnAgregar = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        BtnActualizar = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        BtnEliminar = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        BtnRegresarMenu = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        BtnAsignarMateria = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        BtnDisponibilidad = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        Email = new javax.swing.JTextField();
        Apellidos = new javax.swing.JTextField();
        ComboCategoria = new javax.swing.JComboBox<>();
        Telefono = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        RFC = new javax.swing.JTextField();
        Nombre = new javax.swing.JTextField();
        PregradoOTitulo = new javax.swing.JComboBox<>();
        LinkAdminCategorias = new javax.swing.JLabel();
        jPanelActividades = new javax.swing.JPanel();
        BtnActividades = new javax.swing.JLabel();
        LinkAdminActividades = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel11 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        BtnBuscar = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Bg.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(100, 0, 25));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 160, 0));
        jLabel1.setText("GESTIÓN DE PROFESORES");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(92, 92, 92)
                .addComponent(jLabel1)
                .addContainerGap(736, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(33, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(12, 12, 12))
        );

        Bg.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1050, 70));

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

        Bg.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 1050, 4));

        jPanel3.setBackground(new java.awt.Color(0, 120, 0));
        jPanel3.setForeground(new java.awt.Color(255, 255, 255));

        BtnAgregar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnAgregar.setForeground(new java.awt.Color(255, 255, 255));
        BtnAgregar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnAgregar.setText("AGREGAR");
        BtnAgregar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAgregar, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAgregar, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 130, 40));

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
            .addComponent(BtnActualizar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnActualizar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 90, -1, -1));

        jPanel5.setBackground(new java.awt.Color(204, 0, 0));

        BtnEliminar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnEliminar.setForeground(new java.awt.Color(255, 255, 255));
        BtnEliminar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnEliminar.setText("ELIMINAR");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnEliminar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnEliminar, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 90, -1, 40));

        jPanel6.setBackground(new java.awt.Color(153, 153, 153));

        BtnRegresarMenu.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnRegresarMenu.setForeground(new java.awt.Color(255, 255, 255));
        BtnRegresarMenu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnRegresarMenu.setText("REGRESAR AL MENU");
        BtnRegresarMenu.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnRegresarMenu, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 90, -1, -1));

        jPanel7.setBackground(new java.awt.Color(70, 0, 15));

        BtnAsignarMateria.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnAsignarMateria.setForeground(new java.awt.Color(255, 255, 255));
        BtnAsignarMateria.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnAsignarMateria.setText("ASIGNAR MATERIA");
        BtnAsignarMateria.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAsignarMateria, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnAsignarMateria, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 90, -1, -1));

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

        Bg.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 1030, 4));

        jPanel9.setBackground(new java.awt.Color(70, 0, 15));

        BtnDisponibilidad.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnDisponibilidad.setForeground(new java.awt.Color(255, 255, 255));
        BtnDisponibilidad.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnDisponibilidad.setText("DISPONIBILIDAD");
        BtnDisponibilidad.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnDisponibilidad, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnDisponibilidad, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 90, -1, -1));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(100, 0, 25));
        jLabel4.setText("Apellidos:");
        Bg.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, -1, -1));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(100, 0, 25));
        jLabel5.setText("Teléfono:");
        Bg.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 160, -1, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(100, 0, 25));
        jLabel6.setText("Categoría:");
        Bg.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, -1, -1));

        Email.setText("jTextField1");
        Email.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EmailActionPerformed(evt);
            }
        });
        Bg.add(Email, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 190, 300, -1));

        Apellidos.setText("jTextField1");
        Apellidos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ApellidosActionPerformed(evt);
            }
        });
        Bg.add(Apellidos, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 220, 250, -1));

        ComboCategoria.setModel(new javax.swing.DefaultComboBoxModel<>(new horarios.modelo.CategoriaDocente[] { new horarios.modelo.CategoriaDocente(0, "(Sin categoría)") }));
        Bg.add(ComboCategoria, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 250, 250, -1));

        LinkAdminCategorias.setFont(new java.awt.Font("Segoe UI", 2, 12)); // NOI18N (2 = italic)
        LinkAdminCategorias.setForeground(new java.awt.Color(100, 0, 25));
        LinkAdminCategorias.setText("<html><u>+ Administrar categorías</u></html>");
        LinkAdminCategorias.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        Bg.add(LinkAdminCategorias, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 253, -1, -1));

        Telefono.setText("jTextField1");
        Telefono.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TelefonoActionPerformed(evt);
            }
        });
        Bg.add(Telefono, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 160, 250, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(100, 0, 25));
        jLabel7.setText("RFC:");
        Bg.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, -1, -1));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(100, 0, 25));
        jLabel8.setText("Nombre:");
        Bg.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, -1, -1));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(100, 0, 25));
        jLabel9.setText("Email:");
        Bg.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 190, -1, -1));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(100, 0, 25));
        jLabel10.setText("Pregrado/Titulo:");
        Bg.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 220, -1, -1));

        RFC.setText("jTextField1");
        RFC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RFCActionPerformed(evt);
            }
        });
        Bg.add(RFC, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 160, 250, -1));

        Nombre.setText("jTextField1");
        Nombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NombreActionPerformed(evt);
            }
        });
        Bg.add(Nombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 190, 250, -1));

        PregradoOTitulo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        PregradoOTitulo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PregradoOTituloActionPerformed(evt);
            }
        });
        Bg.add(PregradoOTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 220, 300, -1));

        jPanelActividades.setBackground(new java.awt.Color(70, 0, 15));

        BtnActividades.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnActividades.setForeground(new java.awt.Color(255, 255, 255));
        BtnActividades.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnActividades.setText("ACTIVIDADES COMPLEMENTARIAS");
        BtnActividades.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanelActividadesLayout = new javax.swing.GroupLayout(jPanelActividades);
        jPanelActividades.setLayout(jPanelActividadesLayout);
        jPanelActividadesLayout.setHorizontalGroup(
            jPanelActividadesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnActividades, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
        );
        jPanelActividadesLayout.setVerticalGroup(
            jPanelActividadesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnActividades, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
        );

        Bg.add(jPanelActividades, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 280, 280, 34));

        LinkAdminActividades.setFont(new java.awt.Font("Segoe UI", 2, 12)); // NOI18N (2 = italic)
        LinkAdminActividades.setForeground(new java.awt.Color(100, 0, 25));
        LinkAdminActividades.setText("<html><u>+ Administrar actividades</u></html>");
        LinkAdminActividades.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        Bg.add(LinkAdminActividades, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 288, -1, -1));

        jPanel10.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1030, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 310, 1030, 4));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "RFC", "Nombre completo", "Email", "Categoría"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        Bg.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 330, 1030, 370));

        jPanel11.setBackground(new java.awt.Color(204, 160, 0));

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1050, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        Bg.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 710, 1050, 4));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(100, 0, 25));
        jLabel11.setText("Seleccione un profesor de la tabla o complete el formulario");
        Bg.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 720, -1, -1));

        jPanel12.setBackground(new java.awt.Color(100, 0, 25));

        BtnBuscar.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BtnBuscar.setForeground(new java.awt.Color(204, 160, 0));
        BtnBuscar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        BtnBuscar.setText("BUSCAR");
        BtnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BtnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        Bg.add(jPanel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 90, 140, 40));

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

    private void RFCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RFCActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RFCActionPerformed

    private void ApellidosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ApellidosActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ApellidosActionPerformed

    private void TelefonoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TelefonoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TelefonoActionPerformed

    private void NombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NombreActionPerformed

    private void EmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmailActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EmailActionPerformed

    private void PregradoOTituloActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PregradoOTituloActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PregradoOTituloActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new ProfesoresWindow().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField Apellidos;
    private javax.swing.JPanel Bg;
    private javax.swing.JLabel BtnActividades;
    private javax.swing.JLabel BtnActualizar;
    private javax.swing.JLabel BtnAgregar;
    private javax.swing.JLabel BtnAsignarMateria;
    private javax.swing.JLabel BtnBuscar;
    private javax.swing.JLabel BtnDisponibilidad;
    private javax.swing.JLabel BtnEliminar;
    private javax.swing.JLabel BtnRegresarMenu;
    private javax.swing.JComboBox<horarios.modelo.CategoriaDocente> ComboCategoria;
    private javax.swing.JTextField Email;
    private javax.swing.JLabel LinkAdminActividades;
    private javax.swing.JLabel LinkAdminCategorias;
    private javax.swing.JTextField Nombre;
    private javax.swing.JComboBox<String> PregradoOTitulo;
    private javax.swing.JTextField RFC;
    private javax.swing.JTextField Telefono;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanelActividades;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
