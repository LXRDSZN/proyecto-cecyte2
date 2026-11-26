package com.mycompany.starbucks.view;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.service.*;
import com.mycompany.starbucks.model.Colaborador;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.sql.SQLException;

/**
 * Frame principal de la aplicación STARBUCKS — Pre-nómina.
 * <p>
 * Contiene:
 * - Header con icono, título, nombre usuario y rol
 * - Sidebar con menú de navegación
 * - Área central con CardLayout para módulos
 */
public class MainFrame extends JFrame {

    private final Usuario usuario;
    private final ColaboradorService colaboradorService;
    private final PeriodoQuincenalService periodoService;
    private final IncidenciaService incidenciaService;
    private final AuthService authService;
    private final BitacoraService bitacoraService;

    // Paneles de módulo
    private final UsuarioService usuarioService = new UsuarioService();
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel centerPanel = new JPanel(cards);
    private DashboardPanel dashboardPanel;
    private IncidenciasPanel incidenciasPanel;
    private PrenominaPanel prenominaPanel;
    private ReportesPanel reportesPanel;
    private UsuariosPanel usuariosPanel;
    private PeriodosPanel periodosPanel;
    private PersonalPanel personalPanel;

    // Botones del sidebar

    public MainFrame(Usuario usuario) {
        this.usuario = usuario;
        this.colaboradorService = new ColaboradorService();
        this.periodoService = new PeriodoQuincenalService();
        this.incidenciaService = new IncidenciaService();
        this.authService = new AuthService();
        this.bitacoraService = new BitacoraService();

        initialize();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ColaboradorService getColaboradorService() {
        return colaboradorService;
    }

    public PeriodoQuincenalService getPeriodoService() {
        return periodoService;
    }

    public IncidenciaService getIncidenciaService() {
        return incidenciaService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UsuarioService getUsuarioService() {
        return usuarioService;
    }

    private final ButtonGroup navegacion = new ButtonGroup();

    private void initialize() {
        setTitle("STARBUCKS — Pre-nómina");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setSize(1180, 760);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(14, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        raiz.add(crearEncabezado(), BorderLayout.NORTH);
        raiz.add(crearMenu(), BorderLayout.WEST);
        raiz.add(centerPanel, BorderLayout.CENTER);
        setContentPane(raiz);

        dashboardPanel = new DashboardPanel(this);
        centerPanel.add(dashboardPanel, "inicio");
        personalPanel = new PersonalPanel(this);
        periodosPanel = new PeriodosPanel(this);
        centerPanel.add(personalPanel, "personal");
        centerPanel.add(periodosPanel, "periodos");
        incidenciasPanel = new IncidenciasPanel(this);
        centerPanel.add(incidenciasPanel, "incidencias");
        prenominaPanel = new PrenominaPanel(this);
        centerPanel.add(prenominaPanel, "prenomina");
        reportesPanel = new ReportesPanel(this);
        centerPanel.add(reportesPanel, "reportes");
        if ("ADMINISTRADOR".equals(usuario.getRol())) {
            usuariosPanel = new UsuariosPanel(this);
            centerPanel.add(usuariosPanel, "usuarios");
        }
        dashboardPanel.actualizar();
    }

    private JPanel crearEncabezado() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(4, 1, 10, 1));
        JPanel marca = new JPanel(new BorderLayout(12, 0));
        marca.setOpaque(false);
        marca.add(Estilo.marca(), BorderLayout.WEST);
        JLabel titulo = new JLabel("STARBUCKS — Pre-nómina");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 23f));
        marca.add(titulo, BorderLayout.CENTER);
        barra.add(marca, BorderLayout.WEST);

        JPanel sesion = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 4));
        sesion.setOpaque(false);
        JPanel cuenta = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        cuenta.setOpaque(false);
        JLabel nombre = new JLabel(usuario.getUsuario(), SwingConstants.RIGHT);
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD));
        JLabel rol = new JLabel(usuario.getRol(), SwingConstants.RIGHT);
        rol.setFont(rol.getFont().deriveFont(11f));
        rol.setForeground(Estilo.MENTA);
        cuenta.add(nombre);
        cuenta.add(rol);
        sesion.add(cuenta);
        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> cerrarSesion());
        sesion.add(salir);
        barra.add(sesion, BorderLayout.EAST);
        return barra;
    }

    private JPanel crearMenu() {
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBackground(Estilo.MENU);
        menu.setPreferredSize(new Dimension(198, 0));
        menu.setBorder(Estilo.tarjeta(10));
        boolean administrador = "ADMINISTRADOR".equals(usuario.getRol());
        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 0, 3, 0);
        agregarOpcion(menu, gbc, "Inicio", "inicio", true, () -> dashboardPanel.actualizar()).setSelected(true);
        agregarOpcion(menu, gbc, "Personal", "personal", true, () -> personalPanel.cargarColaboradores());
        agregarOpcion(menu, gbc, "Periodos", "periodos", true, () -> periodosPanel.cargarPeriodos());
        agregarOpcion(menu, gbc, "Incidencias", "incidencias", true, () -> incidenciasPanel.actualizar());
        agregarOpcion(menu, gbc, "Pre-nómina", "prenomina", true, () -> prenominaPanel.actualizar());
        agregarOpcion(menu, gbc, "Reportes", "reportes", administrador || "REVISOR".equals(usuario.getRol()),
                () -> reportesPanel.actualizar());
        agregarOpcion(menu, gbc, "Usuarios", "usuarios", administrador, () -> usuariosPanel.actualizar());
        gbc.weighty = 1;
        menu.add(Box.createVerticalGlue(), gbc);
        return menu;
    }

    private JToggleButton agregarOpcion(JPanel menu, GridBagConstraints gbc, String texto, String tarjeta,
                                      boolean permitido, Runnable actualizar) {
        JToggleButton boton = new JToggleButton(texto, Estilo.icono(tarjeta, Estilo.MENTA));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setIconTextGap(11);
        boton.setMargin(new Insets(11, 12, 11, 12));
        boton.setBackground(Estilo.MENU);
        boton.setForeground(Estilo.TEXTO);
        boton.putClientProperty("FlatLaf.style", "selectedBackground:#286047; hoverBackground:#20503D; arc:12");
        boton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        boton.setEnabled(permitido);
        boton.addActionListener(e -> {
            cards.show(centerPanel, tarjeta);
            actualizar.run();
        });
        navegacion.add(boton);
        menu.add(boton, gbc);
        return boton;
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro que desea cerrar sesión?",
                "Confirmar cierre de sesión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Registrar en bitácora antes de cerrar
            try {
                bitacoraService.registrar(
                        usuario.getId(),
                        "CERRAR_SESION",
                        "usuario",
                        usuario.getId(),
                        "Usuario " + usuario.getUsuario() + " cerró sesión"
                );
            } catch (SQLException ex) {
                System.err.println("Error al registrar bitácora: " + ex.getMessage());
            }

            // Cerrar ventana actual y volver al login
            LoginFrame loginFrame = new LoginFrame(authService);
            dispose();
            loginFrame.setVisible(true);
        }
    }
}
