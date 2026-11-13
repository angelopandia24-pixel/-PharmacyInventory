package com.farmacia.presentation.dto;

import java.time.Instant;

/**
 * Objeto de transferencia de datos (DTO) inmutable para respuestas de error HTTP.
 * Proporciona información segura y estructurada sin exponer trazas internas,
 * consultas SQL, credenciales ni contraseñas (HU-01 / SCRUM-88).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta
) {

    /**
     * Método fábrica de conveniencia para construir una respuesta de error con la marca de tiempo actual.
     *
     * @param status  código numérico de estado HTTP
     * @param error   descripción estándar del estado HTTP
     * @param mensaje mensaje seguro y comprensible para el cliente
     * @param ruta    URI de la solicitud que originó el error
     * @return nueva instancia de {@link ErrorResponse}
     */
    public static ErrorResponse of(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta);
    }
}

