package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.model.Rol;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    public record Cuenta(int id, String nombre, Rol rol, String estado) {}
    public record Evento(LocalDateTime fecha, String usuario, String accion, String entidad, int id, String detalle) {}

    private static final String CUENTAS = """
            SELECT u.id, u.usuario, r.nombre, u.estado
            FROM usuarios u JOIN roles r ON r.id = u.rol_id
            """;

    public List<Cuenta> listar(Connection conn) throws SQLException {
        try (var stmt = conn.prepareStatement(CUENTAS + " ORDER BY u.usuario");
             var filas = stmt.executeQuery()) {
            List<Cuenta> cuentas = new ArrayList<>();
            while (filas.next()) {
                cuentas.add(new Cuenta(filas.getInt(1), filas.getString(2),
                        Rol.valueOf(filas.getString(3)), filas.getString(4)));
            }
            return cuentas;
        }
    }

    public Cuenta buscar(Connection conn, int id, boolean bloquear) throws SQLException {
        try (var stmt = conn.prepareStatement(CUENTAS + " WHERE u.id = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            stmt.setInt(1, id);
            try (var fila = stmt.executeQuery()) {
                return fila.next() ? new Cuenta(fila.getInt(1), fila.getString(2),
                        Rol.valueOf(fila.getString(3)), fila.getString(4)) : null;
            }
        }
    }

    public void bloquearAdministracion(Connection conn) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR' FOR UPDATE");
             var fila = stmt.executeQuery()) {
            if (!fila.next()) throw new SQLException("Falta el rol ADMINISTRADOR.");
        }
    }

    public int contarAdministradores(Connection conn) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                SELECT COUNT(*) FROM usuarios u JOIN roles r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR' AND u.estado = 'ACTIVO'
                """);
             var fila = stmt.executeQuery()) {
            fila.next();
            return fila.getInt(1);
        }
    }

    public int crear(Connection conn, String usuario, String clave, Rol rol, String estado) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                INSERT INTO usuarios (usuario, password, rol_id, estado)
                VALUES (?, ?, (SELECT id FROM roles WHERE nombre = ?), ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario);
            stmt.setString(2, clave);
            stmt.setString(3, rol.name());
            stmt.setString(4, estado);
            stmt.executeUpdate();
            try (var fila = stmt.getGeneratedKeys()) {
                if (!fila.next()) throw new SQLException("No se pudo recuperar el ID del usuario.");
                return fila.getInt(1);
            }
        }
    }

    public void actualizar(Connection conn, int id, String usuario, Rol rol, String estado) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                UPDATE usuarios SET usuario = ?, rol_id = (SELECT id FROM roles WHERE nombre = ?), estado = ?
                WHERE id = ?
                """)) {
            stmt.setString(1, usuario);
            stmt.setString(2, rol.name());
            stmt.setString(3, estado);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }

    public void actualizarClave(Connection conn, int id, String clave) throws SQLException {
        try (var stmt = conn.prepareStatement("UPDATE usuarios SET password = ? WHERE id = ?")) {
            stmt.setString(1, clave);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void auditar(Connection conn, int actor, int id, String accion, String nota) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                INSERT INTO bitacora_cambios (usuario_id, accion, entidad, identificador, informacion_relevante)
                VALUES (?, ?, 'usuario', ?, ?)
                """)) {
            stmt.setInt(1, actor);
            stmt.setString(2, accion);
            stmt.setInt(3, id);
            stmt.setString(4, nota);
            stmt.executeUpdate();
        }
    }

    public List<Evento> bitacora(Connection conn) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                SELECT b.fecha_hora, u.usuario, b.accion, b.entidad, b.identificador, b.informacion_relevante
                FROM bitacora_cambios b LEFT JOIN usuarios u ON u.id = b.usuario_id
                ORDER BY b.fecha_hora DESC, b.id DESC
                """);
             var filas = stmt.executeQuery()) {
            List<Evento> eventos = new ArrayList<>();
            while (filas.next()) eventos.add(new Evento(filas.getObject(1, LocalDateTime.class), filas.getString(2),
                    filas.getString(3), filas.getString(4), filas.getInt(5), filas.getString(6)));
            return eventos;
        }
    }
}
