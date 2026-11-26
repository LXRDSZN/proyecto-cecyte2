package com.mycompany.starbucks.service;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de usuarios del sistema.
 * <p>
 * Operaciones sobre la tabla 'usuarios' de MariaDB.
 * Para este prototipo académico, las contraseñas se manejan en texto plano
 * y están aisladas en este servicio para permitir su reemplazo posterior con hashing.
 */
public class UsuarioService {

    /**
     * Verifica si un usuario existe en la base de datos.
     *
     * @param usuario Nombre de usuario
     * @return true si el usuario existe, false en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean existe(String usuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ? AND estado = 'ACTIVO'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Obtiene un usuario por su nombre de usuario.
     *
     * @param usuario Nombre de usuario
     * @return Optional con el usuario si existe, vacío en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public Optional<Usuario> porNombre(String usuario) throws SQLException {
        if (usuario == null || usuario.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT id, usuario, password, rol_id, estado FROM usuarios WHERE usuario = ? AND estado = 'ACTIVO'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Usuario user = new Usuario();
                    user.setId(resultSet.getInt("id"));
                    user.setUsuario(resultSet.getString("usuario"));
                    user.setPassword(resultSet.getString("password"));
                    // Convertir rol_id a nombre
                    int rolId = resultSet.getInt("rol_id");
                    user.setRol(rolId == 1 ? "ADMINISTRADOR" : rolId == 2 ? "CAPTURISTA" : "REVISOR");
                    user.setEstado(resultSet.getString("estado"));
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Crea un nuevo usuario en la base de datos.
     *
     * @param usuario Nombre de usuario
     * @param password Contraseña (texto plano - prototipo académico)
     * @param rol Rol del usuario
     * @param estado Estado del usuario
     * @return true si el usuario fue creado exitosamente, false si ya existe
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean crear(String usuario, String password, Rol rol, String estado) throws SQLException {
        // Verificar que no exista ya
        if (existe(usuario)) {
            return false;
        }

        String sql = """
                INSERT INTO usuarios (usuario, password, rol_id, estado)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario);
            statement.setString(2, password); // Texto plano - prototipo académico
            statement.setInt(3, ordinalToInt(rol));
            statement.setString(4, estado);

            int filas = statement.executeUpdate();
            return filas > 0;
        }
    }

    /**
     * Actualiza un usuario existente.
     *
     * @param id ID del usuario
     * @param usuario Nuevo nombre de usuario
     * @param password Nueva contraseña (texto plano)
     * @param rol Nuevo rol
     * @param nuevoEstado Nuevo estado
     * @return true si la actualización fue exitosa
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean actualizar(int id, String usuario, String password, Rol rol, String nuevoEstado)
            throws SQLException {
        String sql = """
                UPDATE usuarios SET usuario = ?, password = ?, rol_id = ?, estado = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario);
            statement.setString(2, password);
            statement.setInt(3, ordinalToInt(rol));
            statement.setString(4, nuevoEstado);
            statement.setInt(5, id);

            int filas = statement.executeUpdate();
            return filas > 0;
        }
    }

    /**
     * Obtiene todos los usuarios de la base de datos.
     *
     * @return Lista de usuarios
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id, usuario, password, rol_id, estado FROM usuarios WHERE estado != 'ELIMINADO'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Usuario user = new Usuario();
                user.setId(resultSet.getInt("id"));
                user.setUsuario(resultSet.getString("usuario"));
                user.setPassword(resultSet.getString("password"));
                user.setRol(rolIdToNombre(resultSet.getInt("rol_id")));
                user.setEstado(resultSet.getString("estado"));
                usuarios.add(user);
            }
        }
        return usuarios;
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Optional con el usuario si existe
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public Optional<Usuario> porId(int id) throws SQLException {
        String sql = "SELECT id, usuario, password, rol_id, estado FROM usuarios WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Usuario user = new Usuario();
                    user.setId(resultSet.getInt("id"));
                    user.setUsuario(resultSet.getString("usuario"));
                    user.setPassword(resultSet.getString("password"));
                    user.setRol(rolIdToNombre(resultSet.getInt("rol_id")));
                    user.setEstado(resultSet.getString("estado"));
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Cambia la contraseña de un usuario.
     *
     * @param idUsuario ID del usuario cuya contraseña se cambiará
     * @param nuevaPassword Nueva contraseña (texto plano - prototipo académico)
     * @return true si la contraseña fue cambiada exitosamente
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean cambiarPassword(int idUsuario, String nuevaPassword) throws SQLException {
        String sql = "UPDATE usuarios SET password = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nuevaPassword);
            statement.setInt(2, idUsuario);

            int filas = statement.executeUpdate();
            return filas > 0;
        }
    }

    /**
     * Cambia el estado de un usuario.
     *
     * @param idUsuario ID del usuario
     * @param nuevoEstado Nuevo estado ('ACTIVO' o 'INACTIVO')
     * @return true si el estado fue cambiado exitosamente
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean cambiarEstado(int idUsuario, String nuevoEstado) throws SQLException {
        String sql = "UPDATE usuarios SET estado = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nuevoEstado);
            statement.setInt(2, idUsuario);

            int filas = statement.executeUpdate();
            return filas > 0;
        }
    }

    /**
     * Cuenta la cantidad de adminsitradores activos.
     * Importante: No permitir dejar el sistema sin administradores activos.
     *
     * @return Número de administradores activos
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public int contarAdminsActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol_id = 1 AND estado = 'ACTIVO'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Conta la cantidad total de usuarios activos.
     *
     * @return Número de usuarios activos
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public int contarUsuariosActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE estado = 'ACTIVO'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Convierte un objeto Rol a su valor ordinal entero para la base de datos.
     *
     * @param rol El rol a convertir
     * @return El ordinal entero (ADMINISTRADOR=1, CAPTURISTA=2, REVISOR=3)
     */
    private int ordinalToInt(Rol rol) {
        switch (rol) {
            case ADMINISTRADOR:
                return 1;
            case CAPTURISTA:
                return 2;
            case REVISOR:
                return 3;
            default:
                return 1; // Default a admin
        }
    }

    /**
     Convierte un valor entero de la base de datos al nombre del Rol.
     *
     * @param rolId El ID numérico desde la base de datos
     * @return El nombre del Rol
     */
    private String rolIdToNombre(int rolId) {
        switch (rolId) {
            case 1:
                return "ADMINISTRADOR";
            case 2:
                return "CAPTURISTA";
            case 3:
                return "REVISOR";
            default:
                return "CAPTURISTA";
        }
    }
}