package com.mycompany.starbucks.service;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.dao.UsuarioDAO;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.model.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

final class OperacionBD {
    private OperacionBD() {}

    @FunctionalInterface
    interface Tarea<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    static <T> T transaccion(Tarea<T> tarea) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T resultado = tarea.ejecutar(conn);
                conn.commit();
                return resultado;
            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                throw ex;
            }
        }
    }

    static void exigirRol(Usuario actor, Rol... roles) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            exigirRol(conn, actor, roles);
        }
    }

    static void exigirRol(Connection conn, Usuario actor, Rol... roles) throws SQLException {
        var cuenta = actor == null || actor.getId() == null ? null : new UsuarioDAO().buscar(conn, actor.getId(), false);
        if (cuenta == null || !"ACTIVO".equals(cuenta.estado())
                || Arrays.stream(roles).noneMatch(rol -> rol == cuenta.rol())) {
            throw new IllegalArgumentException("Tu cuenta no tiene permisos para esta operación.");
        }
    }
}
