package com.mycompany.starbucks.service;

import com.mycompany.starbucks.dao.DiaTrabajadoDAO;
import com.mycompany.starbucks.model.Usuario;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

public class DiaTrabajadoService {
    private final DiaTrabajadoDAO dao = new DiaTrabajadoDAO();
    private final PeriodoQuincenalService periodos = new PeriodoQuincenalService();

    public OptionalInt consultar(int periodo, int colaborador) throws SQLException {
        return dao.consultar(periodo, colaborador);
    }

    public void guardar(Usuario actor, int periodoId, int colaboradorId, int dias) throws SQLException {
        if (actor == null || !"ACTIVO".equals(actor.getEstado())
                || !("ADMINISTRADOR".equals(actor.getRol()) || "CAPTURISTA".equals(actor.getRol()))) {
            throw new IllegalArgumentException("Tu rol solo permite consultar los días trabajados.");
        }
        var periodo = periodos.porId(periodoId)
                .orElseThrow(() -> new IllegalArgumentException("El periodo ya no existe."));
        if (!"CAPTURA".equals(periodo.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden modificar días en periodos en captura.");
        }
        long limite = ChronoUnit.DAYS.between(periodo.getFechaInicio(), periodo.getFechaFin()) + 1;
        if (dias < 0 || dias > limite) {
            throw new IllegalArgumentException("Ingresa entre 0 y " + limite + " días.");
        }
        dao.guardar(periodoId, colaboradorId, dias);
    }
}
