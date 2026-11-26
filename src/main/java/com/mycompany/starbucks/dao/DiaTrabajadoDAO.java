package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.OptionalInt;

public class DiaTrabajadoDAO {
    public OptionalInt consultar(int periodo, int colaborador) throws SQLException {
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement("""
                     SELECT dias_trabajados FROM dias_trabajados
                     WHERE periodo_id = ? AND colaborador_id = ?
                     """)) {
            stmt.setInt(1, periodo);
            stmt.setInt(2, colaborador);
            try (var filas = stmt.executeQuery()) {
                return filas.next() ? OptionalInt.of(filas.getInt(1)) : OptionalInt.empty();
            }
        }
    }

    public int guardar(Connection conn, int periodo, int colaborador, int dias) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                     INSERT INTO dias_trabajados (periodo_id, colaborador_id, dias_trabajados)
                     SELECT p.id, c.id, ? FROM periodos_quincenales p
                     JOIN colaboradores c ON c.id = ?
                     WHERE p.id = ? AND p.estado = 'CAPTURA'
                     ON DUPLICATE KEY UPDATE dias_trabajados = VALUES(dias_trabajados)
                     """)) {
            stmt.setInt(1, dias);
            stmt.setInt(2, colaborador);
            stmt.setInt(3, periodo);
            stmt.executeUpdate();
        }
        try (var stmt = conn.prepareStatement("""
                SELECT id FROM dias_trabajados WHERE periodo_id = ? AND colaborador_id = ?
                """)) {
            stmt.setInt(1, periodo);
            stmt.setInt(2, colaborador);
            try (var fila = stmt.executeQuery()) {
                if (!fila.next()) throw new IllegalArgumentException("El colaborador ya no existe.");
                return fila.getInt(1);
            }
        }
    }
}
