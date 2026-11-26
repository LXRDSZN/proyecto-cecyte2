package com.mycompany.starbucks.view;

import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.service.AuthService;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final JTextField usuarioField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel feedback = new JLabel(" ");
    private final JButton entrar = new JButton("Iniciar sesión");
    private boolean iniciando;

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        setTitle("STARBUCKS — Pre-nómina");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(450, 450));
        setLayout(new GridBagLayout());

        JPanel tarjeta = new JPanel(new GridBagLayout());
        tarjeta.setBackground(Estilo.SUPERFICIE);
        tarjeta.setBorder(Estilo.tarjeta(28));
        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel titulo = new JLabel("STARBUCKS — Pre-nómina");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 21f));
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 12, 0);
        tarjeta.add(Estilo.marca(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        tarjeta.add(titulo, gbc);
        gbc.insets = new Insets(8, 0, 4, 0);
        tarjeta.add(new JLabel("Usuario"), gbc);
        tarjeta.add(usuarioField, gbc);
        tarjeta.add(new JLabel("Contraseña"), gbc);
        tarjeta.add(passwordField, gbc);
        gbc.insets = new Insets(15, 0, 6, 0);
        entrar.setBackground(Estilo.VERDE);
        entrar.setForeground(Color.WHITE);
        tarjeta.add(entrar, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        feedback.setFont(feedback.getFont().deriveFont(12f));
        tarjeta.add(feedback, gbc);
        add(tarjeta, new GridBagConstraints());

        entrar.addActionListener(e -> iniciarSesion());
        getRootPane().setDefaultButton(entrar);
        pack();
        setLocationRelativeTo(null);
    }

    private void iniciarSesion() {
        if (iniciando || !isDisplayable()) return;
        String usuario = usuarioField.getText().trim();
        char[] clave = passwordField.getPassword();
        String password = new String(clave);
        Arrays.fill(clave, '\0');
        if (usuario.isEmpty() || password.isEmpty()) {
            informarError("Completa usuario y contraseña.");
            return;
        }
        iniciando = true;
        entrar.setEnabled(false);
        feedback.setForeground(Estilo.MENTA);
        feedback.setText("Conectando…");
        new SwingWorker<Optional<Usuario>, Void>() {
            @Override protected Optional<Usuario> doInBackground() throws Exception {
                return authService.autenticar(usuario, password);
            }

            @Override protected void done() {
                if (!isDisplayable()) return;
                try {
                    var resultado = get();
                    if (resultado.isEmpty()) {
                        informarError("Credenciales incorrectas.");
                        passwordField.setText("");
                        return;
                    }
                    MainFrame ventana = new MainFrame(resultado.get());
                    dispose();
                    ventana.setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    informarError("Se interrumpió el inicio de sesión.");
                } catch (ExecutionException | RuntimeException ex) {
                    System.err.println("Error al iniciar sesión: " + ex);
                    informarError("No se pudo abrir la sesión.");
                } finally {
                    iniciando = false;
                    entrar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void informarError(String mensaje) {
        feedback.setForeground(Color.decode("#FCA5A5"));
        feedback.setText(mensaje);
    }
}
