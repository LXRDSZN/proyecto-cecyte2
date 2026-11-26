package com.mycompany.starbucks.service;

import com.mycompany.starbucks.dao.DiaTrabajadoDAO;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.dao.PrenominaDAO;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

public class DiaTrabajadoService {
    private final DiaTrabajadoDAO dao = new DiaTrabajadoDAO();

    public OptionalInt consultar(int periodo, int colaborador) throws SQLException {
        return dao.consultar(periodo, colaborador);
    }

    public void guardar(Usuario actor, int periodoId, int colaboradorId, int dias) throws SQLException {
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.CAPTURISTA);
            var periodo = new PrenominaDAO().bloquearPeriodo(conn, periodoId);
            if (!"CAPTURA".equals(periodo.estado())) {
                throw new IllegalArgumentException("Solo se pueden modificar días en periodos en captura.");
            }
            long limite = ChronoUnit.DAYS.between(periodo.inicio(), periodo.fin()) + 1;
            if (dias < 0 || dias > limite) {
                throw new IllegalArgumentException("Ingresa entre 0 y " + limite + " días.");
            }
            int id = dao.guardar(conn, periodoId, colaboradorId, dias);
            new BitacoraService().registrar(conn, actor.getId(), "CAPTURAR_DIAS", "dias_trabajados", id,
                    "Periodo " + periodoId + " · Colaborador " + colaboradorId + " · Días " + dias);
            return null;
        });
    }
}
