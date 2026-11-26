package com.mycompany.starbucks.model;

public class PeriodoQuincenal {
    private Integer id;
    private String identificador;
    private java.time.LocalDate fechaInicio;
    private java.time.LocalDate fechaFin;
    private String estado;

    public PeriodoQuincenal() {
    }

    public PeriodoQuincenal(Integer id, String identificador, java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin, String estado) {
        this.id = id;
        this.identificador = identificador;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public java.time.LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(java.time.LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public java.time.LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(java.time.LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}