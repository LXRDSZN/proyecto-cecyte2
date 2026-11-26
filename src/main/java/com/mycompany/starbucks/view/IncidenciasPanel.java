package com.mycompany.starbucks.view;

import com.formdev.flatlaf.FlatLaf;
import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.Incidencia;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.TipoIncidencia;
import com.mycompany.starbucks.service.ColaboradorService;
import com.mycompany.starbucks.service.IncidenciaService;
import com.mycompany.starbucks.service.PeriodoQuincenalService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel de Control de Incidencias del sistema STARBUCKS — Pre-nomina.
 * Permite registrar, consultar, filtrar y gestionar incidencias.
 */
public class IncidenciasPanel extends JPanel {

    private final MainFrame mainFrame;
    private final IncidenciaService incidenciaService;
    private final ColaboradorService colaboradorService;
    private final PeriodoQuincenalService periodoService;

    private JComboBox<Colaborador> cmbColaborador;
    private JComboBox<PeriodoQuincenal> cmbPeriodo;
    private JComboBox<PeriodoQuincenal> cmbPeriodoForm;
    private JTable tableIncidencias;
    private DefaultTableModel tableModel;
    private JComboBox<TipoIncidencia> cmbTipo;
    private JTextField txtObservaciones;
    private JTextField txtFecha;
    private final JTextField txtFechaFin = new JTextField(12);
    private final JTextField txtMinutos = new JTextField(12);
    private final JTextField txtClasificacion = new JTextField(12);
    private final JTextField txtTratamiento = new JTextField(12);
    private final JTextField txtReferencia = new JTextField(12);
    private final JComboBox<String> cmbAutorizacion = new JComboBox<>(new String[]{"", "Sí", "No"});
    private final JPanel datosTipo = new JPanel(new GridBagLayout());
    private JTextField txtBusqueda;
    private Integer incidenciaEnEdicion;
    private boolean recargando;
    private JComboBox<String> cmbFiltroTipo;
    private JComboBox<String> cmbFiltroEstado;

    public IncidenciasPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.incidenciaService = mainFrame.getIncidenciaService();
        this.colaboradorService = mainFrame.getColaboradorService();
        this.periodoService = mainFrame.getPeriodoService();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Estilo.FONDO);

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setBackground(Estilo.SUPERFICIE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel filtrosPanel = new JPanel(new GridBagLayout());
        filtrosPanel.setBackground(Estilo.SUPERFICIE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lbl = new JLabel("Periodo:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        filtrosPanel.add(lbl, gbc);

        gbc.gridx = 1;
        cmbPeriodo = new JComboBox<>();
        configurarEtiquetas(cmbPeriodo, PeriodoQuincenal::getIdentificador, "Todos");
        cargarPeriodosCombo();
        cmbPeriodo.setBackground(Estilo.FONDO);
        cmbPeriodo.setForeground(Color.WHITE);
        cmbPeriodo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        filtrosPanel.add(cmbPeriodo, gbc);

        gbc.gridx = 2;
        lbl = new JLabel("Tipo:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        filtrosPanel.add(lbl, gbc);

        gbc.gridx = 3;
        cmbFiltroTipo = new JComboBox<>();
        cmbFiltroTipo.addItem("Todos");
        cmbFiltroTipo.addItem("Falta");
        cmbFiltroTipo.addItem("Retardo");
        cmbFiltroTipo.addItem("Permiso");
        cmbFiltroTipo.addItem("Vacaciones");
        cmbFiltroTipo.addItem("Incapacidad");
        cmbFiltroTipo.setBackground(Estilo.FONDO);
        cmbFiltroTipo.setForeground(Color.WHITE);
        cmbFiltroTipo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        filtrosPanel.add(cmbFiltroTipo, gbc);

        gbc.gridx = 4;
        lbl = new JLabel("Estado:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        filtrosPanel.add(lbl, gbc);

        gbc.gridx = 5;
        cmbFiltroEstado = new JComboBox<>();
        cmbFiltroEstado.addItem("Todos");
        cmbFiltroEstado.addItem("PENDIENTE");
        cmbFiltroEstado.addItem("REVISADO");
        cmbFiltroEstado.addItem("AUTORIZADO");
        cmbFiltroEstado.addItem("CANCELADA");
        cmbFiltroEstado.setBackground(Estilo.FONDO);
        cmbFiltroEstado.setForeground(Color.WHITE);
        cmbFiltroEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        filtrosPanel.add(cmbFiltroEstado, gbc);

        gbc.gridx = 6; gbc.gridy = 0; gbc.gridwidth = 1;
        JButton btnAplicar = new JButton("Aplicar");
        btnAplicar.setBackground(Estilo.VERDE);
        btnAplicar.setForeground(Color.WHITE);
        btnAplicar.setFocusPainted(false);
        btnAplicar.addActionListener(e -> aplicarFiltros());
        filtrosPanel.add(btnAplicar, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        filtrosPanel.add(new JLabel("Colaborador"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        txtBusqueda = new JTextField(16);
        txtBusqueda.addActionListener(e -> aplicarFiltros());
        filtrosPanel.add(txtBusqueda, gbc);

        topPanel.add(filtrosPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Estilo.SUPERFICIE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        lbl = new JLabel("Colaborador:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        cmbColaborador = new JComboBox<>();
        configurarEtiquetas(cmbColaborador, c -> c.getNumeroEmpleado() + " · " + c.getNombreCompleto(), "");
        cargarColaboradoresCombo();
        cmbColaborador.setBackground(Estilo.FONDO);
        cmbColaborador.setForeground(Color.WHITE);
        cmbColaborador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(cmbColaborador, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        lbl = new JLabel("Periodo:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        cmbPeriodoForm = new JComboBox<>();
        configurarEtiquetas(cmbPeriodoForm, PeriodoQuincenal::getIdentificador, "");
        cargarPeriodosComboForm();
        cmbPeriodoForm.setBackground(Estilo.FONDO);
        cmbPeriodoForm.setForeground(Color.WHITE);
        cmbPeriodoForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(cmbPeriodoForm, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        lbl = new JLabel("Tipo:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        cmbTipo = new JComboBox<>();
        cmbTipo.addItem(TipoIncidencia.FALTA);
        cmbTipo.addItem(TipoIncidencia.RETARDO);
        cmbTipo.addItem(TipoIncidencia.PERMISO);
        cmbTipo.addItem(TipoIncidencia.VACACIONES);
        cmbTipo.addItem(TipoIncidencia.INCAPACIDAD);
        cmbTipo.setBackground(Estilo.FONDO);
        cmbTipo.setForeground(Color.WHITE);
        cmbTipo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(cmbTipo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        lbl = new JLabel("Observaciones:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        txtObservaciones = new JTextField();
        txtObservaciones.setPreferredSize(new Dimension(200, 30));
        txtObservaciones.setBackground(Estilo.FONDO);
        txtObservaciones.setForeground(Color.WHITE);
        txtObservaciones.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtObservaciones, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Fecha inicial (AAAA-MM-DD)"), gbc);
        gbc.gridx = 1;
        txtFecha = new JTextField(12);
        formPanel.add(txtFecha, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JButton btnRegistrar = new JButton("Guardar incidencia");
        btnRegistrar.setBackground(Estilo.VERDE);
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnRegistrar.addActionListener(e -> registrarIncidencia());
        formPanel.add(btnRegistrar, gbc);

        gbc.gridy = 6;
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setBackground(new Color(0x6C, 0x75, 0x7D));
        btnLimpiar.setForeground(Color.WHITE);
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.addActionListener(e -> limpiarFormularioIncidencia());
        formPanel.add(btnLimpiar, gbc);

        topPanel.add(formPanel, BorderLayout.CENTER);
        datosTipo.setPreferredSize(new Dimension(300, 0));
        topPanel.add(datosTipo, BorderLayout.EAST);
        cmbTipo.addActionListener(e -> actualizarCamposPorTipo());
        actualizarCamposPorTipo();

        add(topPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(Estilo.FONDO);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel tableTitle = new JLabel("Historial de Incidencias");
        tableTitle.setForeground(Color.WHITE);
        tableTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] colIncidencias = {"ID", "Colaborador", "Tipo", "Periodo", "Fecha", "Estado", "Observaciones"};
        tableModel = new DefaultTableModel(colIncidencias, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tableIncidencias = new JTable(tableModel);
        tableIncidencias.setBackground(Estilo.SUPERFICIE);
        tableIncidencias.setForeground(Color.WHITE);
        tableIncidencias.setGridColor(Estilo.BORDE);
        tableIncidencias.setSelectionBackground(Estilo.VERDE);
        tableIncidencias.setRowHeight(30);

        JTableHeader headerIncidencias = tableIncidencias.getTableHeader();
        headerIncidencias.setBackground(Estilo.VERDE);
        headerIncidencias.setForeground(Color.WHITE);
        headerIncidencias.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        tableIncidencias.setIntercellSpacing(new Dimension(2, 2));
        tableIncidencias.getTableHeader().setPreferredSize(new Dimension(0, 25));

        JScrollPane scrollPane = new JScrollPane(tableIncidencias);
        scrollPane.setBackground(Estilo.SUPERFICIE);
        scrollPane.getViewport().setBackground(Estilo.SUPERFICIE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editar = new JButton("Editar seleccionada");
        editar.addActionListener(e -> editarSeleccionada());
        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> actualizar());
        JButton detalle = new JButton("Ver detalle");
        detalle.addActionListener(e -> verDetalle());
        acciones.add(detalle);
        acciones.add(editar);
        acciones.add(actualizar);
        tablePanel.add(acciones, BorderLayout.SOUTH);

        add(tablePanel, BorderLayout.CENTER);

        aplicarFiltros();

        cmbFiltroTipo.addActionListener(e -> aplicarFiltros());
        cmbFiltroEstado.addActionListener(e -> aplicarFiltros());
        cmbPeriodo.addActionListener(e -> aplicarFiltros());
    }

    private void cargarColaboradoresCombo() {
        try {
            List<Colaborador> colaboradores = colaboradorService.listarTodos();
            for (Colaborador c : colaboradores) { cmbColaborador.addItem(c); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void cargarPeriodosCombo() {
        try {
            List<PeriodoQuincenal> periodos = periodoService.listarTodos();
            cmbPeriodo.addItem(null);
            for (PeriodoQuincenal p : periodos) { cmbPeriodo.addItem(p); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void cargarPeriodosComboForm() {
        cmbPeriodoForm.removeAllItems();
        try {
            List<PeriodoQuincenal> periodos = periodoService.listarTodos();
            for (PeriodoQuincenal p : periodos) { cmbPeriodoForm.addItem(p); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    void actualizar() {
        recargando = true;
        try {
            limpiarFormularioIncidencia();
            cmbColaborador.removeAllItems();
            cmbPeriodo.removeAllItems();
            cargarColaboradoresCombo();
            cargarPeriodosCombo();
            cargarPeriodosComboForm();
        } finally {
            recargando = false;
        }
        aplicarFiltros();
    }

    private <T> void configurarEtiquetas(JComboBox<T> combo, java.util.function.Function<T, String> etiqueta,
                                         String vacio) {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        combo.setRenderer((lista, valor, indice, seleccionado, foco) ->
                renderer.getListCellRendererComponent(lista, valor == null ? vacio : etiqueta.apply(valor),
                        indice, seleccionado, foco));
    }

    private void aplicarFiltros() {
        if (recargando) {
            return;
        }
        try {
            var periodo = (PeriodoQuincenal) cmbPeriodo.getSelectedItem();
            String tipo = (String) cmbFiltroTipo.getSelectedItem();
            String estado = (String) cmbFiltroEstado.getSelectedItem();
            String busqueda = txtBusqueda.getText().trim().toLowerCase(java.util.Locale.ROOT);
            var incidencias = incidenciaService.listarTodos();
            tableModel.setRowCount(0);
            for (Incidencia inc : incidencias) {
                var colaborador = inc.getColaborador();
                if ((periodo != null && !periodo.getId().equals(inc.getPeriodo().getId()))
                        || (!"Todos".equals(tipo) && (inc.getTipo() == null || !tipo.equalsIgnoreCase(inc.getTipo().name())))
                        || (!"Todos".equals(estado) && !estado.equals(inc.getEstado()))
                        || !(colaborador.getNumeroEmpleado() + " " + colaborador.getNombreCompleto())
                                .toLowerCase(java.util.Locale.ROOT).contains(busqueda)) {
                    continue;
                }
                tableModel.addRow(new Object[]{inc.getId(), colaborador.getNombreCompleto(), inc.getTipo(),
                        inc.getPeriodo().getIdentificador(), mostrarRango(inc), inc.getEstado(), inc.getObservaciones()});
            }
        } catch (SQLException ex) {
            System.err.println("Error al cargar incidencias: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudieron consultar las incidencias.");
        }
    }

    private void actualizarCamposPorTipo() {
        datosTipo.removeAll();
        var tipo = (TipoIncidencia) cmbTipo.getSelectedItem();
        if (tipo == TipoIncidencia.PERMISO || tipo == TipoIncidencia.VACACIONES || tipo == TipoIncidencia.INCAPACIDAD) {
            agregarCampo("Fecha final (opcional)", txtFechaFin);
        }
        if (tipo == TipoIncidencia.RETARDO) {
            agregarCampo("Duración en minutos", txtMinutos);
            agregarCampo("Clasificación", txtClasificacion);
        }
        if (tipo == TipoIncidencia.PERMISO) {
            agregarCampo("Tipo de permiso", txtClasificacion);
            agregarCampo("Autorizado", cmbAutorizacion);
            agregarCampo("Tratamiento", txtTratamiento);
        }
        if (tipo == TipoIncidencia.VACACIONES) agregarCampo("Fechas aprobadas", cmbAutorizacion);
        if (tipo == TipoIncidencia.INCAPACIDAD) {
            agregarCampo("Tipo de incapacidad", txtClasificacion);
            agregarCampo("Referencia documental", txtReferencia);
        }
        datosTipo.revalidate();
        datosTipo.repaint();
    }

    private void agregarCampo(String etiqueta, JComponent campo) {
        var gbc = new GridBagConstraints();
        gbc.gridy = datosTipo.getComponentCount() / 2;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.anchor = GridBagConstraints.WEST;
        datosTipo.add(new JLabel(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        datosTipo.add(campo, gbc);
    }

    private String mostrarRango(Incidencia incidencia) {
        if (incidencia.getFecha() == null) return "—";
        return incidencia.getFecha().equals(incidencia.fechaFinalEfectiva()) ? incidencia.getFecha().toString()
                : incidencia.getFecha() + " a " + incidencia.getFechaFin();
    }

    private void verDetalle() {
        int fila = tableIncidencias.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una incidencia.");
            return;
        }
        int id = (Integer) tableModel.getValueAt(tableIncidencias.convertRowIndexToModel(fila), 0);
        try {
            var inc = incidenciaService.porId(id).orElseThrow();
            StringBuilder detalle = new StringBuilder();
            detalle.append(inc.getColaborador().getNombreCompleto()).append(" · ").append(inc.getTipo())
                    .append("\n").append(mostrarRango(inc)).append(" · ").append(inc.getEstado());
            agregarDetalle(detalle, "Observaciones", inc.getObservaciones());
            agregarDetalle(detalle, "Duración en minutos", inc.getMinutosRetardo());
            agregarDetalle(detalle, "Tipo o clasificación", inc.getClasificacion());
            if (inc.getAutorizacionDocumentada() != null) {
                agregarDetalle(detalle, "Autorización documentada", inc.getAutorizacionDocumentada() ? "Sí" : "No");
            }
            agregarDetalle(detalle, "Tratamiento", inc.getTratamiento());
            agregarDetalle(detalle, "Referencia documental", inc.getReferenciaDocumental());
            JTextArea texto = new JTextArea(detalle.toString(), 8, 42);
            texto.setEditable(false);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Detalle de incidencia", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException | java.util.NoSuchElementException ex) {
            System.err.println("Error al consultar incidencia: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudo consultar la incidencia.");
        }
    }

    private void agregarDetalle(StringBuilder detalle, String etiqueta, Object valor) {
        if (valor != null && !valor.toString().isBlank()) {
            detalle.append("\n").append(etiqueta).append(": ").append(valor);
        }
    }

    private void editarSeleccionada() {
        int fila = tableIncidencias.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una incidencia.");
            return;
        }
        int id = (Integer) tableModel.getValueAt(tableIncidencias.convertRowIndexToModel(fila), 0);
        try {
            var incidencia = incidenciaService.porId(id).orElseThrow();
            if (!"PENDIENTE".equals(incidencia.getEstado())) {
                JOptionPane.showMessageDialog(this, "Solo puedes editar incidencias pendientes.");
                return;
            }
            incidenciaEnEdicion = id;
            seleccionar(cmbColaborador, c -> c.getId().equals(incidencia.getColaborador().getId()));
            seleccionar(cmbPeriodoForm, p -> p.getId().equals(incidencia.getPeriodo().getId()));
            cmbPeriodoForm.setEnabled(false);
            cmbTipo.setSelectedItem(incidencia.getTipo());
            txtFecha.setText(incidencia.getFecha() == null ? "" : incidencia.getFecha().toString());
            txtObservaciones.setText(incidencia.getObservaciones());
            txtFechaFin.setText(incidencia.getFechaFin() == null ? "" : incidencia.getFechaFin().toString());
            txtMinutos.setText(incidencia.getMinutosRetardo() == null ? "" : incidencia.getMinutosRetardo().toString());
            txtClasificacion.setText(incidencia.getClasificacion());
            txtTratamiento.setText(incidencia.getTratamiento());
            txtReferencia.setText(incidencia.getReferenciaDocumental());
            cmbAutorizacion.setSelectedIndex(incidencia.getAutorizacionDocumentada() == null ? 0
                    : incidencia.getAutorizacionDocumentada() ? 1 : 2);
        } catch (SQLException | java.util.NoSuchElementException ex) {
            System.err.println("Error al cargar incidencia: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudo cargar la incidencia.");
        }
    }

    private <T> void seleccionar(JComboBox<T> combo, java.util.function.Predicate<T> coincide) {
        combo.setSelectedIndex(-1);
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (coincide.test(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void registrarIncidencia() {
        Colaborador colaborador = (Colaborador) cmbColaborador.getSelectedItem();
        PeriodoQuincenal periodo = (PeriodoQuincenal) cmbPeriodoForm.getSelectedItem();
        TipoIncidencia tipo = (TipoIncidencia) cmbTipo.getSelectedItem();
        String observaciones = txtObservaciones.getText().trim();

        if (colaborador == null) { JOptionPane.showMessageDialog(this, "Seleccione un colaborador", "Validacion", JOptionPane.WARNING_MESSAGE); return; }
        if (periodo == null) { JOptionPane.showMessageDialog(this, "Seleccione un periodo", "Validacion", JOptionPane.WARNING_MESSAGE); return; }
        if (tipo == null) { JOptionPane.showMessageDialog(this, "Seleccione un tipo de incidencia", "Validacion", JOptionPane.WARNING_MESSAGE); return; }

        Incidencia incidencia = new Incidencia();
        incidencia.setId(incidenciaEnEdicion);
        incidencia.setColaborador(colaborador);
        incidencia.setPeriodo(periodo);
        incidencia.setTipo(tipo);
        incidencia.setObservaciones(observaciones);
        incidencia.setEstado("PENDIENTE");

        try {
            incidencia.setFecha(java.time.LocalDate.parse(txtFecha.getText().trim()));
            if (tipo != TipoIncidencia.FALTA && tipo != TipoIncidencia.RETARDO && !txtFechaFin.getText().isBlank()) {
                incidencia.setFechaFin(java.time.LocalDate.parse(txtFechaFin.getText().trim()));
            }
            if (tipo == TipoIncidencia.RETARDO && !txtMinutos.getText().isBlank()) {
                incidencia.setMinutosRetardo(Integer.valueOf(txtMinutos.getText().trim()));
            }
            if (tipo == TipoIncidencia.RETARDO || tipo == TipoIncidencia.PERMISO || tipo == TipoIncidencia.INCAPACIDAD) {
                incidencia.setClasificacion(txtClasificacion.getText().trim());
            }
            if (tipo == TipoIncidencia.PERMISO || tipo == TipoIncidencia.VACACIONES) {
                int autorizacion = cmbAutorizacion.getSelectedIndex();
                incidencia.setAutorizacionDocumentada(autorizacion == 0 ? null : autorizacion == 1);
            }
            if (tipo == TipoIncidencia.PERMISO) incidencia.setTratamiento(txtTratamiento.getText().trim());
            if (tipo == TipoIncidencia.INCAPACIDAD) incidencia.setReferenciaDocumental(txtReferencia.getText().trim());
            incidenciaService.guardar(mainFrame.getUsuario(), incidencia);
            aplicarFiltros();
            limpiarFormularioIncidencia();
            JOptionPane.showMessageDialog(this, "Incidencia guardada correctamente", "Exito", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa una fecha en formato AAAA-MM-DD.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa los minutos como número entero.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Error al guardar incidencia: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudo guardar la incidencia.");
        }
    }

    private void limpiarFormularioIncidencia() {
        incidenciaEnEdicion = null;
        cmbColaborador.setSelectedIndex(-1);
        cmbPeriodoForm.setEnabled(true);
        cmbPeriodoForm.setSelectedIndex(-1);
        cmbTipo.setSelectedIndex(-1);
        txtFecha.setText("");
        txtFechaFin.setText("");
        txtMinutos.setText("");
        txtClasificacion.setText("");
        txtTratamiento.setText("");
        txtReferencia.setText("");
        cmbAutorizacion.setSelectedIndex(0);
        txtObservaciones.setText("");
    }
}
