package com.mycompany.starbucks.model;

public class Colaborador {
    private Integer id;
    private String numeroEmpleado;
    private String nombreCompleto;
    private Double salarioDiario;
    private String estado;

    public Colaborador() {
    }

    public Colaborador(Integer id, String numeroEmpleado, String nombreCompleto, Double salarioDiario, String estado) {
        this.id = id;
        this.numeroEmpleado = numeroEmpleado;
        this.nombreCompleto = nombreCompleto;
        this.salarioDiario = salarioDiario;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroEmpleado() {
        return numeroEmpleado;
    }

    public void setNumeroEmpleado(String numeroEmpleado) {
        this.numeroEmpleado = numeroEmpleado;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Double getSalarioDiario() {
        return salarioDiario;
    }

    public void setSalarioDiario(Double salarioDiario) {
        this.salarioDiario = salarioDiario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}