package com.mycompany.starbucks.model;

public class Incidencia {
    private java.time.LocalDate fechaFin;
    private Integer minutosRetardo;
    private String clasificacion;
    private Boolean autorizacionDocumentada;
    private String tratamiento;
    private String referenciaDocumental;
    private Integer id;
    private Colaborador colaborador;
    private PeriodoQuincenal periodo;
    private TipoIncidencia tipo;
    private java.time.LocalDate fecha;
    private String observaciones;
    private String estado;

    public Incidencia() {
    }

    public Incidencia(Integer id, Colaborador colaborador, PeriodoQuincenal periodo, TipoIncidencia tipo, java.time.LocalDate fecha, String observaciones, String estado) {
        this.id = id;
        this.colaborador = colaborador;
        this.periodo = periodo;
        this.tipo = tipo;
        this.fecha = fecha;
        this.observaciones = observaciones;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public PeriodoQuincenal getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoQuincenal periodo) {
        this.periodo = periodo;
    }

    public TipoIncidencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoIncidencia tipo) {
        this.tipo = tipo;
    }

    public java.time.LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(java.time.LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    public java.time.LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(java.time.LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
    public Integer getMinutosRetardo() {
        return minutosRetardo;
    }

    public void setMinutosRetardo(Integer minutosRetardo) {
        this.minutosRetardo = minutosRetardo;
    }
    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }
    public Boolean getAutorizacionDocumentada() {
        return autorizacionDocumentada;
    }

    public void setAutorizacionDocumentada(Boolean autorizacionDocumentada) {
        this.autorizacionDocumentada = autorizacionDocumentada;
    }
    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }
    public String getReferenciaDocumental() {
        return referenciaDocumental;
    }

    public void setReferenciaDocumental(String referenciaDocumental) {
        this.referenciaDocumental = referenciaDocumental;
    }
    public java.time.LocalDate fechaFinalEfectiva() {
        return fechaFin == null ? fecha : fechaFin;
    }
}
