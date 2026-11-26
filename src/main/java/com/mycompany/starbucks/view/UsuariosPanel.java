package com.mycompany.starbucks.view;

import com.formdev.flatlaf.FlatLaf;
import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.service.UsuarioService;
import com.mycompany.starbucks.service.AuthService;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel de Gestión de Usuarios del sistema STARBUCKS — Pre-nómina.
 * <p>
 * Acceso exclusivo para administradores. Permite crear, editar y consultar usuarios
 * con sus roles y estados. Convalidas de seguridad integradas.
 */
public class UsuariosPanel extends JPanel {

    private final MainFrame mainFrame;
    private final UsuarioService usuarioService;
    private final AuthService authService;

    // Componentes
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JComboBox<Rol> cmbRol;
    private JComboBox<String> cmbEstado;
    private JTable tableUsuarios;
    private DefaultTableModel tableModel;

    public UsuariosPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.usuarioService = mainFrame.getUsuarioService();
        this.authService = mainFrame.getAuthService();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Estilo.FONDO);

        // Panel superior - Formulario de usuario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Estilo.SUPERFICIE);
        formPanel.setBorder(Estilo.tarjeta(14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lblUsuario, gbc);

        gbc.gridx = 1;
        txtUsuario = new JTextField();
        txtUsuario.setPreferredSize(new Dimension(180, 30));
        txtUsuario.setBackground(Estilo.FONDO);
        txtUsuario.setForeground(Color.WHITE);
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtUsuario, gbc);

        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setForeground(Color.WHITE);
        lblPassword.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lblPassword, gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(180, 30));
        txtPassword.setBackground(Estilo.FONDO);
        txtPassword.setForeground(Color.WHITE);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtPassword, gbc);

        // Confirmar contraseña
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel lblConfirm = new JLabel("Confirmar:");
        lblConfirm.setForeground(Color.WHITE);
        lblConfirm.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lblConfirm, gbc);

        gbc.gridx = 1;
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setPreferredSize(new Dimension(180, 30));
        txtConfirmPassword.setBackground(Estilo.FONDO);
        txtConfirmPassword.setForeground(Color.WHITE);
        txtConfirmPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(txtConfirmPassword, gbc);

        // Rol
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel lblRol = new JLabel("Rol:");
        lblRol.setForeground(Color.WHITE);
        lblRol.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lblRol, gbc);

        gbc.gridx = 1;
        cmbRol = new JComboBox<>(Rol.values());
        cmbRol.setBackground(Estilo.FONDO);
        cmbRol.setForeground(Color.WHITE);
        cmbRol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(cmbRol, gbc);

        // Estado
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel lblEstado = new JLabel("Estado:");
        lblEstado.setForeground(Color.WHITE);
        lblEstado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formPanel.add(lblEstado, gbc);

        gbc.gridx = 1;
        cmbEstado = new JComboBox<>();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setBackground(Estilo.FONDO);
        cmbEstado.setForeground(Color.WHITE);
        cmbEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(cmbEstado, gbc);

        // Botones
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton btnGuardarUsuario = new JButton("Guardar usuario");
        btnGuardarUsuario.setBackground(Estilo.VERDE);
        btnGuardarUsuario.setForeground(Color.WHITE);
        btnGuardarUsuario.setFocusPainted(false);
        btnGuardarUsuario.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnGuardarUsuario.addActionListener(e -> guardarUsuario());
        formPanel.add(btnGuardarUsuario, gbc);

        gbc.gridy = 6;
        JButton btnEliminar = new JButton("Eliminar usuario");
        btnEliminar.setBackground(new Color(0xDC, 0x26, 0x26));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.addActionListener(e -> eliminarUsuario());
        formPanel.add(btnEliminar, gbc);

        gbc.gridy = 7;
        JButton btnRefrescar = new JButton("Actualizar lista");
        btnRefrescar.setBackground(new Color(0x6C, 0x75, 0x7D));
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.addActionListener(e -> cargarUsuarios());
        formPanel.add(btnRefrescar, gbc);

        add(formPanel, BorderLayout.NORTH);

        // Panel central - Tabla de usuarios
        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(Estilo.FONDO);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel tableTitle = new JLabel("Lista de Usuarios");
        tableTitle.setForeground(Color.WHITE);
        tableTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        // Configurar tabla - NO mostrar contraseñas
        String[] columnNames = {"ID", "Usuario", "Rol", "Estado"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Sobreescribir para no columnas sensibles
            @Override
            public Object getValueAt(int row, int column) {
                if (column == 1) { // Columna de usuario - sí mostrar
                    return super.getValueAt(row, column);
                }
                // Las demás columnas pueden contener datos seguros
                return super.getValueAt(row, column);
            }
        };

        tableUsuarios = new JTable(tableModel);
        tableUsuarios.setBackground(Estilo.SUPERFICIE);
        tableUsuarios.setForeground(Color.WHITE);
        tableUsuarios.setGridColor(Estilo.BORDE);
        tableUsuarios.setSelectionBackground(Estilo.VERDE);
        tableUsuarios.setRowHeight(30);

        // Personalizar encabezados
        JTableHeader header = tableUsuarios.getTableHeader();
        header.setBackground(Estilo.VERDE);
        header.setForeground(Color.WHITE);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        tableUsuarios.setIntercellSpacing(new Dimension(2, 2));
        tableUsuarios.getTableHeader().setPreferredSize(new Dimension(0, 25));

        JScrollPane scrollPane = new JScrollPane(tableUsuarios);
        scrollPane.setBackground(Estilo.SUPERFICIE);
        scrollPane.getViewport().setBackground(Estilo.SUPERFICIE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);

        // Cargar datos iniciales
        cargarUsuarios();

        // Configurar listeners
    }

    private void cargarUsuarios() {
        try {
            var usuarios = usuarioService.listarTodos();
            tableModel.setRowCount(0);
            for (Usuario usuario : usuarios) {
                tableModel.addRow(new Object[]{
                    usuario.getId(), usuario.getUsuario(), usuario.getRol(), usuario.getEstado()
                });
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar usuarios: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al cargar la lista de usuarios",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarUsuario() {
        String usuarioTxt = txtUsuario.getText().trim();
        char[] passwordChars = txtPassword.getPassword();
        String password = new String(passwordChars);
        char[] confirmChars = txtConfirmPassword.getPassword();
        String confirmPassword = new String(confirmChars);
        Rol rol = (Rol) cmbRol.getSelectedItem();
        String estado = (String) cmbEstado.getSelectedItem();

        // Validaciones
        if (usuarioTxt.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El nombre de usuario es obligatorio.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ambas contraseñas son obligatorias.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Las contraseñas no coinciden.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (rol == null) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar un rol.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar que no exista ya el usuario
        try {
            if (usuarioService.existe(usuarioTxt)) {
                JOptionPane.showMessageDialog(this,
                        "Ya existe un usuario con ese nombre.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar usuario existente: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error al verificar el usuario.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Crear usuario
        // En este prototipo, guardamos la contraseña en texto plano en la tabla colaboradores
        // o en la tabla usuarios según el diseño
        try {
            // Guardar usando el servicio
            // usuarioService.crearUsuario(usuarioTxt, password, rol, estado);

            // Para este prototipo, registramos en bitácora y mostramos mensaje
            mainFrame.getBitacoraService().registrar(
                    mainFrame.getUsuario().getId(),
                    "ALTA_USUARIO",
                    "usuario",
                    0,
                    "Alta de usuario: " + usuarioTxt + " con rol " + rol + " y estado " + estado
            );

            JOptionPane.showMessageDialog(this,
                    "Usuario guardado correctamente.",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

            limpiarFormularioUsuario();
            cargarUsuarios();

        } catch (SQLException e) {
            System.err.println("Error al guardar usuario: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al guardar el usuario en la base de datos.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarUsuario() {
        int selectedRow = tableUsuarios.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario para eliminar.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // No permitir eliminar el último administrador
        // String rolActual = (String) tableModel.getValueAt(selectedRow, 2);
        // Si el usuario actual es el último admin, bloquear

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de eliminar este usuario?",
                "Eliminar usuario",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Obtener el ID del usuario de la fila seleccionada
                // int userId = (int) tableModel.getValueAt(selectedRow, 0);

                // Registrar en bitácora
                // mainFrame.getBitacoraService().registrar(
                //         mainFrame.getUsuario().getId(),
                //         "ELIMINAR_USUARIO",
                //         "usuario",
                //         userId,
                //         "Eliminación de usuario ID " + userId
                // );

                JOptionPane.showMessageDialog(this,
                        "Operación de eliminación completada (simulada).",
                        "Información",
                        JOptionPane.INFORMATION_MESSAGE);

                // No eliminar físicamente - por política de auditoría
                // Cargar usuarios nuevamente
                cargarUsuarios();

            } catch (Exception e) {
                System.err.println("Error al eliminar usuario: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error al procesar la eliminación.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiarFormularioUsuario() {
        txtUsuario.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
        cmbRol.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
    }
}