package com.mycompany.starbucks.service;

import com.mycompany.starbucks.dao.IncidenciaDAO;
import com.mycompany.starbucks.model.Incidencia;
import com.mycompany.starbucks.model.Usuario;
import com.mycompany.starbucks.model.Rol;
import com.mycompany.starbucks.dao.PrenominaDAO;
import com.mycompany.starbucks.model.Colaborador;
import com.mycompany.starbucks.model.PeriodoQuincenal;
import com.mycompany.starbucks.model.TipoIncidencia;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class IncidenciaService {

    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();

    public Incidencia guardar(Usuario actor, Incidencia incidencia) throws SQLException {
        return OperacionBD.transaccion(conn -> {
            OperacionBD.exigirRol(conn, actor, Rol.ADMINISTRADOR, Rol.CAPTURISTA);
            if (incidencia.getColaborador() == null || incidencia.getColaborador().getId() == null
                    || incidencia.getPeriodo() == null || incidencia.getPeriodo().getId() == null
                    || incidencia.getTipo() == null || incidencia.getFecha() == null) {
                throw new IllegalArgumentException("Selecciona colaborador, periodo, tipo y fecha.");
            }
            int periodo = incidencia.getPeriodo().getId();
            var quincena = new PrenominaDAO().bloquearPeriodo(conn, periodo);
            if (!"CAPTURA".equals(quincena.estado())) {
                throw new IllegalArgumentException("Solo se permiten cambios en periodos en captura.");
            }
            if (new ColaboradorService().porId(incidencia.getColaborador().getId()).isEmpty()) {
                throw new IllegalArgumentException("El colaborador no existe.");
            }
            var inicio = incidencia.getFecha();
            var fin = incidencia.fechaFinalEfectiva();
            if (inicio.isBefore(quincena.inicio()) || fin.isAfter(quincena.fin())
                    || fin.isBefore(inicio)) {
                throw new IllegalArgumentException("El rango debe estar ordenado y dentro de la quincena.");
            }
            validarDatosEspecificos(incidencia);
            if (incidencia.getId() != null) {
                var anterior = porId(incidencia.getId())
                        .orElseThrow(() -> new IllegalArgumentException("La incidencia no existe."));
                if (!"PENDIENTE".equals(anterior.getEstado())
                        || !java.util.Objects.equals(periodo, anterior.getPeriodo().getId())) {
                    throw new IllegalArgumentException("Solo se pueden editar incidencias pendientes dentro del periodo original.");
                }
            }
            for (Incidencia existente : porColaborador(incidencia.getColaborador())) {
                if (!java.util.Objects.equals(periodo, existente.getPeriodo().getId())
                        || existente.getFecha() == null || existente.getTipo() == null
                        || existente.getId().equals(incidencia.getId()) || "CANCELADA".equals(existente.getEstado())
                        || inicio.isAfter(existente.fechaFinalEfectiva()) || fin.isBefore(existente.getFecha())) {
                    continue;
                }
                if (existente.getTipo() == incidencia.getTipo() || incompatible(existente.getTipo(), incidencia.getTipo())) {
                    throw new IllegalArgumentException("El rango se cruza con una incidencia "
                            + existente.getTipo() + " del mismo colaborador.");
                }
            }
            incidencia.setEstado("PENDIENTE");
            boolean alta = incidencia.getId() == null;
            if (alta) incidenciaDAO.guardar(conn, incidencia);
            else incidenciaDAO.actualizar(conn, incidencia);
            new BitacoraService().registrar(conn, actor.getId(), alta ? "ALTA_INCIDENCIA" : "MODIFICAR_INCIDENCIA",
                    "incidencia", incidencia.getId(), incidencia.getTipo() + " · " + inicio + " a " + fin);
            return incidencia;
        });
    }

    private void validarDatosEspecificos(Incidencia incidencia) {
        TipoIncidencia tipo = incidencia.getTipo();
        if ((tipo == TipoIncidencia.FALTA || tipo == TipoIncidencia.RETARDO)
                && !incidencia.getFecha().equals(incidencia.fechaFinalEfectiva())) {
            throw new IllegalArgumentException("Las faltas y los retardos se registran por día.");
        }
        String clasificacion = incidencia.getClasificacion();
        Integer minutos = incidencia.getMinutosRetardo();
        if (tipo == TipoIncidencia.RETARDO && ((minutos != null && (minutos < 1 || minutos >= 1440))
                || (minutos == null && vacio(clasificacion)))) {
            throw new IllegalArgumentException("Indica una duración de 1 a 1439 minutos o una clasificación.");
        }
        if (tipo == TipoIncidencia.PERMISO && (vacio(clasificacion) || vacio(incidencia.getTratamiento())
                || incidencia.getAutorizacionDocumentada() == null)) {
            throw new IllegalArgumentException("Completa el tipo, la autorización y el tratamiento del permiso.");
        }
        if (tipo == TipoIncidencia.VACACIONES && !Boolean.TRUE.equals(incidencia.getAutorizacionDocumentada())) {
            throw new IllegalArgumentException("Confirma que las fechas de vacaciones están aprobadas.");
        }
        if (tipo == TipoIncidencia.INCAPACIDAD && (vacio(clasificacion) || vacio(incidencia.getReferenciaDocumental()))) {
            throw new IllegalArgumentException("Indica el tipo y la referencia documental de la incapacidad.");
        }
        if (excede(clasificacion, 100) || excede(incidencia.getTratamiento(), 150)
                || excede(incidencia.getReferenciaDocumental(), 150)) {
            throw new IllegalArgumentException("La clasificación admite 100 caracteres y el tratamiento y la referencia admiten 150.");
        }
        if (tipo != TipoIncidencia.RETARDO) incidencia.setMinutosRetardo(null);
        if (tipo == TipoIncidencia.FALTA || tipo == TipoIncidencia.VACACIONES) incidencia.setClasificacion(null);
        if (tipo != TipoIncidencia.PERMISO && tipo != TipoIncidencia.VACACIONES) incidencia.setAutorizacionDocumentada(null);
        if (tipo != TipoIncidencia.PERMISO) incidencia.setTratamiento(null);
        if (tipo != TipoIncidencia.INCAPACIDAD) incidencia.setReferenciaDocumental(null);
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private boolean excede(String texto, int maximo) {
        return texto != null && texto.length() > maximo;
    }

    private boolean incompatible(TipoIncidencia primero, TipoIncidencia segundo) {
        return primero != TipoIncidencia.PERMISO && segundo != TipoIncidencia.PERMISO;
    }

    public Optional<Incidencia> porId(Integer id) throws SQLException {
        return incidenciaDAO.porId(id);
    }

    public List<Incidencia> listarTodos() throws SQLException {
        return incidenciaDAO.listarTodos();
    }

    public List<Incidencia> porColaborador(Colaborador colaborador) throws SQLException {
        return incidenciaDAO.porColaborador(colaborador);
    }

    public List<Incidencia> porPeriodoYTipo(PeriodoQuincenal periodo, TipoIncidencia tipo) throws SQLException {
        return incidenciaDAO.porPeriodoYTipo(periodo, tipo);
    }

    public int contarPorTipo(TipoIncidencia tipo) throws SQLException {
        List<Incidencia> incidencias = listarTodos();
        int count = 0;
        for (Incidencia i : incidencias) {
            if (tipo == i.getTipo()) {
                count++;
            }
        }
        return count;
    }

    public int contarPorEstado(String estado) throws SQLException {
        List<Incidencia> incidencias = listarTodos();
        int count = 0;
        for (Incidencia i : incidencias) {
            if (estado.equals(i.getEstado())) {
                count++;
            }
        }
        return count;
    }
}