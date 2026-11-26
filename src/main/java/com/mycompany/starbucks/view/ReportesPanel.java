package com.mycompany.starbucks.view;

import com.mycompany.starbucks.dao.ReporteDAO.Datos;
import com.mycompany.starbucks.dao.ReporteDAO.Filtros;
import com.mycompany.starbucks.dao.ReporteDAO.Tipo;
import com.mycompany.starbucks.model.TipoIncidencia;
import com.mycompany.starbucks.service.ReporteService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class ReportesPanel extends JPanel {
    private record Opcion(Integer id, String etiqueta) {
        @Override public String toString() { return etiqueta; }
    }
    private record Catalogos(List<Opcion> periodos, List<Opcion> colaboradores) {}

    private final MainFrame mainFrame;
    private final ReporteService servicio = new ReporteService();
    private final JComboBox<Tipo> tipos = new JComboBox<>(Tipo.values());
    private final JComboBox<Opcion> periodos = new JComboBox<>();
    private final JComboBox<Opcion> colaboradores = new JComboBox<>();
    private final JComboBox<String> incidencias = new JComboBox<>();
    private final JComboBox<String> estados = new JComboBox<>();
    private final JButton consultar = new JButton("Consultar");
    private final JButton exportar = new JButton("Exportar CSV");
    private final JLabel estado = new JLabel("Selecciona un reporte.");
    private final JTable tabla = new JTable();
    private Datos datos;
    private boolean ocupado;
    private boolean ajustando;

    public ReportesPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel superior = new JPanel(new BorderLayout(0, 8));
        JLabel titulo = new JLabel("Reportes de Pre-nómina");
        titulo.setFont(titulo.getFont().deriveFont(22f));
        superior.add(titulo, BorderLayout.NORTH);

        JPanel filtros = new JPanel(new java.awt.GridLayout(0, 1));
        JPanel alcance = new JPanel(new FlowLayout(FlowLayout.LEFT));
        alcance.add(new JLabel("Reporte"));
        alcance.add(tipos);
        alcance.add(new JLabel("Quincena"));
        alcance.add(periodos);
        alcance.add(new JLabel("Colaborador"));
        alcance.add(colaboradores);
        filtros.add(alcance);

        JPanel detalle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        detalle.add(new JLabel("Tipo de incidencia"));
        detalle.add(incidencias);
        detalle.add(new JLabel("Estado"));
        detalle.add(estados);
        detalle.add(consultar);
        detalle.add(exportar);
        filtros.add(detalle);
        superior.add(filtros, BorderLayout.CENTER);
        superior.add(estado, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setAutoCreateRowSorter(true);
        tabla.setRowHeight(26);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        incidencias.addItem("Todos");
        for (var tipo : TipoIncidencia.values()) incidencias.addItem(tipo.name());
        tipos.addActionListener(e -> cambiarTipo());
        for (var filtro : List.of(periodos, colaboradores, incidencias, estados)) {
            filtro.addActionListener(e -> {
                if (!ajustando) limpiarConsulta();
            });
        }
        consultar.addActionListener(e -> consultar());
        exportar.addActionListener(e -> exportar());
        cambiarTipo();
    }

    void actualizar() {
        if (ocupado) return;
        ejecutar(() -> new Catalogos(mainFrame.getPeriodoService().listarTodos().stream()
                .map(p -> new Opcion(p.getId(), p.getIdentificador())).toList(),
                mainFrame.getColaboradorService().listarTodos().stream()
                .map(c -> new Opcion(c.getId(), c.getNumeroEmpleado() + " · " + c.getNombreCompleto())).toList()),
                catalogos -> {
                    ajustando = true;
                    try {
                        actualizarCombo(periodos, catalogos.periodos(), "Todas");
                        actualizarCombo(colaboradores, catalogos.colaboradores(), "Todos");
                    } finally {
                        ajustando = false;
                    }
                    limpiarConsulta();
                });
    }

    private void actualizarCombo(JComboBox<Opcion> combo, List<Opcion> opciones, String general) {
        Integer anterior = idSeleccionado(combo);
        var modelo = new DefaultComboBoxModel<>(new Opcion[]{new Opcion(null, general)});
        for (var opcion : opciones) {
            modelo.addElement(opcion);
            if (opcion.id().equals(anterior)) modelo.setSelectedItem(opcion);
        }
        combo.setModel(modelo);
        combo.setPrototypeDisplayValue(new Opcion(null, "0000 · Nombre de ejemplo"));
    }

    private void cambiarTipo() {
        var tipo = (Tipo) tipos.getSelectedItem();
        ajustando = true;
        try {
            estados.removeAllItems();
            estados.addItem("Todos");
            var valores = switch (tipo) {
                case PERSONAL -> List.of("ACTIVO", "INACTIVO");
                case INCIDENCIAS -> List.of("PENDIENTE", "REVISADO", "AUTORIZADO", "CANCELADA");
                case DESCUENTOS -> List.of("PENDIENTE", "REVISION", "REVISADO", "AUTORIZADO", "DESACTUALIZADO");
                case REVISIONES -> List.of("CAPTURA", "REVISION", "REVISADO", "AUTORIZADO");
            };
            valores.forEach(estados::addItem);
        } finally {
            ajustando = false;
        }
        limpiarConsulta();
    }

    private void limpiarConsulta() {
        datos = null;
        tabla.setModel(new DefaultTableModel());
        estado.setText("Pulsa Consultar para cargar el reporte.");
        actualizarControles();
    }

    private void consultar() {
        var filtros = new Filtros((Tipo) tipos.getSelectedItem(), idSeleccionado(periodos),
                idSeleccionado(colaboradores), textoSeleccionado(incidencias), textoSeleccionado(estados));
        ejecutar(() -> servicio.consultar(mainFrame.getUsuario(), filtros), resultado -> {
            datos = resultado;
            var modelo = new DefaultTableModel(resultado.columnas().toArray(), 0) {
                @Override public boolean isCellEditable(int fila, int columna) { return false; }
            };
            resultado.filas().forEach(fila -> modelo.addRow(fila.toArray()));
            tabla.setModel(modelo);
            for (int i = 0; i < tabla.getColumnCount(); i++) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(
                        Math.max(125, tabla.getColumnName(i).length() * 8 + 22));
            }
            estado.setText(resultado.filas().isEmpty() ? "Sin resultados para los filtros seleccionados."
                    : "Registros encontrados: " + resultado.filas().size());
        });
    }

    private void exportar() {
        if (datos == null) return;
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar CSV");
        selector.setFileFilter(new FileNameExtensionFilter("Archivo CSV", "csv"));
        selector.setSelectedFile(new java.io.File("starbucks-reporte.csv"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path elegido = selector.getSelectedFile().toPath();
        Path destino = elegido.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")
                ? elegido : elegido.resolveSibling(elegido.getFileName() + ".csv");
        if (Files.exists(destino) && JOptionPane.showConfirmDialog(this, "¿Reemplazar el archivo existente?",
                "Exportar CSV", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        List<List<Object>> filas = new ArrayList<>();
        for (int i = 0; i < tabla.getRowCount(); i++) {
            filas.add(datos.filas().get(tabla.convertRowIndexToModel(i)));
        }
        List<String> columnas = new ArrayList<>();
        for (int i = 0; i < tabla.getColumnCount(); i++) columnas.add(tabla.getColumnName(i));
        List<List<Object>> ordenadas = new ArrayList<>();
        for (var fila : filas) {
            List<Object> ordenada = new ArrayList<>();
            for (int i = 0; i < tabla.getColumnCount(); i++) ordenada.add(fila.get(tabla.convertColumnIndexToModel(i)));
            ordenadas.add(ordenada);
        }
        var salida = new Datos(columnas, ordenadas);
        ejecutar(() -> {
            servicio.exportarCsv(destino, salida);
            return destino;
        }, archivo -> estado.setText("CSV guardado en " + archivo));
    }

    private Integer idSeleccionado(JComboBox<Opcion> combo) {
        var seleccion = (Opcion) combo.getSelectedItem();
        return seleccion == null ? null : seleccion.id();
    }

    private String textoSeleccionado(JComboBox<String> combo) {
        return combo.getSelectedIndex() <= 0 ? null : (String) combo.getSelectedItem();
    }

    private void actualizarControles() {
        var tipo = (Tipo) tipos.getSelectedItem();
        tipos.setEnabled(!ocupado);
        periodos.setEnabled(!ocupado && tipo != Tipo.PERSONAL);
        colaboradores.setEnabled(!ocupado && tipo != Tipo.REVISIONES);
        incidencias.setEnabled(!ocupado && tipo == Tipo.INCIDENCIAS);
        estados.setEnabled(!ocupado);
        consultar.setEnabled(!ocupado);
        exportar.setEnabled(!ocupado && datos != null);
    }

    private <T> void ejecutar(Callable<T> tarea, Consumer<T> completar) {
        ocupado = true;
        actualizarControles();
        estado.setText("Procesando…");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarea.call(); }
            @Override protected void done() {
                ocupado = false;
                try {
                    completar.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    estado.setText("Operación interrumpida.");
                } catch (ExecutionException ex) {
                    var causa = ex.getCause();
                    if (!(causa instanceof IllegalArgumentException)) System.err.println("Error en reportes: " + causa);
                    estado.setText(causa instanceof IllegalArgumentException ? causa.getMessage()
                            : "No se pudo completar la operación de reportes.");
                } finally {
                    actualizarControles();
                }
            }
        }.execute();
    }
}
