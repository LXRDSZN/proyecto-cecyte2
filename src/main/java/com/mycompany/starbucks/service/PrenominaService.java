package com.mycompany.starbucks.service;

import com.mycompany.starbucks.config.DatabaseConnection;
import com.mycompany.starbucks.dao.PrenominaDAO;
import com.mycompany.starbucks.dao.PrenominaDAO.Regla;
import com.mycompany.starbucks.dao.PrenominaDAO.Resultado;
import com.mycompany.starbucks.model.TipoIncidencia;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.model.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PrenominaService {
    private final PrenominaDAO dao = new PrenominaDAO();

    public List<Resultado> consultar(int periodo) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            return dao.resultados(conn, periodo);
        }
    }

    public record Detalle(Integer diasTrabajados, List<PrenominaDAO.Incidencia> incidencias) {}

    public Detalle consultarDetalle(int periodo, int colaborador) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            var persona = dao.personal(conn, periodo).stream().filter(fila -> fila.id() == colaborador)
                    .findFirst().orElse(null);
            return new Detalle(persona == null ? null : persona.diasTrabajados(), dao.incidencias(conn, periodo)
                    .stream().filter(incidencia -> incidencia.colaborador() == colaborador).toList());
        }
    }

    public Map<TipoIncidencia, Regla> consultarReglas(int periodo) throws SQLException {
        try (var conn = DatabaseConnection.getConnection()) {
            return dao.reglas(conn, periodo);
        }
    }

    public void autorizarRegla(Usuario actor, int periodo, TipoIncidencia tipo, boolean descuenta, String referencia)
            throws SQLException {
        if (tipo == null || referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("Selecciona el tipo e indica la referencia a la aprobación.");
        }
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR);
            exigirCaptura(dao.bloquearPeriodo(conn, periodo));
            dao.guardarRegla(conn, periodo, tipo, new Regla(descuenta, referencia.strip()), actor.getId());
            dao.auditar(conn, actor.getId(), periodo, "AUTORIZAR_REGLA",
                    tipo + (descuenta ? " · descuento por día" : " · sin descuento") + " · " + referencia.strip());
            return null;
        });
    }

    public List<Resultado> calcular(Usuario actor, int periodo) throws SQLException {
        return OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR);
            var quincena = dao.bloquearPeriodo(conn, periodo);
            exigirCaptura(quincena);
            var resultados = prepararResultados(conn, periodo, quincena);
            dao.reemplazarResultados(conn, periodo, resultados);
            dao.auditar(conn, actor.getId(), periodo, "CALCULAR_PRENOMINA", "Resultados guardados: " + resultados.size());
            return resultados;
        });
    }

    public void enviarARevision(Usuario actor, int periodo) throws SQLException {
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR);
            var quincena = dao.bloquearPeriodo(conn, periodo);
            exigirCaptura(quincena);
            comprobarResultados(conn, periodo, quincena, "PENDIENTE");
            cambiarEstado(conn, periodo, actor, "REVISION", "Resultados enviados a revisión.");
            return null;
        });
    }

    public void revisar(Usuario actor, int periodo, String observaciones) throws SQLException {
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.REVISOR);
            var quincena = dao.bloquearPeriodo(conn, periodo);
            exigirRevision(quincena);
            comprobarResultados(conn, periodo, quincena, "REVISION");
            cambiarEstado(conn, periodo, actor, "REVISADO", validarObservaciones(observaciones));
            return null;
        });
    }

    public void autorizar(Usuario actor, int periodo) throws SQLException {
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.REVISOR);
            var quincena = dao.bloquearPeriodo(conn, periodo);
            exigirRevision(quincena);
            comprobarResultados(conn, periodo, quincena, "REVISADO");
            cambiarEstado(conn, periodo, actor, "AUTORIZADO", "Quincena autorizada.");
            return null;
        });
    }

    public void devolverACaptura(Usuario actor, int periodo, String observaciones) throws SQLException {
        OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.REVISOR);
            exigirRevision(dao.bloquearPeriodo(conn, periodo));
            cambiarEstado(conn, periodo, actor, "CAPTURA", validarObservaciones(observaciones));
            return null;
        });
    }

    private void cambiarEstado(Connection conn, int periodo, Usuario actor, String estado, String nota)
            throws SQLException {
        dao.cambiarEstado(conn, periodo, estado);
        dao.registrarRevision(conn, periodo, actor.getId(), estado, nota);
        dao.auditar(conn, actor.getId(), periodo, "PRENOMINA_" + estado, nota);
    }

    private void comprobarResultados(Connection conn, int periodo, PrenominaDAO.Quincena quincena,
                                    String estadoEsperado) throws SQLException {
        var anteriores = dao.resultados(conn, periodo).stream()
                .collect(Collectors.toMap(Resultado::colaborador, fila -> fila));
        var actuales = prepararResultados(conn, periodo, quincena);
        if (anteriores.size() != actuales.size()) {
            throw new IllegalArgumentException("La quincena requiere un cálculo vigente y completo.");
        }
        for (Resultado actual : actuales) {
            var anterior = anteriores.get(actual.colaborador());
            if (anterior == null || !estadoEsperado.equals(anterior.estado()) || anterior.salario() == null
                    || anterior.importe() == null || anterior.salario().compareTo(actual.salario()) != 0
                    || anterior.diasDescuento() != actual.diasDescuento()
                    || anterior.importe().compareTo(actual.importe()) != 0) {
                throw new IllegalArgumentException("Los resultados cambiaron o falta completar la etapa anterior.");
            }
        }
    }

    private void exigirRevision(PrenominaDAO.Quincena quincena) {
        if (!"REVISION".equals(quincena.estado())) {
            throw new IllegalArgumentException("La operación requiere una quincena en revisión.");
        }
    }

    private String validarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            throw new IllegalArgumentException("Registra una observación para continuar.");
        }
        return observaciones.strip();
    }

    private List<Resultado> prepararResultados(Connection conn, int periodo, PrenominaDAO.Quincena quincena)
            throws SQLException {
        var reglas = dao.reglas(conn, periodo);
        if (reglas.isEmpty()) {
            throw new IllegalArgumentException("Cálculo pendiente. La quincena no tiene reglas autorizadas.");
        }
        var personal = dao.personal(conn, periodo);
        if (personal.isEmpty()) throw new IllegalArgumentException("No hay colaboradores para calcular.");
        var ids = personal.stream().map(PrenominaDAO.Personal::id).collect(Collectors.toSet());
        Map<Integer, Set<LocalDate>> descuentos = new HashMap<>();
        for (var incidencia : dao.incidencias(conn, periodo)) {
            if (!ids.contains(incidencia.colaborador()) || incidencia.tipo() == null || incidencia.inicio() == null
                    || incidencia.fin() == null || incidencia.inicio().isBefore(quincena.inicio())
                    || incidencia.fin().isAfter(quincena.fin()) || incidencia.fin().isBefore(incidencia.inicio())
                    || incidencia.estado() == null) {
                throw new IllegalArgumentException("Hay incidencias incompletas o fuera de la quincena.");
            }
            var regla = reglas.get(incidencia.tipo());
            if (regla == null) {
                throw new IllegalArgumentException("Cálculo pendiente. Falta una regla autorizada para " + incidencia.tipo() + ".");
            }
            if (regla.descuenta()) {
                var fechas = descuentos.computeIfAbsent(incidencia.colaborador(), id -> new HashSet<>());
                incidencia.inicio().datesUntil(incidencia.fin().plusDays(1)).forEach(fechas::add);
            }
        }
        long limite = ChronoUnit.DAYS.between(quincena.inicio(), quincena.fin()) + 1;
        List<Resultado> resultados = new ArrayList<>();
        for (var persona : personal) {
            if (persona.diasTrabajados() == null || persona.diasTrabajados() < 0
                    || persona.diasTrabajados() > limite || persona.salario().signum() <= 0) {
                throw new IllegalArgumentException("Verifica los días trabajados y el salario de " + persona.numero() + ".");
            }
            int dias = descuentos.getOrDefault(persona.id(), Set.of()).size();
            if (persona.diasTrabajados() + dias > limite) {
                throw new IllegalArgumentException("Los días trabajados y de descuento exceden la quincena para "
                        + persona.numero() + ".");
            }
            var importe = persona.salario().multiply(java.math.BigDecimal.valueOf(dias));
            resultados.add(new Resultado(persona.id(), persona.numero(), persona.nombre(), persona.salario(),
                    dias, importe, "PENDIENTE"));
        }
        return resultados;
    }

    private void exigirCaptura(PrenominaDAO.Quincena quincena) {
        if (!"CAPTURA".equals(quincena.estado())) {
            throw new IllegalArgumentException("La operación requiere una quincena en captura.");
        }
    }

}
