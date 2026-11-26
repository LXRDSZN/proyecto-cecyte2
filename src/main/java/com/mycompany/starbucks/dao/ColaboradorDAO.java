package com.mycompany.starbucks.dao;

import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ColaboradorDAO {

    private static final String INSERT = """
            INSERT INTO colaboradores (numero_empleado, nombre_completo, salario_diario, estado)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, numero_empleado, nombre_completo, salario_diario, estado
            FROM colaboradores WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, numero_empleado, nombre_completo, salario_diario, estado
            FROM colaboradores ORDER BY nombre_completo
            """;

    private static final String UPDATE = """
            UPDATE colaboradores SET numero_empleado = ?, nombre_completo = ?, salario_diario = ?, estado = ?
            WHERE id = ?
            """;

    private static final String DELETE_LOGICAL = """
            UPDATE colaboradores SET estado = 'INACTIVO' WHERE id = ?
            """;

    private static final String SEARCH_BY_NUMERO = """
            SELECT id, numero_empleado, nombre_completo, salario_diario, estado
            FROM colaboradores WHERE numero_empleado = ?
            """;

    private static final String SEARCH_BY_NOMBRE = """
            SELECT id, numero_empleado, nombre_completo, salario_diario, estado
            FROM colaboradores WHERE nombre_completo LIKE ? AND estado = 'ACTIVO'
            """;

    public Colaborador guardar(Connection connection, Colaborador colaborador) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, colaborador.getNumeroEmpleado());
            statement.setString(2, colaborador.getNombreCompleto());
            statement.setDouble(3, colaborador.getSalarioDiario());
            statement.setString(4, colaborador.getEstado());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    colaborador.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("No se pudo obtener el ID generado.");
                }
            }
        }
        return colaborador;
    }

    public Optional<Colaborador> porId(Integer id) throws SQLException {
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

    public List<Colaborador> listarTodos() throws SQLException {
        List<Colaborador> colaboradores = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                colaboradores.add(map(resultSet));
            }
        }
        return colaboradores;
    }

    public Colaborador actualizar(Connection connection, Colaborador colaborador) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setString(1, colaborador.getNumeroEmpleado());
            statement.setString(2, colaborador.getNombreCompleto());
            statement.setDouble(3, colaborador.getSalarioDiario());
            statement.setString(4, colaborador.getEstado());
            statement.setInt(5, colaborador.getId());

            statement.executeUpdate();
        }
        return colaborador;
    }

    public void desactivar(Integer id) throws SQLException {
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_LOGICAL)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<Colaborador> buscarPorNumero(String numero) throws SQLException {
        List<Colaborador> resultados = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH_BY_NUMERO)) {

            statement.setString(1, numero);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    resultados.add(map(resultSet));
                }
            }
        }
        return resultados;
    }

    public List<Colaborador> buscarPorNombre(String nombre) throws SQLException {
        List<Colaborador> resultados = new ArrayList<>();
        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH_BY_NOMBRE)) {

            statement.setString(1, "%" + nombre + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    resultados.add(map(resultSet));
                }
            }
        }
        return resultados;
    }

    private Colaborador map(ResultSet resultSet) throws SQLException {
        Colaborador c = new Colaborador();
        c.setId(resultSet.getInt("id"));
        c.setNumeroEmpleado(resultSet.getString("numero_empleado"));
        c.setNombreCompleto(resultSet.getString("nombre_completo"));
        c.setSalarioDiario(resultSet.getDouble("salario_diario"));
        c.setEstado(resultSet.getString("estado"));
        return c;
    }
}