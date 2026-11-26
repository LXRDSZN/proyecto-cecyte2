package com.mycompany.starbucks.view;

import com.formdev.flatlaf.FlatLaf;
import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.service.ColaboradorService;
import com.mycompany.starbucks.service.PeriodoQuincenalService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel de Periodos Quincenales del sistema STARBUCKS — Pre-nomina.
 * Permite crear, consultar y gestionar periodos quincenales.
 */
public class PeriodosPanel extends JPanel {

    private final MainFrame mainFrame;
    private final PeriodoQuincenalService periodoService;
    private final ColaboradorService colaboradorService;

    private JTextField txtIdentificador;
    private JTextField txtFechaInicio;
    private JTextField txtFechaFin;
    private JTable tablePeriodos;
    private DefaultTableModel tableModelPeriodos;

    public PeriodosPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.periodoService = mainFrame.getPeriodoService();
        this.colaboradorService = mainFrame.getColaboradorService();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Estilo.FONDO);

        // Panel superior - Formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Estilo.SUPERFICIE);
        formPanel.setBorder(Estilo.tarjeta(14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lbl = new JLabel("Identificador:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        txtIdentificador = new JTextField();
        txtIdentificador.setPreferredSize(new Dimension(180, 30));
        txtIdentificador.setBackground(Estilo.FONDO);
        txtIdentificador.setForeground(Color.WHITE);
        txtIdentificador.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtIdentificador, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        lbl = new JLabel("Fecha inicial:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        txtFechaInicio = new JTextField();
        txtFechaInicio.setPreferredSize(new Dimension(180, 30));
        txtFechaInicio.setBackground(Estilo.FONDO);
        txtFechaInicio.setForeground(Color.WHITE);
        txtFechaInicio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtFechaInicio, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        lbl = new JLabel("Fecha final:"); lbl.setForeground(Color.WHITE); lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lbl, gbc);

        gbc.gridx = 1;
        txtFechaFin = new JTextField();
        txtFechaFin.setPreferredSize(new Dimension(180, 30));
        txtFechaFin.setBackground(Estilo.FONDO);
        txtFechaFin.setForeground(Color.WHITE);
        txtFechaFin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtFechaFin, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton btnGuardarPeriodo = new JButton("Registrar periodo");
        btnGuardarPeriodo.setBackground(Estilo.VERDE);
        btnGuardarPeriodo.setForeground(Color.WHITE);
        btnGuardarPeriodo.setFocusPainted(false);
        btnGuardarPeriodo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnGuardarPeriodo.addActionListener(e -> guardarPeriodo());
        formPanel.add(btnGuardarPeriodo, gbc);

        gbc.gridy = 5;
        JButton btnActualizar = new JButton("Actualizar periodo");
        btnActualizar.setBackground(new Color(0x6C, 0x75, 0x7D));
        btnActualizar.setForeground(Color.WHITE);
        btnActualizar.setFocusPainted(false);
        btnActualizar.addActionListener(e -> actualizarPeriodo());
        formPanel.add(btnActualizar, gbc);

        gbc.gridy = 6;
        JButton dias = new JButton("Días trabajados del periodo seleccionado");
        dias.addActionListener(e -> mostrarDiasTrabajados());
        formPanel.add(dias, gbc);
        add(formPanel, BorderLayout.NORTH);

        // Panel central - Tabla de periodos
        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(Estilo.FONDO);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel tableTitle = new JLabel("Periodos Quincenales Registrados");
        tableTitle.setForeground(Color.WHITE);
        tableTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] colPeriodos = {"ID", "Identificador", "Fecha Inicio", "Fecha Fin", "Estado"};
        tableModelPeriodos = new DefaultTableModel(colPeriodos, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tablePeriodos = new JTable(tableModelPeriodos);
        tablePeriodos.setBackground(Estilo.SUPERFICIE);
        tablePeriodos.setForeground(Color.WHITE);
        tablePeriodos.setGridColor(Estilo.BORDE);
        tablePeriodos.setSelectionBackground(Estilo.VERDE);
        tablePeriodos.setRowHeight(30);

        JTableHeader headerPeriodos = tablePeriodos.getTableHeader();
        headerPeriodos.setBackground(Estilo.VERDE);
        headerPeriodos.setForeground(Color.WHITE);
        headerPeriodos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        tablePeriodos.setIntercellSpacing(new Dimension(2, 2));
        tablePeriodos.getTableHeader().setPreferredSize(new Dimension(0, 25));

        JScrollPane scrollPeriodos = new JScrollPane(tablePeriodos);
        scrollPeriodos.setBackground(Estilo.SUPERFICIE);
        scrollPeriodos.getViewport().setBackground(Estilo.SUPERFICIE);
        tablePanel.add(scrollPeriodos, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);

        cargarPeriodos();

    }

    private void mostrarDiasTrabajados() {
        int fila = tablePeriodos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un periodo.");
            return;
        }
        int indice = tablePeriodos.convertRowIndexToModel(fila);
        int periodo = (Integer) tableModelPeriodos.getValueAt(indice, 0);
        var servicio = new com.mycompany.starbucks.service.DiaTrabajadoService();
        try {
            List<Colaborador> colaboradores = colaboradorService.listarTodos();
            Object[] nombres = colaboradores.stream()
                    .map(c -> c.getNumeroEmpleado() + " · " + c.getNombreCompleto()).toArray();
            JComboBox<Object> selector = new JComboBox<>(nombres);
            JTextField dias = new JTextField(5);
            JButton consultar = new JButton("Consultar");
            consultar.addActionListener(e -> {
                if (selector.getSelectedIndex() < 0) {
                    dias.setText("");
                    return;
                }
                try {
                    int id = colaboradores.get(selector.getSelectedIndex()).getId();
                    var registro = servicio.consultar(periodo, id);
                    dias.setText(registro.isPresent() ? Integer.toString(registro.getAsInt()) : "");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error al consultar los días trabajados.");
                }
            });
            selector.addActionListener(e -> consultar.doClick());
            consultar.doClick();
            JPanel campos = new JPanel(new GridLayout(0, 1, 0, 6));
            campos.add(new JLabel("Colaborador"));
            campos.add(selector);
            campos.add(new JLabel("Días trabajados"));
            campos.add(dias);
            campos.add(consultar);
            var actor = mainFrame.getUsuario();
            boolean editable = "CAPTURA".equals(tableModelPeriodos.getValueAt(indice, 4))
                    && ("ADMINISTRADOR".equals(actor.getRol()) || "CAPTURISTA".equals(actor.getRol()));
            dias.setEditable(editable);
            if (!editable || colaboradores.isEmpty()) {
                JOptionPane.showMessageDialog(this, campos, "Días trabajados", JOptionPane.PLAIN_MESSAGE);
                return;
            }
            while (JOptionPane.showOptionDialog(this, campos, "Días trabajados",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new String[]{"Guardar", "Cancelar"}, "Guardar") == JOptionPane.OK_OPTION) {
                try {
                    int id = colaboradores.get(selector.getSelectedIndex()).getId();
                    servicio.guardar(actor, periodo, id, Integer.parseInt(dias.getText().trim()));
                    JOptionPane.showMessageDialog(this, "Días trabajados guardados.");
                    return;
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Ingresa un número entero de días.");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error en días trabajados: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error al acceder a los días trabajados.");
        }
    }

    private void cargarPeriodos() {
        try {
            List<PeriodoQuincenal> periodos = periodoService.listarTodos();
            tableModelPeriodos.setRowCount(0);
            for (PeriodoQuincenal p : periodos) {
                tableModelPeriodos.addRow(new Object[]{p.getId(), p.getIdentificador(), p.getFechaInicio(), p.getFechaFin(), p.getEstado()});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void guardarPeriodo() {
        String identificador = txtIdentificador.getText().trim();
        String fechaInicioTexto = txtFechaInicio.getText().trim();
        String fechaFinTexto = txtFechaFin.getText().trim();

        if (identificador.isEmpty()) { JOptionPane.showMessageDialog(this, "Identificador obligatorio", "Validacion", JOptionPane.WARNING_MESSAGE); return; }
        if (fechaInicioTexto.isEmpty()) { JOptionPane.showMessageDialog(this, "Fecha inicial obligatoria", "Validacion", JOptionPane.WARNING_MESSAGE); return; }
        if (fechaFinTexto.isEmpty()) { JOptionPane.showMessageDialog(this, "Fecha final obligatoria", "Validacion", JOptionPane.WARNING_MESSAGE); return; }

        try {
            java.time.LocalDate fechaInicio = java.time.LocalDate.parse(fechaInicioTexto);
            java.time.LocalDate fechaFin = java.time.LocalDate.parse(fechaFinTexto);
            if (fechaFin.isBefore(fechaInicio) || fechaFin.isEqual(fechaInicio)) {
                JOptionPane.showMessageDialog(this, "Fecha final debe ser despues de la inicial", "Validacion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (periodoService.identificadorExiste(identificador)) {
                JOptionPane.showMessageDialog(this, "Ya existe un periodo con ese identificador", "Validacion", JOptionPane.WARNING_MESSAGE);
                return;
            }

            PeriodoQuincenal periodo = new PeriodoQuincenal();
            periodo.setIdentificador(identificador);
            periodo.setFechaInicio(fechaInicio);
            periodo.setFechaFin(fechaFin);
            periodo.setEstado("CAPTURA");

            periodoService.guardar(mainFrame.getUsuario(), periodo);
            try { mainFrame.getBitacoraService().registrar(mainFrame.getUsuario().getId(), "ALTA_PERIODO", "periodo_quincenal", periodo.getId(), "Periodo: " + identificador); } catch (SQLException ex) { ex.printStackTrace(); }

            cargarPeriodos();
            limpiarFormularioPeriodo();
            JOptionPane.showMessageDialog(this, "Periodo quincenal guardado correctamente", "Exito", JOptionPane.INFORMATION_MESSAGE);

        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Fechas deben tener formato AAAA-MM-DD", "Validacion", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException e) { JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(this, "Error al guardar periodo", "Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void actualizarPeriodo() {
        int selectedRow = tablePeriodos.getSelectedRow();
        if (selectedRow < 0) { JOptionPane.showMessageDialog(this, "Seleccione un periodo", "Validacion", JOptionPane.WARNING_MESSAGE); return; }

        int periodoId = (int) tableModelPeriodos.getValueAt(selectedRow, 0);
        String identificador = JOptionPane.showInputDialog(this, "Nuevo identificador:", "Actualizar periodo", JOptionPane.QUESTION_MESSAGE);
        if (identificador == null || identificador.trim().isEmpty()) return;
        identificador = identificador.trim();

        try {
            periodoService.renombrar(mainFrame.getUsuario(), periodoId, identificador);
            try { mainFrame.getBitacoraService().registrar(mainFrame.getUsuario().getId(), "MODIFICAR_PERIODO", "periodo_quincenal", periodoId, "Modificacion periodo ID " + periodoId); } catch (SQLException ex) { ex.printStackTrace(); }

            cargarPeriodos();
            JOptionPane.showMessageDialog(this, "Periodo actualizado correctamente", "Exito", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) { JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(this, "Error al actualizar periodo", "Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void limpiarFormularioPeriodo() {
        txtIdentificador.setText("");
        txtFechaInicio.setText("");
        txtFechaFin.setText("");
    }
}