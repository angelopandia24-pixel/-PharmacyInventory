package com.farmacia.presentation.dto;

import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.domain.model.Rol;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta web de usuario registrado.
 * Contiene estrictamente los seis campos del contrato aprobado sin exponer credenciales
 * ni detalles internos de la base de datos (HU-01 / SCRUM-88).
 */
public record UsuarioResponse(
        Long id,
        String codigoUsuario,
        String nombres,
        String apellidos,
        Rol rol,
        boolean activo
) {

    /**
     * Mapea un {@link UsuarioRegistradoResponse} proveniente de la capa de aplicación
     * hacia un {@link UsuarioResponse} de la capa de presentación.
     *
     * @param response respuesta devuelta por el caso de uso RegistrarUsuario
     * @return nueva instancia de {@link UsuarioResponse} o {@code null} si response es nulo
     */
    public static UsuarioResponse from(UsuarioRegistradoResponse response) {
        if (response == null) {
            return null;
        }
        return new UsuarioResponse(
                response.id(),
                response.codigoUsuario(),
                response.nombres(),
                response.apellidos(),
                response.rol(),
                response.activo()
        );
    }
}
