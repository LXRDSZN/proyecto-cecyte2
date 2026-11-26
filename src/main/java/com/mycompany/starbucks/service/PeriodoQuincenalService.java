package com.mycompany.starbucks.service;

import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.dao.PeriodoQuincenalDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PeriodoQuincenalService {

    private final PeriodoQuincenalDAO periodoQuincenalDAO = new PeriodoQuincenalDAO();

    public PeriodoQuincenal guardar(Usuario actor, PeriodoQuincenal periodo) throws SQLException {
        validarAdministrador(actor);
        synchronized (PeriodoQuincenalService.class) {
            validarIdentificador(periodo.getIdentificador());
            var inicio = periodo.getFechaInicio();
            var fin = periodo.getFechaFin();
            if (inicio == null || fin == null
                    || !(inicio.getDayOfMonth() == 1 && fin.equals(inicio.withDayOfMonth(15))
                    || inicio.getDayOfMonth() == 16 && fin.equals(inicio.withDayOfMonth(inicio.lengthOfMonth())))) {
                throw new IllegalArgumentException("Usa del día 1 al 15 o del 16 al último día del mes.");
            }
            if (identificadorExiste(periodo.getIdentificador())) {
                throw new IllegalArgumentException("Ya existe un periodo con ese identificador.");
            }
            for (PeriodoQuincenal existente : listarTodos()) {
                if (!inicio.isAfter(existente.getFechaFin()) && !fin.isBefore(existente.getFechaInicio())) {
                    throw new IllegalArgumentException("Las fechas se superponen con " + existente.getIdentificador());
                }
            }
            periodo.setEstado("CAPTURA");
            return periodoQuincenalDAO.guardar(periodo);
        }
    }

    public Optional<PeriodoQuincenal> porId(Integer id) throws SQLException {
        return periodoQuincenalDAO.porId(id);
    }

    public List<PeriodoQuincenal> listarTodos() throws SQLException {
        return periodoQuincenalDAO.listarTodos();
    }

    public void renombrar(Usuario actor, int id, String identificador) throws SQLException {
        validarAdministrador(actor);
        validarIdentificador(identificador);
        var periodo = porId(id).orElseThrow(() -> new IllegalArgumentException("El periodo ya no existe."));
        if (!"CAPTURA".equals(periodo.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden editar periodos en captura.");
        }
        if (periodo.getIdentificador().equals(identificador)) {
            return;
        }
        if (identificadorExiste(identificador)) {
            throw new IllegalArgumentException("Ya existe un periodo con ese identificador.");
        }
        periodoQuincenalDAO.renombrar(id, identificador);
    }

    private void validarAdministrador(Usuario actor) {
        if (actor == null || !"ACTIVO".equals(actor.getEstado()) || !"ADMINISTRADOR".equals(actor.getRol())) {
            throw new IllegalArgumentException("Solo un administrador activo puede gestionar periodos.");
        }
    }

    private void validarIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank() || identificador.length() > 20) {
            throw new IllegalArgumentException("El identificador debe tener entre 1 y 20 caracteres.");
        }
    }

    public boolean identificadorExiste(String identificador) throws SQLException {
        return periodoQuincenalDAO.identificadorExiste(identificador);
    }

    public int contarPorEstado(String estado) throws SQLException {
        List<PeriodoQuincenal> periodos = listarTodos();
        int count = 0;
        for (PeriodoQuincenal p : periodos) {
            if (estado.equals(p.getEstado())) {
                count++;
            }
        }
        return count;
    }
}