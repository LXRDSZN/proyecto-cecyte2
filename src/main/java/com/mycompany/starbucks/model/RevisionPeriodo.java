package com.mycompany.starbucks.model;

public class RevisionPeriodo {
    private Integer id;
    private PeriodoQuincenal periodo;
    private Usuario usuario;
    private String observaciones;
    private String estado;
    private java.time.LocalDate fechaRevision;

    public RevisionPeriodo() {
    }

    public RevisionPeriodo(Integer id, PeriodoQuincenal periodo, Usuario usuario, String observaciones, String estado, java.time.LocalDate fechaRevision) {
        this.id = id;
        this.periodo = periodo;
        this.usuario = usuario;
        this.observaciones = observaciones;
        this.estado = estado;
        this.fechaRevision = fechaRevision;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PeriodoQuincenal getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoQuincenal periodo) {
        this.periodo = periodo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public java.time.LocalDate getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(java.time.LocalDate fechaRevision) {
        this.fechaRevision = fechaRevision;
    }
}