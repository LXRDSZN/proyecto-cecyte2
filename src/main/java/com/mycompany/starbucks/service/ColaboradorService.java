package com.mycompany.starbucks.service;

import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.dao.ColaboradorDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ColaboradorService {

    private final ColaboradorDAO colaboradorDAO = new ColaboradorDAO();

    public Colaborador guardar(Usuario actor, Colaborador colaborador) throws SQLException {
        return OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.CAPTURISTA);
            validarCaptura(colaborador);
            colaboradorDAO.guardar(conn, colaborador);
            new BitacoraService().registrar(conn, actor.getId(), "ALTA_COLABORADOR", "colaborador",
                    colaborador.getId(), colaborador.getNumeroEmpleado() + " · " + colaborador.getEstado());
            return colaborador;
        });
    }

    public Optional<Colaborador> porId(Integer id) throws SQLException {
        return colaboradorDAO.porId(id);
    }

    public List<Colaborador> listarTodos() throws SQLException {
        return colaboradorDAO.listarTodos();
    }

    public List<Colaborador> buscarPorNumero(String numero) throws SQLException {
        return colaboradorDAO.buscarPorNumero(numero);
    }

    public List<Colaborador> buscarPorNombre(String nombre) throws SQLException {
        return colaboradorDAO.buscarPorNombre(nombre);
    }

    public Colaborador actualizar(Usuario actor, Colaborador colaborador) throws SQLException {
        return OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.CAPTURISTA);
            validarCaptura(colaborador);
            if (colaborador.getId() == null || colaboradorDAO.porId(colaborador.getId()).isEmpty()) {
                throw new IllegalArgumentException("El colaborador ya no existe.");
            }
            colaboradorDAO.actualizar(conn, colaborador);
            new BitacoraService().registrar(conn, actor.getId(), "MODIFICAR_COLABORADOR", "colaborador",
                    colaborador.getId(), colaborador.getNumeroEmpleado() + " · " + colaborador.getEstado());
            return colaborador;
        });
    }

    private void validarCaptura(Colaborador colaborador) throws SQLException {
        String numero = colaborador.getNumeroEmpleado();
        String nombre = colaborador.getNombreCompleto();
        Double salario = colaborador.getSalarioDiario();
        if (numero == null || numero.isBlank() || numero.length() > 20
                || nombre == null || nombre.isBlank() || nombre.length() > 150
                || salario == null || !Double.isFinite(salario) || salario <= 0 || salario > 99999999.99
                || !("ACTIVO".equals(colaborador.getEstado()) || "INACTIVO".equals(colaborador.getEstado()))) {
            throw new IllegalArgumentException("Verifica el número, el nombre, el salario diario y el estado.");
        }
        for (Colaborador existente : buscarPorNumero(numero)) {
            if (!existente.getId().equals(colaborador.getId())) {
                throw new IllegalArgumentException("El número de empleado ya está registrado.");
            }
        }
    }

    public boolean existePorNumero(String numero) throws SQLException {
        List<Colaborador> resultados = buscarPorNumero(numero);
        return !resultados.isEmpty();
    }

    public boolean existePorNombre(String nombre) throws SQLException {
        List<Colaborador> resultados = buscarPorNombre(nombre);
        return !resultados.isEmpty();
    }

    public int contarActivos() throws SQLException {
        List<Colaborador> colaboradores = listarTodos();
        int count = 0;
        for (Colaborador c : colaboradores) {
            if ("ACTIVO".equals(c.getEstado())) {
                count++;
            }
        }
        return count;
    }

    public int contarInactivos() throws SQLException {
        List<Colaborador> colaboradores = listarTodos();
        int count = 0;
        for (Colaborador c : colaboradores) {
            if ("INACTIVO".equals(c.getEstado())) {
                count++;
            }
        }
        return count;
    }
}