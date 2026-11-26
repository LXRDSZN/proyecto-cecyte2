package com.mycompany.starbucks;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.service.AuthService;
import com.mycompany.starbucks.view.LoginFrame;
import com.mycompany.starbucks.view.Estilo;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicación STARBUCKS — Pre-nómina.
 * Inicializa la conexión con la base de datos y muestra el frame de login.
 */
public class Starbucks {

    public static void main(String[] args) {
        if (!reservarInstancia()) return;
        Estilo.instalar();

        // Verificar conexión a la base de datos antes de iniciar
        try {
            try (var conn = DatabaseConnection.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    System.out.println("Conexión a MariaDB exitosa.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error crítico: No se pudo conectar con MariaDB.");
            System.err.println("Causa: " + e.getMessage());
            e.printStackTrace();
            // Intentar continuar de todos modos para muestra, fallará el login
        }

        // Iniciar aplicación en el hilo de eventos Swing
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                AuthService authService = new AuthService();
                LoginFrame loginFrame = new LoginFrame(authService);
                loginFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error al iniciar la aplicación: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    private static boolean reservarInstancia() {
        Path archivo = Path.of(System.getProperty("user.home"), ".starbucks-prenomina.lock");
        try {
            FileChannel canal = FileChannel.open(archivo, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock bloqueo;
            try {
                bloqueo = canal.tryLock();
            } catch (IOException | OverlappingFileLockException ex) {
                canal.close();
                throw ex;
            }
            if (bloqueo == null) {
                canal.close();
                System.out.println("STARBUCKS — Pre-nómina ya está abierto.");
                return false;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try (canal; bloqueo) {
                    // El cierre libera el bloqueo para el próximo arranque.
                } catch (IOException ex) {
                    System.err.println("Error al liberar el bloqueo: " + ex.getMessage());
                }
            }, "liberar-instancia"));
            return true;
        } catch (OverlappingFileLockException ex) {
            System.out.println("STARBUCKS — Pre-nómina ya está abierto.");
            return false;
        } catch (IOException ex) {
            System.err.println("No se pudo reservar la instancia: " + ex.getMessage());
            return false;
        }
    }
}
