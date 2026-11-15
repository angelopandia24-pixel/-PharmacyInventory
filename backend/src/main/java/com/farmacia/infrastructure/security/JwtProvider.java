package com.farmacia.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessExpirationMs; // 1 hora

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs; // 7 días

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 1. Generar Access Token (Para navegar en la API)
    public String generateToken(String codigoAcceso, String rol) {
        return Jwts.builder()
                .subject(codigoAcceso)
                .claim("rol", rol)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // 2. Generar Refresh Token (Solo para renovar sesión)
    public String generateRefreshToken(String codigoAcceso) {
        return Jwts.builder()
                .subject(codigoAcceso)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public String getRolFromToken(String token) {
        return getAllClaims(token).get("rol", String.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getAllClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getAllClaims(token).get("type", String.class));
    }

    public boolean validateToken(String token) {
        try {
            getAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}