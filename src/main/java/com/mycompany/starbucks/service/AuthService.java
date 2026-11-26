package com.mycompany.starbucks.service;

import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Servicio de autenticación para el sistema STARBUCKS — Pre-nómina.
 * <p>
 * Para este prototipo académico, las contraseñas se comparan en texto plano.
 * En producción se utilizaría BCrypt u otro mecanismo de hashing.
 */
public class AuthService {

    /**
     * Autentica a un usuario consultando la base de datos MariaDB.
     *
     * @param usuario Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Optional con el usuario autenticado si la credencial son válidas, vacío en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public Optional<Usuario> autenticar(String usuario, String password) throws SQLException {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT u.id, u.usuario, u.password, r.nombre as rol_nombre, u.estado "
                + "FROM usuarios u "
                + "JOIN roles r ON u.rol_id = r.id "
                + "WHERE u.usuario = ? AND u.estado = 'ACTIVO'";

        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String passwordAlmacenada = resultSet.getString("password");
                    String rolNombre = resultSet.getString("rol_nombre");

                    // Comparar contraseña en texto plano (prototipo académico)
                    if (password.equals(passwordAlmacenada)) {
                        Usuario user = new Usuario();
                        user.setId(resultSet.getInt("id"));
                        user.setUsuario(resultSet.getString("usuario"));
                        user.setRol(rolNombre != null ? rolNombre : "CAPTURISTA");
                        user.setEstado(resultSet.getString("estado"));
                        return Optional.of(user);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica si las credenciales son válidas sin devolver el objeto Usuario completo.
     *
     * @param usuario Nombre de usuario
     * @param password Contraseña en texto plano
     * @return true si las credenciales son válidas, false en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean verificarCredenciales(String usuario, String password) throws SQLException {
        return autenticar(usuario, password).isPresent();
    }

    /**
     * Recupera un usuario por su nombre de usuario.
     *
     * @param usuario Nombre de usuario
     * @return Optional con el usuario si existe, vacío en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public Optional<Usuario> porNombre(String usuario) throws SQLException {
        if (usuario == null || usuario.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT u.id, u.usuario, u.password, r.nombre as rol_nombre, u.estado "
                + "FROM usuarios u "
                + "JOIN roles r ON u.rol_id = r.id "
                + "WHERE u.usuario = ? AND u.estado = 'ACTIVO'";

        try (Connection connection = com.mycompany.starbucks.config.DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Usuario user = new Usuario();
                    user.setId(resultSet.getInt("id"));
                    user.setUsuario(resultSet.getString("usuario"));
                    user.setRol(resultSet.getString("rol_nombre"));
                    user.setEstado(resultSet.getString("estado"));
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica si un usuario existe en la base de datos.
     *
     * @param usuario Nombre de usuario
     * @return true si el usuario existe y está activo, false en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean existe(String usuario) throws SQLException {
        return porNombre(usuario).isPresent();
    }

    /**
     * Obtiene el rol de un usuario.
     *
     * @param usuario Nombre de usuario
     * @return El rol del usuario, o null si no existe
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public String obtenerRol(String usuario) throws SQLException {
        Optional<Usuario> user = porNombre(usuario);
        return user.map(Usuario::getRol).orElse(null);
    }

    /**
     * Verifica si el usuario tiene el rol especificado.
     *
     * @param usuario Nombre de usuario
     * @param rol Rol a verificar
     * @return true si el usuario tiene el rol especificado, false en caso contrario
     * @throws SQLException Si hay error de conexión a la base de datos
     */
    public boolean tieneRol(String usuario, String rol) throws SQLException {
        String userRol = obtenerRol(usuario);
        return userRol != null && userRol.equalsIgnoreCase(rol);
    }
}
