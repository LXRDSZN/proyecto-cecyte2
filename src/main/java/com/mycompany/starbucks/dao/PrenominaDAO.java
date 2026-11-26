package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.model.TipoIncidencia;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PrenominaDAO {
    public record Quincena(String estado, LocalDate inicio, LocalDate fin) {}
    public record Personal(int id, String numero, String nombre, BigDecimal salario, Integer diasTrabajados) {}
    public record Incidencia(int colaborador, TipoIncidencia tipo, LocalDate inicio, LocalDate fin, String estado) {}
    public record Resultado(int colaborador, String numero, String nombre, BigDecimal salario,
                            int diasDescuento, BigDecimal importe, String estado) {}
    public record Regla(boolean descuenta, String referencia) {}

    public Quincena bloquearPeriodo(Connection conn, int periodo) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                SELECT estado, fecha_inicio, fecha_fin FROM periodos_quincenales WHERE id = ? FOR UPDATE
                """)) {
            stmt.setInt(1, periodo);
            try (var fila = stmt.executeQuery()) {
                if (!fila.next()) throw new IllegalArgumentException("La quincena no existe.");
                return new Quincena(fila.getString(1), fila.getObject(2, LocalDate.class),
                        fila.getObject(3, LocalDate.class));
            }
        }
    }

    public Map<TipoIncidencia, Regla> reglas(Connection conn, int periodo) throws SQLException {
        Map<TipoIncidencia, Regla> reglas = new EnumMap<>(TipoIncidencia.class);
        try (var stmt = conn.prepareStatement("""
                SELECT UPPER(t.nombre), r.aplica_descuento, r.descripcion
                FROM reglas_calculo r JOIN tipos_incidencia t ON t.id = r.tipo_id
                WHERE r.periodo_id = ? AND r.activo = 1 AND r.aplica_descuento IS NOT NULL
                    AND r.autorizado_por IS NOT NULL AND r.autorizado_en IS NOT NULL
                """)) {
            stmt.setInt(1, periodo);
            try (var filas = stmt.executeQuery()) {
                while (filas.next()) reglas.put(TipoIncidencia.valueOf(filas.getString(1)),
                        new Regla(filas.getBoolean(2), filas.getString(3)));
            }
        }
        return reglas;
    }

    public void guardarRegla(Connection conn, int periodo, TipoIncidencia tipo, Regla regla, int actor)
            throws SQLException {
        try (var stmt = conn.prepareStatement("""
                INSERT INTO reglas_calculo (periodo_id, tipo_id, nombre, descripcion, aplica_descuento,
                                            autorizado_por, autorizado_en, activo)
                SELECT ?, id, ?, ?, ?, ?, CURRENT_TIMESTAMP, 1 FROM tipos_incidencia WHERE UPPER(nombre) = ?
                ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion), aplica_descuento = VALUES(aplica_descuento),
                    autorizado_por = VALUES(autorizado_por), autorizado_en = VALUES(autorizado_en), activo = 1
                """)) {
            stmt.setInt(1, periodo);
            stmt.setString(2, tipo.name());
            stmt.setString(3, regla.referencia());
            stmt.setBoolean(4, regla.descuenta());
            stmt.setInt(5, actor);
            stmt.setString(6, tipo.name());
            if (stmt.executeUpdate() == 0) throw new IllegalArgumentException("El tipo no está en el catálogo.");
        }
        try (var stmt = conn.prepareStatement("""
                UPDATE detalles_prenomina SET estado = 'DESACTUALIZADO' WHERE periodo_id = ?
                """)) {
            stmt.setInt(1, periodo);
            stmt.executeUpdate();
        }
    }

    public List<Personal> personal(Connection conn, int periodo) throws SQLException {
        List<Personal> lista = new ArrayList<>();
        try (var stmt = conn.prepareStatement("""
                SELECT c.id, c.numero_empleado, c.nombre_completo, c.salario_diario, d.dias_trabajados
                FROM colaboradores c LEFT JOIN dias_trabajados d ON d.colaborador_id = c.id AND d.periodo_id = ?
                WHERE c.estado = 'ACTIVO' OR d.id IS NOT NULL OR EXISTS (
                    SELECT 1 FROM incidencias i WHERE i.colaborador_id = c.id AND i.periodo_id = ?
                        AND (i.estado IS NULL OR i.estado <> 'CANCELADA'))
                ORDER BY c.numero_empleado
                """)) {
            stmt.setInt(1, periodo);
            stmt.setInt(2, periodo);
            try (var filas = stmt.executeQuery()) {
                while (filas.next()) lista.add(new Personal(filas.getInt(1), filas.getString(2),
                        filas.getString(3), filas.getBigDecimal(4), filas.getObject(5, Integer.class)));
            }
        }
        return lista;
    }

    public List<Incidencia> incidencias(Connection conn, int periodo) throws SQLException {
        List<Incidencia> lista = new ArrayList<>();
        try (var stmt = conn.prepareStatement("""
                SELECT i.colaborador_id, UPPER(t.nombre), i.fecha, COALESCE(i.fecha_fin, i.fecha), i.estado
                FROM incidencias i LEFT JOIN tipos_incidencia t ON t.id = i.tipo_id
                WHERE i.periodo_id = ? AND (i.estado IS NULL OR i.estado <> 'CANCELADA')
                """)) {
            stmt.setInt(1, periodo);
            try (var filas = stmt.executeQuery()) {
                while (filas.next()) {
                    String tipo = filas.getString(2);
                    lista.add(new Incidencia(filas.getInt(1), tipo == null ? null : TipoIncidencia.valueOf(tipo),
                            filas.getObject(3, LocalDate.class), filas.getObject(4, LocalDate.class), filas.getString(5)));
                }
            }
        }
        return lista;
    }

    public List<Resultado> resultados(Connection conn, int periodo) throws SQLException {
        List<Resultado> resultados = new ArrayList<>();
        try (var stmt = conn.prepareStatement("""
                SELECT c.id, c.numero_empleado, c.nombre_completo, d.salario_diario,
                       d.dias_descuento, d.descuento_preliminar, d.estado
                FROM detalles_prenomina d JOIN colaboradores c ON c.id = d.colaborador_id
                WHERE d.periodo_id = ? ORDER BY c.numero_empleado
                """)) {
            stmt.setInt(1, periodo);
            try (var filas = stmt.executeQuery()) {
                while (filas.next()) resultados.add(new Resultado(filas.getInt(1), filas.getString(2),
                        filas.getString(3), filas.getBigDecimal(4), filas.getInt(5), filas.getBigDecimal(6), filas.getString(7)));
            }
        }
        return resultados;
    }

    public void reemplazarResultados(Connection conn, int periodo, List<Resultado> resultados) throws SQLException {
        try (var borrar = conn.prepareStatement("DELETE FROM detalles_prenomina WHERE periodo_id = ?");
             var insertar = conn.prepareStatement("""
                     INSERT INTO detalles_prenomina
                         (periodo_id, colaborador_id, salario_diario, dias_descuento, descuento_preliminar, estado)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            borrar.setInt(1, periodo);
            borrar.executeUpdate();
            for (Resultado fila : resultados) {
                insertar.setInt(1, periodo);
                insertar.setInt(2, fila.colaborador());
                insertar.setBigDecimal(3, fila.salario());
                insertar.setInt(4, fila.diasDescuento());
                insertar.setBigDecimal(5, fila.importe());
                insertar.setString(6, fila.estado());
                insertar.addBatch();
            }
            insertar.executeBatch();
        }
    }

    public void cambiarEstado(Connection conn, int periodo, String estado) throws SQLException {
        try (var quincena = conn.prepareStatement("UPDATE periodos_quincenales SET estado = ? WHERE id = ?");
             var detalles = conn.prepareStatement("UPDATE detalles_prenomina SET estado = ? WHERE periodo_id = ?")) {
            quincena.setString(1, "REVISADO".equals(estado) ? "REVISION" : estado);
            quincena.setInt(2, periodo);
            quincena.executeUpdate();
            detalles.setString(1, "CAPTURA".equals(estado) ? "DESACTUALIZADO" : estado);
            detalles.setInt(2, periodo);
            detalles.executeUpdate();
        }
        if ("REVISION".equals(estado)) return;
        try (var incidencias = conn.prepareStatement("""
                UPDATE incidencias SET estado = ? WHERE periodo_id = ?
                AND (estado IS NULL OR estado <> 'CANCELADA')
                """)) {
            incidencias.setString(1, "CAPTURA".equals(estado) ? "PENDIENTE" : estado);
            incidencias.setInt(2, periodo);
            incidencias.executeUpdate();
        }
    }

    public void registrarRevision(Connection conn, int periodo, int actor, String estado, String nota)
            throws SQLException {
        try (var stmt = conn.prepareStatement("""
                INSERT INTO revisiones_periodo (periodo_id, usuario_id, observaciones, estado)
                VALUES (?, ?, ?, ?)
                """)) {
            stmt.setInt(1, periodo);
            stmt.setInt(2, actor);
            stmt.setString(3, nota);
            stmt.setString(4, estado);
            stmt.executeUpdate();
        }
    }

    public void auditar(Connection conn, int actor, int periodo, String accion, String informacion) throws SQLException {
        try (var stmt = conn.prepareStatement("""
                INSERT INTO bitacora_cambios (usuario_id, accion, entidad, identificador, informacion_relevante)
                VALUES (?, ?, 'periodo_quincenal', ?, ?)
                """)) {
            stmt.setInt(1, actor);
            stmt.setString(2, accion);
            stmt.setInt(3, periodo);
            stmt.setString(4, informacion);
            stmt.executeUpdate();
        }
    }
}
