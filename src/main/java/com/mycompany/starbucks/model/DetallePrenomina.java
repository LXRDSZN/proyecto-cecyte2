package com.mycompany.starbucks.model;

public class DetallePrenomina {
    private Integer id;
    private PeriodoQuincenal periodo;
    private Colaborador colaborador;
    private Integer diasDescuento;
    private java.math.BigDecimal descuentoPreliminar;
    private String estado;

    public DetallePrenomina() {
    }

    public DetallePrenomina(Integer id, PeriodoQuincenal periodo, Colaborador colaborador, Integer diasDescuento, java.math.BigDecimal descuentoPreliminar, String estado) {
        this.id = id;
        this.periodo = periodo;
        this.colaborador = colaborador;
        this.diasDescuento = diasDescuento;
        this.descuentoPreliminar = descuentoPreliminar;
        this.estado = estado;
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

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public Integer getDiasDescuento() {
        return diasDescuento;
    }

    public void setDiasDescuento(Integer diasDescuento) {
        this.diasDescuento = diasDescuento;
    }

    public java.math.BigDecimal getDescuentoPreliminar() {
        return descuentoPreliminar;
    }

    public void setDescuentoPreliminar(java.math.BigDecimal descuentoPreliminar) {
        this.descuentoPreliminar = descuentoPreliminar;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}