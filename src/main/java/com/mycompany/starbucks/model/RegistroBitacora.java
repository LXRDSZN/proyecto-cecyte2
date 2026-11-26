package com.mycompany.starbucks.model;

public class RegistroBitacora {
    private Integer id;
    private Usuario usuario;
    private String accion;
    private String entidad;
    private Integer identificador;
    private java.time.LocalDateTime fechaHora;
    private String informacionRelevante;

    public RegistroBitacora() {
    }

    public RegistroBitacora(Integer id, Usuario usuario, String accion, String entidad, Integer identificador, java.time.LocalDateTime fechaHora, String informacionRelevante) {
        this.id = id;
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.identificador = identificador;
        this.fechaHora = fechaHora;
        this.informacionRelevante = informacionRelevante;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Integer getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Integer identificador) {
        this.identificador = identificador;
    }

    public java.time.LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(java.time.LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getInformacionRelevante() {
        return informacionRelevante;
    }

    public void setInformacionRelevante(String informacionRelevante) {
        this.informacionRelevante = informacionRelevante;
    }
}