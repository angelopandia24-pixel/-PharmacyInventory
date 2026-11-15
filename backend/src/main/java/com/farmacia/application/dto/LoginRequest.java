package com.farmacia.application.dto;

public class LoginRequest {
    private String codigoAcceso;
    private String contrasena;

    // Getters y Setters
    public String getCodigoAcceso() { return codigoAcceso; }
    public void setCodigoAcceso(String codigoAcceso) { this.codigoAcceso = codigoAcceso; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}

