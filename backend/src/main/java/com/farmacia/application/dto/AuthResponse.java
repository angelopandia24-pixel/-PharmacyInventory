package com.farmacia.application.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String nombreCompleto;
    private String rol;

    public AuthResponse(String accessToken, String refreshToken, String nombreCompleto, String rol) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRol() { return rol; }
}