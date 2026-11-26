package com.mycompany.starbucks.view;

import com.mycompany.starbucks.dao.UsuarioDAO.Cuenta;
import com.mycompany.starbucks.model.Rol;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class UsuariosPanel extends JPanel {
    private final MainFrame mainFrame;
    private final DefaultTableModel modelo = modelo("ID", "Usuario", "Rol", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final JButton crear = new JButton("Nuevo usuario");
    private final JButton editar = new JButton("Editar");
    private final JButton clave = new JButton("Cambiar contraseña");
    private final JButton estado = new JButton("Cambiar estado");
    private final JButton actualizar = new JButton("Actualizar");
    private final JButton bitacora = new JButton("Ver bitácora");
    private final JLabel mensaje = new JLabel(" ");
    private List<Cuenta> cuentas = List.of();
    private boolean ocupado;

    public UsuariosPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel titulo = new JLabel("Gestión de Usuarios");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        JPanel encabezado = new JPanel(new BorderLayout(0, 8));
        encabezado.add(titulo, BorderLayout.NORTH);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton boton : List.of(crear, editar, clave, estado, actualizar, bitacora)) acciones.add(boton);
        encabezado.add(acciones, BorderLayout.CENTER);
        encabezado.add(mensaje, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);
        tabla.setRowHeight(30);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarControles());
        crear.addActionListener(e -> formulario(null));
        editar.addActionListener(e -> formulario(seleccion()));
        clave.addActionListener(e -> cambiarClave());
        estado.addActionListener(e -> cambiarEstado());
        actualizar.addActionListener(e -> actualizar());
        bitacora.addActionListener(e -> mostrarBitacora());
        actualizarControles();
    }

    void actualizar() {
        if (ocupado) return;
        ejecutar(() -> mainFrame.getUsuarioService().listar(mainFrame.getUsuario()), this::mostrar);
    }

    private void mostrar(List<Cuenta> datos) {
        tabla.clearSelection();
        cuentas = datos;
        modelo.setRowCount(0);
        for (Cuenta cuenta : cuentas) {
            modelo.addRow(new Object[]{cuenta.id(), cuenta.nombre(), cuenta.rol(), cuenta.estado()});
        }
        mensaje.setText("Cuentas registradas: " + cuentas.size());
    }

    private Cuenta seleccion() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : cuentas.get(tabla.convertRowIndexToModel(fila));
    }

    private void formulario(Cuenta cuenta) {
        JTextField nombre = new JTextField(cuenta == null ? "" : cuenta.nombre(), 24);
        JComboBox<Rol> roles = new JComboBox<>(Rol.values());
        roles.setSelectedItem(cuenta == null ? Rol.CAPTURISTA : cuenta.rol());
        JComboBox<String> estados = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});
        if (cuenta != null) estados.setSelectedItem(cuenta.estado());
        JPasswordField password = new JPasswordField(24);
        JPasswordField confirmar = new JPasswordField(24);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 6));
        agregarCampo(campos, "Usuario", nombre);
        agregarCampo(campos, "Rol", roles);
        agregarCampo(campos, "Estado", estados);
        if (cuenta == null) {
            agregarCampo(campos, "Contraseña", password);
            agregarCampo(campos, "Confirmar contraseña", confirmar);
        }
        if (JOptionPane.showConfirmDialog(this, campos, cuenta == null ? "Nuevo usuario" : "Editar usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String usuario = nombre.getText();
        Rol rol = (Rol) roles.getSelectedItem();
        String nuevoEstado = (String) estados.getSelectedItem();
        String primera = leerClave(password);
        String segunda = leerClave(confirmar);
        guardar(() -> {
            var servicio = mainFrame.getUsuarioService();
            var actor = mainFrame.getUsuario();
            if (cuenta == null) servicio.crear(actor, usuario, rol, nuevoEstado, primera, segunda);
            else servicio.editar(actor, cuenta.id(), usuario, rol, nuevoEstado);
        });
    }

    private void cambiarClave() {
        Cuenta cuenta = seleccion();
        if (cuenta == null) return;
        JPasswordField password = new JPasswordField(24);
        JPasswordField confirmar = new JPasswordField(24);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 6));
        agregarCampo(campos, "Nueva contraseña", password);
        agregarCampo(campos, "Confirmar contraseña", confirmar);
        if (JOptionPane.showConfirmDialog(this, campos, "Cambiar contraseña de " + cuenta.nombre(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String primera = leerClave(password);
        String segunda = leerClave(confirmar);
        guardar(() -> mainFrame.getUsuarioService().cambiarClave(mainFrame.getUsuario(), cuenta.id(), primera, segunda));
    }

    private void cambiarEstado() {
        Cuenta cuenta = seleccion();
        if (cuenta == null) return;
        boolean activar = !"ACTIVO".equals(cuenta.estado());
        String accion = activar ? "activar" : "desactivar";
        if (JOptionPane.showConfirmDialog(this, "¿Confirmas " + accion + " a " + cuenta.nombre() + "?",
                "Cambiar estado", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            guardar(() -> mainFrame.getUsuarioService().cambiarEstado(mainFrame.getUsuario(), cuenta.id(), activar));
        }
    }

    private void mostrarBitacora() {
        ejecutar(() -> mainFrame.getUsuarioService().bitacora(mainFrame.getUsuario()), eventos -> {
            var historial = modelo("Fecha y hora", "Usuario", "Acción", "Entidad", "Identificador", "Detalle");
            for (var evento : eventos) {
                historial.addRow(new Object[]{evento.fecha(), evento.usuario(), evento.accion(),
                    evento.entidad(), evento.id(), evento.detalle()});
            }
            var tablaHistorial = new JTable(historial);
            tablaHistorial.setRowHeight(27);
            tablaHistorial.setAutoCreateRowSorter(true);
            var scroll = new JScrollPane(tablaHistorial);
            scroll.setPreferredSize(new java.awt.Dimension(880, 390));
            mensaje.setText("Eventos en bitácora: " + eventos.size());
            JOptionPane.showMessageDialog(this, scroll, "Bitácora de cambios", JOptionPane.PLAIN_MESSAGE);
        });
    }

    @FunctionalInterface
    private interface Cambio {
        void guardar() throws SQLException;
    }

    private void guardar(Cambio cambio) {
        ejecutar(() -> {
            cambio.guardar();
            return mainFrame.getUsuarioService().listar(mainFrame.getUsuario());
        }, datos -> {
            mostrar(datos);
            mensaje.setText("Cambios guardados.");
        });
    }

    private <T> void ejecutar(Callable<T> accion, Consumer<T> completar) {
        ocupado = true;
        actualizarControles();
        mensaje.setText("Procesando…");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return accion.call(); }
            @Override protected void done() {
                try {
                    completar.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mensaje.setText("Operación interrumpida.");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    if (causa instanceof IllegalArgumentException) mensaje.setText(causa.getMessage());
                    else {
                        mensaje.setText("No se pudo completar la operación de usuarios.");
                        System.err.println("Error en usuarios: " + causa);
                    }
                } finally {
                    ocupado = false;
                    actualizarControles();
                }
            }
        }.execute();
    }

    private void actualizarControles() {
        boolean seleccionado = !ocupado && seleccion() != null;
        editar.setEnabled(seleccionado);
        clave.setEnabled(seleccionado);
        estado.setEnabled(seleccionado);
        crear.setEnabled(!ocupado);
        actualizar.setEnabled(!ocupado);
        bitacora.setEnabled(!ocupado);
        tabla.setEnabled(!ocupado);
    }

    private static void agregarCampo(JPanel panel, String etiqueta, JComponent campo) {
        panel.add(new JLabel(etiqueta));
        panel.add(campo);
    }

    private static String leerClave(JPasswordField campo) {
        char[] valor = campo.getPassword();
        try {
            return new String(valor);
        } finally {
            Arrays.fill(valor, '\0');
            campo.setText("");
        }
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }
}
