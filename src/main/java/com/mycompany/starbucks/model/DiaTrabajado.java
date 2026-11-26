package com.mycompany.starbucks.model;

public class DiaTrabajado {
    private Integer id;
    private Colaborador colaborador;
    private PeriodoQuincenal periodo;
    private Integer diasTrabajados;

    public DiaTrabajado() {
    }

    public DiaTrabajado(Integer id, Colaborador colaborador, PeriodoQuincenal periodo, Integer diasTrabajados) {
        this.id = id;
        this.colaborador = colaborador;
        this.periodo = periodo;
        this.diasTrabajados = diasTrabajados;
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

    public Integer getDiasTrabajados() {
        return diasTrabajados;
    }

    public void setDiasTrabajados(Integer diasTrabajados) {
        this.diasTrabajados = diasTrabajados;
    }
}