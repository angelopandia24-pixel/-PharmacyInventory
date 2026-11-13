package com.farmacia.domain.exception;

/**
 * Excepción de negocio lanzada cuando los datos proporcionados para el registro
 * no cumplen con las reglas o validaciones requeridas por el dominio.
 */
public class DatosUsuarioInvalidosException extends RuntimeException {

    public DatosUsuarioInvalidosException(String mensaje) {
        super(mensaje);
    }
}

