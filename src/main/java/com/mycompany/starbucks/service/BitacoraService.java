package com.mycompany.starbucks.service;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.RegistroBitacora;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio para la gestión de la bitácora de cambios del sistema.
 * <p>
 * Registra todas las operaciones importantes del sistema:
 * altas, modificaciones, cambios de estado, revisiones, autorizaciones, etc.
 * No registra contraseñas bajo ningún concepto.
 */
public class BitacoraService {

    /**
     * Registra una operación en la bitácora de la base de datos.
     *
     * @param usuarioId ID del usuario que realizó la operación
     * @param accion Nombre de la acción (ALTA, MODIFICACION, ELIMINACION, etc.)
     * @param entidad Nombre de la entidad afectada (usuario, colaborador, periodo, incidencia, etc.)
     * @param identificador ID del registro afectado
     * @param informacionRelevante Descripción relevante de la operación
     * @throws SQLException Si hay error al conectar con la base de datos
     */
    public void registrar(int usuarioId, String accion, String entidad, int identificador,
                          String informacionRelevante) throws SQLException {
        String sql = """
                INSERT INTO bitacora_cambios (usuario_id, accion, entidad, identificador, fecha_hora, informacion_relevante)
                VALUES (?, ?, ?, ?, NOW(), ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, usuarioId);
            statement.setString(2, accion);
            statement.setString(3, entidad);
            statement.setInt(4, identificador);
            statement.setString(5, informacionRelevante);

            statement.executeUpdate();
        }
    }

    /**
     * Obtiene el registro de bitácora por ID.
     *
     * @param id ID del registro de bitácora
     * @return Optional con el registro de bitácora si existe
     * @throws SQLException Si hay error de conexión
     */
    public Optional<RegistroBitacora> porId(int id) throws SQLException {
        String sql = """
                SELECT id, usuario_id, accion, entidad, identificador, fecha_hora, informacion_relevante
                FROM bitacora_cambios WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    RegistroBitacora registro = new RegistroBitacora();
                    registro.setId(resultSet.getInt("id"));
                    // No cargamos el usuario completo aquí para evitar cargas innecesarias
                    // Se podría completar si es necesario
                    return Optional.of(registro);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Obtiene los últimos registros de bitácora.
     *
     * @param limite Número máximo de registros a retornar
     * @return Lista de registros de bitácora
     * @throws SQLException Si hay error de conexión
     */
    public java.util.List<RegistroBitacora> ultimos(int limite) throws SQLException {
        String sql = """
                SELECT id, usuario_id, accion, entidad, identificador, fecha_hora, informacion_relevante
                FROM bitacora_cambios
                ORDER BY fecha_hora DESC
                LIMIT ?
                """;

        java.util.List<RegistroBitacora> lista = new java.util.ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limite);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    RegistroBitacora registro = new RegistroBitacora();
                    registro.setId(resultSet.getInt("id"));
                    lista.add(registro);
                }
            }
        }
        return lista;
    }

    /**
     * Obtiene los registros de bitácora filtrados por entidad y/o acción.
     *
     * @param entidad Nombre de la entidad (opcional)
     * @param accion Nombre de la acción (opcional)
     * @return Lista de registros de bitácora
     * @throws SQLException Si hay error de conexión
     */
    public java.util.List<RegistroBitacora> porEntidadYAccion(String entidad, String accion)
            throws SQLException {
        // Construir query dinámicamente basándose en los parámetros proporcionados
        StringBuilder sqlBuilder = new StringBuilder("""
                SELECT id, usuario_id, accion, entidad, identificador, fecha_hora, informacion_relevante
                FROM bitacora_cambios WHERE 1=1
                """);
        java.util.List<Object> parametros = new java.util.ArrayList<>();

        if (entidad != null && !entidad.isEmpty()) {
            sqlBuilder.append(" AND entidad = ? ");
            parametros.add(entidad);
        }

        if (accion != null && !accion.isEmpty()) {
            sqlBuilder.append(" AND accion = ? ");
            parametros.add(accion);
        }

        sqlBuilder.append(" ORDER BY fecha_hora DESC");

        String sql = sqlBuilder.toString();
        java.util.List<RegistroBitacora> lista = new java.util.ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(sql)) {

            // Establecer parámetros
            int idx = 1;
            if (entidad != null && !entidad.isEmpty()) {
                statement.setString(idx++, entidad);
            }
            if (accion != null && !accion.isEmpty()) {
                statement.setString(idx++, accion);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    RegistroBitacora registro = new RegistroBitacora();
                    registro.setId(resultSet.getInt("id"));
                    lista.add(registro);
                }
            }
        }
        return lista;
    }
}