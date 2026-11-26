package com.mycompany.starbucks.view;

import com.mycompany.starbucks.model.Incidencia;
import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.service.ColaboradorService;
import com.mycompany.starbucks.service.IncidenciaService;
import com.mycompany.starbucks.service.PeriodoQuincenalService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel del dashboard principal del sistema STARBUCKS — Pre-nómina.
 * <p>
 * Muestra cuatro tarjetas en distribución 2x2 y tabla de incidencias recientes.
 * Todos los valores se obtienen desde MariaDB.
 */
public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ColaboradorService colaboradorService;
    private final PeriodoQuincenalService periodoService;
    private final IncidenciaService incidenciaService;

    // Componentes
    private JLabel lblPersonal;
    private JLabel lblIncidencias;
    private JLabel lblQuincena;
    private JLabel lblPendientes;
    private JTable tableIncidenciasRecientes;
    private DefaultTableModel tableModel;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.colaboradorService = mainFrame.getColaboradorService();
        this.periodoService = mainFrame.getPeriodoService();
        this.incidenciaService = mainFrame.getIncidenciaService();

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 6));
        JLabel titulo = new JLabel("Panel principal");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));

        JPanel tarjetas = new JPanel(new GridLayout(2, 2, 12, 12));
        tarjetas.setOpaque(false);
        lblPersonal = new JLabel("—");
        lblIncidencias = new JLabel("—");
        lblQuincena = new JLabel("—");
        lblPendientes = new JLabel("—");
        tarjetas.add(crearTarjeta("Personal", "personal", lblPersonal, Estilo.TEXTO));
        tarjetas.add(crearTarjeta("Incidencias", "incidencias", lblIncidencias, Estilo.TEXTO));
        tarjetas.add(crearTarjeta("Quincena", "periodos", lblQuincena, Estilo.MENTA));
        tarjetas.add(crearTarjeta("Pendientes", "reportes", lblPendientes, Color.decode("#FBBF6D")));

        JPanel resumen = new JPanel(new BorderLayout(0, 16));
        resumen.setOpaque(false);
        resumen.add(titulo, BorderLayout.NORTH);
        resumen.add(tarjetas, BorderLayout.CENTER);
        add(resumen, BorderLayout.NORTH);

        JPanel recientes = new JPanel(new BorderLayout(0, 12));
        recientes.setBackground(Estilo.SUPERFICIE);
        recientes.setBorder(Estilo.tarjeta(16));
        JLabel tablaTitulo = new JLabel("Incidencias recientes");
        tablaTitulo.setFont(tablaTitulo.getFont().deriveFont(Font.BOLD, 17f));
        recientes.add(tablaTitulo, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new String[]{"Colaborador", "Tipo", "Estado"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        tableIncidenciasRecientes = new JTable(tableModel);
        tableIncidenciasRecientes.setRowHeight(32);
        tableIncidenciasRecientes.getTableHeader().setPreferredSize(new Dimension(0, 33));
        tableIncidenciasRecientes.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tableIncidenciasRecientes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        recientes.add(scroll, BorderLayout.CENTER);
        add(recientes, BorderLayout.CENTER);
    }

    private JPanel crearTarjeta(String titulo, String icono, JLabel valor, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Estilo.SUPERFICIE);
        card.setBorder(Estilo.tarjeta(15));
        JLabel etiqueta = new JLabel(titulo, Estilo.icono(icono, Estilo.MENTA), SwingConstants.LEFT);
        etiqueta.setIconTextGap(9);
        etiqueta.setForeground(Estilo.SECUNDARIO);
        card.add(etiqueta, BorderLayout.NORTH);
        valor.setForeground(color);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 24f));
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private record Resumen(int personal, String quincena, long incidencias, long pendientes,
                           List<Incidencia> recientes) {
    }

    private SwingWorker<Resumen, Void> carga;

    void actualizar() {
        if (carga != null) {
            carga.cancel(true);
        }
        carga = new SwingWorker<>() {
            @Override
            protected Resumen doInBackground() throws SQLException {
                int personal = colaboradorService.listarTodos().size();
                var hoy = java.time.LocalDate.now();
                var periodo = periodoService.listarTodos().stream()
                        .filter(p -> !hoy.isBefore(p.getFechaInicio()) && !hoy.isAfter(p.getFechaFin()))
                        .findFirst().orElse(null);
                var incidencias = incidenciaService.listarTodos();
                var delPeriodo = incidencias.stream()
                        .filter(i -> periodo != null && i.getPeriodo() != null
                                && periodo.getId().equals(i.getPeriodo().getId()))
                        .toList();
                return new Resumen(personal, periodo == null ? "Sin quincena vigente" : periodo.getIdentificador(),
                        delPeriodo.size(), delPeriodo.stream().filter(i -> "PENDIENTE".equals(i.getEstado())).count(),
                        incidencias.stream().limit(5).toList());
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    Resumen datos = get();
                    lblPersonal.setText(Integer.toString(datos.personal()));
                    lblQuincena.setText(datos.quincena());
                    lblIncidencias.setText(Long.toString(datos.incidencias()));
                    lblPendientes.setText(Long.toString(datos.pendientes()));
                    tableModel.setRowCount(0);
                    for (Incidencia incidencia : datos.recientes()) {
                        tableModel.addRow(new Object[]{incidencia.getColaborador() == null ? "—"
                                : incidencia.getColaborador().getNombreCompleto(),
                                incidencia.getTipo(), incidencia.getEstado()});
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    System.err.println("Error al consultar el panel principal: " + e.getCause());
                    lblQuincena.setText("Error de consulta");
                }
            }
        };
        carga.execute();
    }
}
