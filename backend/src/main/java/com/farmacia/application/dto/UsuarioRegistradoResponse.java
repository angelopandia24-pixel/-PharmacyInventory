package com.farmacia.application.dto;

import com.farmacia.domain.model.Rol;

/**
 * Objeto inmutable de transferencia de datos que representa el resultado exitoso
 * del registro de un usuario.
 * Garantiza que nunca se devuelvan ni expongan la contraseña original ni el passwordHash.
 */
public record UsuarioRegistradoResponse(
        Long id,
        String codigoUsuario,
        String nombres,
        String apellidos,
        Rol rol,
        boolean activo
) {
}

