package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.Incidencia;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.TipoIncidencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidenciaDAO {

    private static final String INSERT = """
            INSERT INTO incidencias (colaborador_id, periodo_id, tipo_id, fecha, observaciones, estado,
                                      fecha_fin, minutos_retardo, clasificacion, autorizacion_documentada,
                                      tratamiento, referencia_documental)
            SELECT ?, p.id, (SELECT id FROM tipos_incidencia WHERE UPPER(nombre) = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?
            FROM periodos_quincenales p WHERE p.id = ? AND p.estado = 'CAPTURA' 
            """;

    private static final String SELECT_BY_ID = """
            SELECT i.id, i.colaborador_id, i.periodo_id, i.tipo_id, t.nombre as tipo_nombre, i.fecha, i.observaciones, i.estado,
                   i.fecha_fin, i.minutos_retardo, i.clasificacion, i.autorizacion_documentada,
                   i.tratamiento, i.referencia_documental,
                   c.numero_empleado, c.nombre_completo, c.salario_diario, c.estado as col_estado, p.identificador AS periodo_identificador
            FROM incidencias i
            JOIN colaboradores c ON i.colaborador_id = c.id
            LEFT JOIN periodos_quincenales p ON p.id = i.periodo_id
            LEFT JOIN tipos_incidencia t ON i.tipo_id = t.id
            WHERE i.id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT i.id, i.colaborador_id, i.periodo_id, i.tipo_id, t.nombre as tipo_nombre, i.fecha, i.observaciones, i.estado,
                   i.fecha_fin, i.minutos_retardo, i.clasificacion, i.autorizacion_documentada,
                   i.tratamiento, i.referencia_documental,
                   c.numero_empleado, c.nombre_completo, c.salario_diario, c.estado as col_estado, p.identificador AS periodo_identificador
            FROM incidencias i
            JOIN colaboradores c ON i.colaborador_id = c.id
            LEFT JOIN periodos_quincenales p ON p.id = i.periodo_id
            LEFT JOIN tipos_incidencia t ON i.tipo_id = t.id
            ORDER BY i.fecha DESC
            """;

    private static final String SELECT_BY_COLABORADOR = """
            SELECT i.id, i.colaborador_id, i.periodo_id, i.tipo_id, t.nombre as tipo_nombre, i.fecha, i.observaciones, i.estado,
                   i.fecha_fin, i.minutos_retardo, i.clasificacion, i.autorizacion_documentada,
                   i.tratamiento, i.referencia_documental,
                   c.numero_empleado, c.nombre_completo, c.salario_diario, c.estado as col_estado, p.identificador AS periodo_identificador
            FROM incidencias i
            JOIN colaboradores c ON i.colaborador_id = c.id
            LEFT JOIN periodos_quincenales p ON p.id = i.periodo_id
            LEFT JOIN tipos_incidencia t ON i.tipo_id = t.id
            WHERE i.colaborador_id = ?
            ORDER BY i.fecha DESC
            """;

    private static final String SELECT_BY_PERIODO_AND_TIPO = """
            SELECT i.id, i.colaborador_id, i.periodo_id, i.tipo_id, t.nombre as tipo_nombre, i.fecha, i.observaciones, i.estado,
                   i.fecha_fin, i.minutos_retardo, i.clasificacion, i.autorizacion_documentada,
                   i.tratamiento, i.referencia_documental,
                   c.numero_empleado, c.nombre_completo, c.salario_diario, c.estado as col_estado, p.identificador AS periodo_identificador
            FROM incidencias i
            JOIN colaboradores c ON i.colaborador_id = c.id
            LEFT JOIN periodos_quincenales p ON p.id = i.periodo_id
            LEFT JOIN tipos_incidencia t ON i.tipo_id = t.id
            WHERE i.periodo_id = ? AND t.nombre = ?
            ORDER BY i.fecha
            """;

    private static final String UPDATE = """
            UPDATE incidencias SET colaborador_id = ?, periodo_id = ?, tipo_id = (SELECT id FROM tipos_incidencia WHERE UPPER(nombre) = ?), fecha = ?, observaciones = ?, estado = ?,
                fecha_fin = ?, minutos_retardo = ?, clasificacion = ?, autorizacion_documentada = ?,
                tratamiento = ?, referencia_documental = ?
            WHERE id = ? AND incidencias.estado = 'PENDIENTE'
              AND periodo_id IN (SELECT id FROM periodos_quincenales WHERE estado = 'CAPTURA')
            """;

    private static final String DELETE = """
            UPDATE incidencias SET estado = 'CANCELADA' WHERE id = ?
            """;

    public Incidencia guardar(Incidencia incidencia) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, incidencia.getColaborador().getId());
            statement.setString(2, incidencia.getTipo().name());

            if (incidencia.getFecha() != null) {
                statement.setDate(3, java.sql.Date.valueOf(incidencia.getFecha()));
            } else {
                statement.setNull(3, java.sql.Types.DATE);
            }
            statement.setString(4, incidencia.getObservaciones());
            statement.setString(5, incidencia.getEstado());

            asignarDatosEspecificos(statement, 6, incidencia);
            statement.setInt(12, incidencia.getPeriodo().getId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("El periodo dejó de estar en captura.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    incidencia.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID generado.");
                }
            }
        }
        return incidencia;
    }

    public Optional<Incidencia> porId(Integer id) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public List<Incidencia> listarTodos() throws SQLException {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                incidencias.add(map(resultSet));
            }
        }
        return incidencias;
    }

    public List<Incidencia> porColaborador(Colaborador colaborador) throws SQLException {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_COLABORADOR)) {

            statement.setInt(1, colaborador.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    incidencias.add(map(resultSet));
                }
            }
        }
        return incidencias;
    }

    public List<Incidencia> porPeriodoYTipo(PeriodoQuincenal periodo, TipoIncidencia tipo) throws SQLException {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_PERIODO_AND_TIPO)) {

            statement.setInt(1, periodo.getId());
            statement.setString(2, tipo.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    incidencias.add(map(resultSet));
                }
            }
        }
        return incidencias;
    }

    public Incidencia actualizar(Incidencia incidencia) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setInt(1, incidencia.getColaborador().getId());
            statement.setInt(2, incidencia.getPeriodo().getId());
            statement.setString(3, incidencia.getTipo().name());

            if (incidencia.getFecha() != null) {
                statement.setDate(4, java.sql.Date.valueOf(incidencia.getFecha()));
            } else {
                statement.setNull(4, java.sql.Types.DATE);
            }
            statement.setString(5, incidencia.getObservaciones());
            statement.setString(6, incidencia.getEstado());
            asignarDatosEspecificos(statement, 7, incidencia);
            statement.setInt(13, incidencia.getId());

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("La incidencia ya no está disponible para edición.");
            }
        }
        return incidencia;
    }

    public void cancelar(Integer id) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void asignarDatosEspecificos(PreparedStatement stmt, int indice, Incidencia incidencia) throws SQLException {
        stmt.setObject(indice, incidencia.getFechaFin(), java.sql.Types.DATE);
        stmt.setObject(indice + 1, incidencia.getMinutosRetardo(), java.sql.Types.INTEGER);
        stmt.setString(indice + 2, incidencia.getClasificacion());
        stmt.setObject(indice + 3, incidencia.getAutorizacionDocumentada(), java.sql.Types.BOOLEAN);
        stmt.setString(indice + 4, incidencia.getTratamiento());
        stmt.setString(indice + 5, incidencia.getReferenciaDocumental());
    }

    private Incidencia map(ResultSet resultSet) throws SQLException {
        Incidencia inc = new Incidencia();
        inc.setId(resultSet.getInt("id"));

        Colaborador c = new Colaborador();
        c.setId(resultSet.getInt("colaborador_id"));
        c.setNumeroEmpleado(resultSet.getString("numero_empleado"));
        c.setNombreCompleto(resultSet.getString("nombre_completo"));
        c.setSalarioDiario(resultSet.getDouble("salario_diario"));
        c.setEstado(resultSet.getString("col_estado"));
        inc.setColaborador(c);

        PeriodoQuincenal p = new PeriodoQuincenal();
        p.setId(resultSet.getInt("periodo_id"));
        p.setIdentificador(resultSet.getString("periodo_identificador"));
        inc.setPeriodo(p);

        String tipoNombre = resultSet.getString("tipo_nombre");
        if (tipoNombre != null) {
            inc.setTipo(TipoIncidencia.valueOf(tipoNombre.toUpperCase(java.util.Locale.ROOT)));
        }

        if (resultSet.getDate("fecha") != null) {
            inc.setFecha(resultSet.getDate("fecha").toLocalDate());
        }
        inc.setFechaFin(resultSet.getObject("fecha_fin", java.time.LocalDate.class));
        inc.setMinutosRetardo(resultSet.getObject("minutos_retardo", Integer.class));
        boolean autorizada = resultSet.getBoolean("autorizacion_documentada");
        inc.setAutorizacionDocumentada(resultSet.wasNull() ? null : autorizada);
        inc.setClasificacion(resultSet.getString("clasificacion"));
        inc.setTratamiento(resultSet.getString("tratamiento"));
        inc.setReferenciaDocumental(resultSet.getString("referencia_documental"));
        inc.setObservaciones(resultSet.getString("observaciones"));
        inc.setEstado(resultSet.getString("estado"));
        return inc;
    }
}
