package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeriodoQuincenalDAO {

    private static final String INSERT = """
            INSERT INTO periodos_quincenales (identificador, fecha_inicio, fecha_fin, estado)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, identificador, fecha_inicio, fecha_fin, estado
            FROM periodos_quincenales WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, identificador, fecha_inicio, fecha_fin, estado
            FROM periodos_quincenales ORDER BY fecha_inicio DESC
            """;

    private static final String UPDATE = """
            UPDATE periodos_quincenales SET identificador = ? WHERE id = ? AND estado = 'CAPTURA'
            """;

    private static final String SEARCH_DUPLICATE_IDENTIFICADOR = """
            SELECT id FROM periodos_quincenales WHERE identificador = ?
            """;

    public PeriodoQuincenal guardar(PeriodoQuincenal periodo) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, periodo.getIdentificador());
            statement.setDate(2, java.sql.Date.valueOf(periodo.getFechaInicio()));
            statement.setDate(3, java.sql.Date.valueOf(periodo.getFechaFin()));
            statement.setString(4, periodo.getEstado());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    periodo.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID generado.");
                }
            }
        }
        return periodo;
    }

    public Optional<PeriodoQuincenal> porId(Integer id) throws SQLException {
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

    public List<PeriodoQuincenal> listarTodos() throws SQLException {
        List<PeriodoQuincenal> periodos = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                periodos.add(map(resultSet));
            }
        }
        return periodos;
    }

    public void renombrar(int id, String identificador) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, identificador);
            statement.setInt(2, id);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Solo se pueden editar periodos en captura.");
            }
        }
    }

    public boolean identificadorExiste(String identificador) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH_DUPLICATE_IDENTIFICADOR)) {

            statement.setString(1, identificador);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private PeriodoQuincenal map(ResultSet resultSet) throws SQLException {
        PeriodoQuincenal p = new PeriodoQuincenal();
        p.setId(resultSet.getInt("id"));
        p.setIdentificador(resultSet.getString("identificador"));
        p.setFechaInicio(resultSet.getDate("fecha_inicio").toLocalDate());
        p.setFechaFin(resultSet.getDate("fecha_fin").toLocalDate());
        p.setEstado(resultSet.getString("estado"));
        return p;
    }
}