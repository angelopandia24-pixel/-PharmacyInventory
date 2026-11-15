package com.farmacia.presentation.controller;

import com.farmacia.application.dto.AuthResponse;
import com.farmacia.application.dto.LoginRequest;
import com.farmacia.application.dto.RefreshRequest;
import com.farmacia.domain.exception.AuthException;
import com.farmacia.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Error de autenticación", "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "mensaje", "Ocurrió un problema al procesar la solicitud"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            return ResponseEntity.ok(authService.refresh(request));
        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sesión inválida", "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "mensaje", "Ocurrió un problema al renovar la sesión"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @RequestBody(required = false) RefreshRequest refreshRequest) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = (refreshRequest != null) ? refreshRequest.getRefreshToken() : null;

        if ((accessToken == null || accessToken.isBlank()) && (refreshToken == null || refreshToken.isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Solicitud inválida", "mensaje", "Debe proporcionar al menos un token para cerrar sesión"));
        }

        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }
}