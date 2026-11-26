package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.config.DatabaseConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {
    public enum Tipo {
        PERSONAL("Personal", "c.id", null, "c.estado", """
                SELECT c.numero_empleado AS 'Número de empleado', c.nombre_completo AS 'Colaborador',
                       c.salario_diario AS 'Salario diario', c.estado AS 'Estado'
                FROM colaboradores c
                """, "c.numero_empleado"),
        INCIDENCIAS("Incidencias", "c.id", "p.id", "i.estado", """
                SELECT p.identificador AS 'Quincena', c.numero_empleado AS 'Número de empleado',
                       c.nombre_completo AS 'Colaborador', t.nombre AS 'Tipo', i.fecha AS 'Inicio',
                       COALESCE(i.fecha_fin,i.fecha) AS 'Fin', i.estado AS 'Estado',
                       i.observaciones AS 'Observaciones', i.minutos_retardo AS 'Minutos de retardo',
                       i.clasificacion AS 'Tipo o clasificación',
                       CASE i.autorizacion_documentada WHEN 1 THEN 'Sí' WHEN 0 THEN 'No' END AS 'Autorización documentada',
                       i.tratamiento AS 'Tratamiento', i.referencia_documental AS 'Referencia documental'
                FROM incidencias i JOIN colaboradores c ON c.id = i.colaborador_id
                LEFT JOIN periodos_quincenales p ON p.id = i.periodo_id
                LEFT JOIN tipos_incidencia t ON t.id = i.tipo_id
                """, "i.fecha DESC, i.id DESC"),
        DESCUENTOS("Descuentos preliminares", "c.id", "p.id", "d.estado", """
                SELECT p.identificador AS 'Quincena', c.numero_empleado AS 'Número de empleado',
                       c.nombre_completo AS 'Colaborador', d.salario_diario AS 'Salario diario',
                       d.dias_descuento AS 'Días de descuento', d.descuento_preliminar AS 'Descuento preliminar',
                       d.estado AS 'Estado'
                FROM detalles_prenomina d JOIN colaboradores c ON c.id = d.colaborador_id
                JOIN periodos_quincenales p ON p.id = d.periodo_id
                """, "p.fecha_inicio DESC, c.numero_empleado"),
        REVISIONES("Revisiones y autorizaciones", null, "p.id", "r.estado", """
                SELECT p.identificador AS 'Quincena', p.estado AS 'Estado actual de la quincena',
                       r.estado AS 'Estado de revisión', u.usuario AS 'Responsable',
                       r.fecha_revision AS 'Fecha y hora', r.observaciones AS 'Observaciones'
                FROM revisiones_periodo r JOIN periodos_quincenales p ON p.id = r.periodo_id
                JOIN usuarios u ON u.id = r.usuario_id
                """, "r.fecha_revision DESC, r.id DESC");

        private final String etiqueta;
        private final String colaborador;
        private final String periodo;
        private final String estado;
        private final String consulta;
        private final String orden;

        Tipo(String etiqueta, String colaborador, String periodo, String estado, String consulta, String orden) {
            this.etiqueta = etiqueta;
            this.colaborador = colaborador;
            this.periodo = periodo;
            this.estado = estado;
            this.consulta = consulta;
            this.orden = orden;
        }

        @Override public String toString() { return etiqueta; }
    }

    public record Filtros(Tipo reporte, Integer periodo, Integer colaborador, String incidencia, String estado) {}
    public record Datos(List<String> columnas, List<List<Object>> filas) {}

    public Datos consultar(Filtros filtros) throws SQLException {
        var tipo = filtros.reporte();
        var sql = new StringBuilder(tipo.consulta).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        agregarFiltro(sql, parametros, tipo.periodo, filtros.periodo());
        agregarFiltro(sql, parametros, tipo.colaborador, filtros.colaborador());
        agregarFiltro(sql, parametros, tipo.estado, filtros.estado());
        if (tipo == Tipo.INCIDENCIAS) agregarFiltro(sql, parametros, "UPPER(t.nombre)", filtros.incidencia());
        sql.append(" ORDER BY ").append(tipo.orden);

        try (var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) stmt.setObject(i + 1, parametros.get(i));
            try (var rs = stmt.executeQuery()) {
                int columnas = rs.getMetaData().getColumnCount();
                List<String> encabezados = new ArrayList<>();
                for (int i = 1; i <= columnas; i++) encabezados.add(rs.getMetaData().getColumnLabel(i));
                List<List<Object>> filas = new ArrayList<>();
                while (rs.next()) {
                    List<Object> fila = new ArrayList<>();
                    for (int i = 1; i <= columnas; i++) fila.add(rs.getObject(i));
                    filas.add(fila);
                }
                return new Datos(encabezados, filas);
            }
        }
    }

    private void agregarFiltro(StringBuilder sql, List<Object> parametros, String columna, Object valor) {
        if (columna != null && valor != null) {
            sql.append(" AND ").append(columna).append(" = ?");
            parametros.add(valor);
        }
    }
}
