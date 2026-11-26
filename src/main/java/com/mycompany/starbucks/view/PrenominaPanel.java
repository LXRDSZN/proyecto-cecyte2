package com.mycompany.starbucks.view;

import com.mycompany.starbucks.dao.PrenominaDAO.Regla;
import com.mycompany.starbucks.dao.PrenominaDAO.Resultado;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.TipoIncidencia;
import com.mycompany.starbucks.service.PrenominaService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PrenominaPanel extends JPanel {
    private final MainFrame mainFrame;
    private final PrenominaService servicio = new PrenominaService();
    private final JComboBox<PeriodoQuincenal> periodos = new JComboBox<>();
    private final JButton calcular = new JButton("Calcular");
    private final JButton enviar = new JButton("Enviar a revisión");
    private final JButton revisar = new JButton("Revisar");
    private final JButton autorizar = new JButton("Autorizar");
    private final JButton devolver = new JButton("Devolver a captura");
    private final JButton reglas = new JButton("Reglas autorizadas");
    private final JLabel estado = new JLabel("Selecciona una quincena.");
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{
        "Número de empleado", "Colaborador", "Salario diario", "Días de descuento", "Descuento preliminar", "Estado"
    }, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };

    private final JTable tabla = new JTable(modelo);
    private final JButton detalle = new JButton("Ver detalle");
    private List<Resultado> resultadosVisibles = List.of();
    private boolean ocupado;

    public PrenominaPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel titulo = new JLabel("Cálculo de Pre-nómina");
        titulo.setFont(titulo.getFont().deriveFont(22f));
        JPanel superior = new JPanel(new BorderLayout(0, 8));
        superior.add(titulo, BorderLayout.NORTH);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(new JLabel("Quincena"));
        acciones.add(periodos);
        acciones.add(calcular);
        acciones.add(reglas);
        acciones.add(detalle);
        acciones.add(enviar);
        acciones.add(revisar);
        acciones.add(autorizar);
        acciones.add(devolver);
        superior.add(acciones, BorderLayout.CENTER);
        superior.add(estado, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);
        tabla.setRowHeight(27);
        tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        var renderer = new DefaultListCellRenderer();
        periodos.setRenderer((lista, valor, indice, seleccionado, foco) -> renderer.getListCellRendererComponent(
                lista, valor == null ? "Selecciona una quincena" : valor.getIdentificador() + " · " + valor.getEstado(),
                indice, seleccionado, foco));
        periodos.addActionListener(e -> {
            if (!ocupado) consultar();
        });
        calcular.addActionListener(e -> calcular());
        reglas.addActionListener(e -> editarRegla());
        detalle.addActionListener(e -> verDetalle());
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarControles());
        enviar.addActionListener(e -> transicion("¿Enviar los resultados a revisión?", id -> {
            servicio.enviarARevision(mainFrame.getUsuario(), id);
        }));
        revisar.addActionListener(e -> transicionConNota("Observaciones de la revisión", (id, nota) -> {
            servicio.revisar(mainFrame.getUsuario(), id, nota);
        }));
        autorizar.addActionListener(e -> transicion("¿Autorizar y cerrar la quincena para cambios?",
                id -> servicio.autorizar(mainFrame.getUsuario(), id)));
        devolver.addActionListener(e -> transicionConNota("Motivo para devolver a captura", (id, nota) -> {
            servicio.devolverACaptura(mainFrame.getUsuario(), id, nota);
        }));
        actualizarControles();
    }

    void actualizar() {
        if (ocupado) return;
        var anterior = (PeriodoQuincenal) periodos.getSelectedItem();
        ejecutar(() -> mainFrame.getPeriodoService().listarTodos(), lista -> {
            var combo = new DefaultComboBoxModel<PeriodoQuincenal>();
            for (var periodo : lista) {
                combo.addElement(periodo);
                if (anterior != null && anterior.getId().equals(periodo.getId())) combo.setSelectedItem(periodo);
            }
            periodos.setModel(combo);
        }, true);
    }

    private void consultar() {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        mostrarResultados(List.of());
        if (periodo == null) {
            estado.setText("No hay quincenas disponibles.");
            actualizarControles();
            return;
        }
        ejecutar(() -> servicio.consultar(periodo.getId()), resultados -> {
            mostrarResultados(resultados);
            estado.setText(resultados.isEmpty() ? "La quincena aún no tiene resultados." : "Resultados guardados.");
        }, false);
    }

    private void calcular() {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        if (periodo == null) return;
        ejecutar(() -> servicio.calcular(mainFrame.getUsuario(), periodo.getId()), resultados -> {
            mostrarResultados(resultados);
            estado.setText("Resultados guardados para " + resultados.size() + " colaboradores.");
        }, false);
    }

    private void editarRegla() {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        if (periodo == null) return;
        ejecutar(() -> servicio.consultarReglas(periodo.getId()), existentes -> {
            if (!puedeEditar()) {
                JTextArea texto = new JTextArea(8, 42);
                texto.setEditable(false);
                texto.setLineWrap(true);
                texto.setWrapStyleWord(true);
                if (existentes.isEmpty()) texto.append("La quincena no tiene reglas autorizadas.");
                existentes.forEach((tipo, regla) -> texto.append(tipo + " · "
                        + (regla.descuenta() ? "Descontar por día" : "Sin descuento") + " · " + regla.referencia() + "\n"));
                JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Reglas autorizadas", JOptionPane.PLAIN_MESSAGE);
                estado.setText("Consulta de reglas finalizada.");
                return;
            }
            JComboBox<TipoIncidencia> tipos = new JComboBox<>(TipoIncidencia.values());
            JComboBox<String> decisiones = new JComboBox<>(new String[]{"Selecciona el tratamiento", "Sin descuento", "Descontar por día"});
            JTextField referencia = new JTextField(24);
            Runnable cargar = () -> {
                Regla regla = existentes.get(tipos.getSelectedItem());
                decisiones.setSelectedIndex(regla == null ? 0 : regla.descuenta() ? 2 : 1);
                referencia.setText(regla == null ? "" : regla.referencia());
            };
            tipos.addActionListener(e -> cargar.run());
            cargar.run();
            JPanel form = new JPanel(new GridLayout(0, 1, 0, 5));
            form.add(new JLabel("Tipo de incidencia"));
            form.add(tipos);
            form.add(new JLabel("Tratamiento aprobado"));
            form.add(decisiones);
            form.add(new JLabel("Referencia de la aprobación"));
            form.add(referencia);
            estado.setText("Consulta de reglas finalizada.");
            while (JOptionPane.showConfirmDialog(this, form, "Autorizar regla de la quincena",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                if (decisiones.getSelectedIndex() == 0 || referencia.getText().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Selecciona el tratamiento e indica la referencia.");
                    continue;
                }
                var tipo = (TipoIncidencia) tipos.getSelectedItem();
                boolean descuenta = decisiones.getSelectedIndex() == 2;
                String documento = referencia.getText();
                ejecutar(() -> {
                    servicio.autorizarRegla(mainFrame.getUsuario(), periodo.getId(), tipo, descuenta, documento);
                    return null;
                }, ignorado -> estado.setText("Regla autorizada para " + tipo + "."), true);
                return;
            }
        }, false);
    }

    @FunctionalInterface
    private interface Cambio {
        void ejecutar(int id) throws SQLException;
    }

    @FunctionalInterface
    private interface CambioConNota {
        void ejecutar(int id, String nota) throws SQLException;
    }

    private void transicion(String mensaje, Cambio cambio) {
        if (JOptionPane.showConfirmDialog(this, mensaje, "Pre-nómina", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            aplicarCambio(cambio);
        }
    }

    private void transicionConNota(String mensaje, CambioConNota cambio) {
        String nota = JOptionPane.showInputDialog(this, mensaje);
        if (nota != null) aplicarCambio(id -> cambio.ejecutar(id, nota));
    }

    private void aplicarCambio(Cambio cambio) {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        if (periodo == null) return;
        ejecutar(() -> {
            cambio.ejecutar(periodo.getId());
            return null;
        }, ignorado -> actualizar(), false);
    }

    private void verDetalle() {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        int fila = tabla.getSelectedRow();
        if (periodo == null || fila < 0) return;
        Resultado resultado = resultadosVisibles.get(tabla.convertRowIndexToModel(fila));
        ejecutar(() -> servicio.consultarDetalle(periodo.getId(), resultado.colaborador()), datos -> {
            JTextArea texto = new JTextArea(12, 48);
            texto.setEditable(false);
            texto.append("Días trabajados registrados: " + (datos.diasTrabajados() == null ? "Sin captura"
                    : datos.diasTrabajados()) + "\n");
            texto.append("Días de descuento guardados: " + resultado.diasDescuento() + "\n");
            texto.append("Descuento preliminar guardado: " + resultado.importe() + "\n\n");
            for (var incidencia : datos.incidencias()) {
                texto.append(incidencia.tipo() + " · " + incidencia.inicio()
                        + (java.util.Objects.equals(incidencia.inicio(), incidencia.fin()) ? "" : " a " + incidencia.fin())
                        + " · " + incidencia.estado() + "\n");
            }
            if (datos.incidencias().isEmpty()) texto.append("Sin incidencias vigentes.");
            JOptionPane.showMessageDialog(this, new JScrollPane(texto), resultado.nombre(), JOptionPane.PLAIN_MESSAGE);
            estado.setText("Detalle consultado.");
        }, false);
    }

    private void mostrarResultados(List<Resultado> resultados) {
        resultadosVisibles = resultados;
        modelo.setRowCount(0);
        for (Resultado fila : resultados) {
            modelo.addRow(new Object[]{fila.numero(), fila.nombre(), fila.salario(), fila.diasDescuento(),
                fila.importe(), fila.estado()});
        }
    }

    private boolean puedeEditar() {
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        return periodo != null && "CAPTURA".equals(periodo.getEstado())
                && "ADMINISTRADOR".equals(mainFrame.getUsuario().getRol());
    }

    private void actualizarControles() {
        periodos.setEnabled(!ocupado);
        calcular.setEnabled(!ocupado && puedeEditar());
        var periodo = (PeriodoQuincenal) periodos.getSelectedItem();
        String rol = mainFrame.getUsuario().getRol();
        boolean enRevision = !ocupado && periodo != null && "REVISION".equals(periodo.getEstado())
                && ("ADMINISTRADOR".equals(rol) || "REVISOR".equals(rol));
        reglas.setEnabled(!ocupado && periodo != null);
        detalle.setEnabled(!ocupado && periodo != null && tabla.getSelectedRow() >= 0);
        enviar.setEnabled(!ocupado && puedeEditar() && modelo.getRowCount() != 0);
        boolean revisados = modelo.getRowCount() > 0 && "REVISADO".equals(modelo.getValueAt(0, 5));
        revisar.setEnabled(enRevision && !revisados && modelo.getRowCount() > 0);
        autorizar.setEnabled(enRevision && revisados);
        devolver.setEnabled(enRevision);
    }

    private <T> void ejecutar(Callable<T> tarea, Consumer<T> aplicar, boolean recargar) {
        ocupado = true;
        actualizarControles();
        estado.setText("Consultando MariaDB…");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarea.call(); }

            @Override protected void done() {
                try {
                    T valor = get();
                    if (!recargar) {
                        ocupado = false;
                        actualizarControles();
                    }
                    aplicar.accept(valor);
                    actualizarControles();
                    if (recargar) {
                        ocupado = false;
                        consultar();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    ocupado = false;
                    actualizarControles();
                    estado.setText("Operación interrumpida.");
                } catch (ExecutionException ex) {
                    ocupado = false;
                    actualizarControles();
                    var causa = ex.getCause();
                    if (!(causa instanceof IllegalArgumentException)) {
                        System.err.println("Error en pre-nómina: " + causa);
                    }
                    estado.setText(causa instanceof IllegalArgumentException
                            ? causa.getMessage() : "Error de base de datos. Se conservaron los resultados anteriores.");
                }
            }
        }.execute();
    }
}
