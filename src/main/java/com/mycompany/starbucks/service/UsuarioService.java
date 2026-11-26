package com.mycompany.starbucks.service;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.dao.UsuarioDAO;
import com.mycompany.starbucks.dao.UsuarioDAO.Cuenta;
import com.mycompany.starbucks.dao.UsuarioDAO.Evento;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.model.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UsuarioService {
    private final UsuarioDAO dao = new UsuarioDAO();

    public List<Cuenta> listar(Usuario actor) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            exigirAdministrador(conn, actor);
            return dao.listar(conn);
        }
    }

    public List<Evento> bitacora(Usuario actor) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            exigirAdministrador(conn, actor);
            return dao.bitacora(conn);
        }
    }

    public void crear(Usuario actor, String nombre, Rol rol, String estado,
                      String clave, String confirmacion) throws SQLException {
        validarDatos(nombre, rol, estado);
        validarClave(clave, confirmacion);
        modificar(actor, conn -> {
            int id = dao.crear(conn, nombre.strip(), clave, rol, estado);
            dao.auditar(conn, actor.getId(), id, "ALTA_USUARIO", nombre.strip() + " · " + rol + " · " + estado);
        });
    }

    public void editar(Usuario actor, int id, String nombre, Rol rol, String estado) throws SQLException {
        validarDatos(nombre, rol, estado);
        modificar(actor, conn -> {
            Cuenta cuenta = obtener(conn, id);
            validarCambio(conn, actor, cuenta, rol, estado);
            dao.actualizar(conn, id, nombre.strip(), rol, estado);
            dao.auditar(conn, actor.getId(), id, "MODIFICAR_USUARIO",
                    cuenta.nombre() + " → " + nombre.strip() + " · " + rol + " · " + estado);
        });
    }

    public void cambiarEstado(Usuario actor, int id, boolean activo) throws SQLException {
        modificar(actor, conn -> {
            Cuenta cuenta = obtener(conn, id);
            String estado = activo ? "ACTIVO" : "INACTIVO";
            validarCambio(conn, actor, cuenta, cuenta.rol(), estado);
            dao.actualizar(conn, id, cuenta.nombre(), cuenta.rol(), estado);
            dao.auditar(conn, actor.getId(), id, "ESTADO_USUARIO", cuenta.nombre() + " · " + estado);
        });
    }

    public void cambiarClave(Usuario actor, int id, String clave, String confirmar) throws SQLException {
        validarClave(clave, confirmar);
        modificar(actor, conn -> {
            Cuenta cuenta = obtener(conn, id);
            dao.actualizarClave(conn, id, clave);
            dao.auditar(conn, actor.getId(), id, "CAMBIAR_CONTRASENA", cuenta.nombre());
        });
    }

    private Cuenta obtener(Connection conn, int id) throws SQLException {
        Cuenta cuenta = dao.buscar(conn, id, true);
        if (cuenta == null) throw new IllegalArgumentException("El usuario ya no existe.");
        return cuenta;
    }

    private void validarCambio(Connection conn, Usuario actor, Cuenta cuenta, Rol rol, String estado)
            throws SQLException {
        boolean conservaAcceso = rol == Rol.ADMINISTRADOR && "ACTIVO".equals(estado);
        if (!conservaAcceso && cuenta.rol() == Rol.ADMINISTRADOR && "ACTIVO".equals(cuenta.estado())) {
            if (dao.contarAdministradores(conn) <= 1) {
                throw new IllegalArgumentException("Debe permanecer al menos un administrador activo.");
            }
            if (cuenta.id() == actor.getId()) {
                throw new IllegalArgumentException("Otro administrador debe cambiar el acceso de tu cuenta.");
            }
        }
    }

    private void validarDatos(String nombre, Rol rol, String estado) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 50 || rol == null
                || !("ACTIVO".equals(estado) || "INACTIVO".equals(estado))) {
            throw new IllegalArgumentException("Completa el nombre de usuario, el rol y un estado válido.");
        }
    }

    private void validarClave(String clave, String confirmar) {
        if (clave == null || clave.isBlank() || clave.length() > 100 || !clave.equals(confirmar)) {
            throw new IllegalArgumentException("Las contraseñas deben coincidir y tener entre 1 y 100 caracteres.");
        }
    }

    private void exigirAdministrador(Connection conn, Usuario actor) throws SQLException {
        Cuenta cuenta = actor == null || actor.getId() == null ? null : dao.buscar(conn, actor.getId(), false);
        if (cuenta == null || cuenta.rol() != Rol.ADMINISTRADOR || !"ACTIVO".equals(cuenta.estado())) {
            throw new IllegalArgumentException("Se requiere un administrador activo.");
        }
    }

    @FunctionalInterface
    private interface Cambio {
        void realizar(Connection conn) throws SQLException;
    }

    private void modificar(Usuario actor, Cambio cambio) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.bloquearAdministracion(conn);
                exigirAdministrador(conn, actor);
                cambio.realizar(conn);
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                if (ex instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Ese nombre de usuario ya existe.", ex);
                }
                throw ex;
            }
        }
    }
}
