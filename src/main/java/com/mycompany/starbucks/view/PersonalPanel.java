package com.mycompany.starbucks.view;

import com.formdev.flatlaf.FlatLaf;
import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.service.ColaboradorService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel de gestion de Personal del sistema STARBUCKS — Pre-nomina.
 * Permite agregar, buscar, editar y cambiar estado de colaboradores.
 * Los datos se persisten en MariaDB.
 */
public class PersonalPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ColaboradorService colaboradorService;
    private JTextField txtNumeroEmpleado;
    private JTextField txtNombreCompleto;
    private JTextField txtSalarioDiario;
    private JComboBox<String> cmbEstado;
    private JTable tableColaboradores;
    private DefaultTableModel tableModel;
    private Integer colaboradorEnEdicion;

    public PersonalPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
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

        // Campos del formulario
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Num. Empleado:"), gbc);
        gbc.gridx = 1; txtNumeroEmpleado = new JTextField(); txtNumeroEmpleado.setPreferredSize(new Dimension(180, 30)); txtNumeroEmpleado.setBackground(Estilo.FONDO); txtNumeroEmpleado.setForeground(Color.WHITE); txtNumeroEmpleado.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Estilo.BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10))); formPanel.add(txtNumeroEmpleado, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Nombre completo:"), gbc);
        gbc.gridx = 1; txtNombreCompleto = new JTextField(); txtNombreCompleto.setPreferredSize(new Dimension(180, 30)); txtNombreCompleto.setBackground(Estilo.FONDO); txtNombreCompleto.setForeground(Color.WHITE); txtNombreCompleto.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Estilo.BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10))); formPanel.add(txtNombreCompleto, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Salario diario:"), gbc);
        gbc.gridx = 1; txtSalarioDiario = new JTextField(); txtSalarioDiario.setPreferredSize(new Dimension(180, 30)); txtSalarioDiario.setBackground(Estilo.FONDO); txtSalarioDiario.setForeground(Color.WHITE); txtSalarioDiario.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Estilo.BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10))); formPanel.add(txtSalarioDiario, gbc);

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1; cmbEstado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"}); cmbEstado.setBackground(Estilo.FONDO); cmbEstado.setForeground(Color.WHITE); cmbEstado.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Estilo.BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10))); formPanel.add(cmbEstado, gbc);

        // Botones
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton btnGuardar = new JButton("Guardar"); btnGuardar.setBackground(Estilo.VERDE); btnGuardar.setForeground(Color.WHITE); btnGuardar.setFocusPainted(false); btnGuardar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); btnGuardar.addActionListener(e -> guardarColaborador()); formPanel.add(btnGuardar, gbc);

        gbc.gridy = 5; JButton btnLimpiar = new JButton("Limpiar"); btnLimpiar.setBackground(new Color(0x6C, 0x75, 0x7D)); btnLimpiar.setForeground(Color.WHITE); btnLimpiar.setFocusPainted(false); btnLimpiar.addActionListener(e -> limpiarFormulario()); formPanel.add(btnLimpiar, gbc);

        add(formPanel, BorderLayout.NORTH);

        // Panel central - Tabla
        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(Estilo.FONDO);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel tableTitle = new JLabel("Lista de Colaboradores"); tableTitle.setForeground(Color.WHITE); tableTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18)); tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Num. Empleado", "Nombre Completo", "Salario Diario", "Estado"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tableColaboradores = new JTable(tableModel);
        tableColaboradores.setBackground(Estilo.SUPERFICIE);
        tableColaboradores.setForeground(Color.WHITE);
        tableColaboradores.setGridColor(Estilo.BORDE);
        tableColaboradores.setSelectionBackground(Estilo.VERDE);
        tableColaboradores.setRowHeight(30);

        JTableHeader header = tableColaboradores.getTableHeader();
        header.setBackground(Estilo.VERDE);
        header.setForeground(Color.WHITE);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        tableColaboradores.setIntercellSpacing(new Dimension(2, 2));
        tableColaboradores.getTableHeader().setPreferredSize(new Dimension(0, 25));

        JScrollPane scrollPane = new JScrollPane(tableColaboradores);
        scrollPane.setBackground(Estilo.SUPERFICIE);
        scrollPane.getViewport().setBackground(Estilo.SUPERFICIE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        var sorter = new TableRowSorter<>(tableModel);
        tableColaboradores.setRowSorter(sorter);
        JTextField busqueda = new JTextField(18);
        JButton buscar = new JButton("Buscar");
        buscar.addActionListener(e -> sorter.setRowFilter(RowFilter.regexFilter(
                "(?iu)" + java.util.regex.Pattern.quote(busqueda.getText().strip()), 1, 2)));
        busqueda.addActionListener(e -> buscar.doClick());
        JButton editar = new JButton("Editar seleccionado");
        editar.addActionListener(e -> cargarSeleccion());
        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> cargarColaboradores());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(new JLabel("Número o nombre"));
        acciones.add(busqueda);
        acciones.add(buscar);
        acciones.add(editar);
        acciones.add(actualizar);
        tablePanel.add(acciones, BorderLayout.SOUTH);

        add(tablePanel, BorderLayout.CENTER);

        cargarColaboradores();

    }

    private void cargarColaboradores() {
        try {
            List<Colaborador> colaboradores = colaboradorService.listarTodos();
            tableModel.setRowCount(0);
            for (Colaborador c : colaboradores) {
                tableModel.addRow(new Object[]{c.getId(), c.getNumeroEmpleado(), c.getNombreCompleto(), c.getSalarioDiario(), c.getEstado()});
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar colaboradores: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar la lista de colaboradores", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarSeleccion() {
        int fila = tableColaboradores.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un colaborador.");
            return;
        }
        int indice = tableColaboradores.convertRowIndexToModel(fila);
        colaboradorEnEdicion = (Integer) tableModel.getValueAt(indice, 0);
        txtNumeroEmpleado.setText(tableModel.getValueAt(indice, 1).toString());
        txtNombreCompleto.setText(tableModel.getValueAt(indice, 2).toString());
        txtSalarioDiario.setText(tableModel.getValueAt(indice, 3).toString());
        cmbEstado.setSelectedItem(tableModel.getValueAt(indice, 4));
    }

    private void guardarColaborador() {
        boolean alta = colaboradorEnEdicion == null;
        Colaborador colaborador = new Colaborador();
        colaborador.setId(colaboradorEnEdicion);
        colaborador.setNumeroEmpleado(txtNumeroEmpleado.getText().trim());
        colaborador.setNombreCompleto(txtNombreCompleto.getText().trim());
        colaborador.setEstado((String) cmbEstado.getSelectedItem());

        try {
            colaborador.setSalarioDiario(new java.math.BigDecimal(txtSalarioDiario.getText().trim())
                    .setScale(2, java.math.RoundingMode.UNNECESSARY).doubleValue());
            var actor = mainFrame.getUsuario();
            if (alta) {
                colaboradorService.guardar(actor, colaborador);
            } else {
                colaboradorService.actualizar(actor, colaborador);
            }
            try {
                mainFrame.getBitacoraService().registrar(actor.getId(),
                        alta ? "ALTA_COLABORADOR" : "MODIFICAR_COLABORADOR", "colaborador",
                        colaborador.getId(), "Empleado " + colaborador.getNumeroEmpleado()
                                + " con estado " + colaborador.getEstado());
            } catch (SQLException ex) {
                System.err.println("Error al registrar bitácora: " + ex.getMessage());
            }
            cargarColaboradores();
            limpiarFormulario();
            JOptionPane.showMessageDialog(this, "Colaborador guardado.");
        } catch (NumberFormatException | ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Escribe un salario con hasta dos decimales.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Error al guardar colaborador: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error al guardar el colaborador.");
        }
    }

    private void limpiarFormulario() {
        colaboradorEnEdicion = null;
        txtNumeroEmpleado.setText("");
        txtNombreCompleto.setText("");
        txtSalarioDiario.setText("");
        cmbEstado.setSelectedIndex(0);
    }
}
