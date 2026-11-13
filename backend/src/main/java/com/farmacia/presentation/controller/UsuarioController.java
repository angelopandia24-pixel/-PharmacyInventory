package com.farmacia.presentation.controller;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.application.usecase.RegistrarUsuario;
import com.farmacia.presentation.dto.RegistrarUsuarioRequest;
import com.farmacia.presentation.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controlador REST para el registro de nuevos usuarios en el sistema Biofarma.
 * Expone el endpoint POST /api/v1/usuarios conforme a Clean Architecture (HU-01 / SCRUM-88).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final RegistrarUsuario registrarUsuario;

    public UsuarioController(RegistrarUsuario registrarUsuario) {
        this.registrarUsuario = Objects.requireNonNull(registrarUsuario, "El caso de uso RegistrarUsuario no puede ser nulo");
    }

    /**
     * Registra un nuevo usuario en el sistema a partir de la solicitud web recibida.
     *
     * @param request DTO de solicitud validado con Bean Validation
     * @return ResponseEntity con el DTO de usuario registrado y código HTTP 201 Created
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> registrarUsuario(@Valid @RequestBody RegistrarUsuarioRequest request) {
        RegistrarUsuarioCommand command = request.toCommand();
        UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

        if (response == null || response.id() == null || response.id() <= 0L) {
            throw new IllegalStateException("El resultado de registro de usuario es inválido o no contiene un identificador generado válido");
        }

        UsuarioResponse body = UsuarioResponse.from(response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
