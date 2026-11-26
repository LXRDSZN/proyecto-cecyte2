package com.mycompany.starbucks.model;

public class Usuario {
    private Integer id;
    private String usuario;
    private String password;
    private String rol;
    private String estado;

    public Usuario() {
    }

    public Usuario(Integer id, String usuario, String password, String rol, String estado) {
        this.id = id;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}