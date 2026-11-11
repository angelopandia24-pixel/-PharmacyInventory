package com.farmacia.presentation.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Ajusta este import según el paquete exacto donde guardaste tu RegisterDto
import com.farmacia.application.dto.RegisterDto;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto request) {
        // Aquí llamas a tu Use Case / Servicio de aplicación
        return ResponseEntity.status(201).body("Usuario registrado con éxito");
    }
}