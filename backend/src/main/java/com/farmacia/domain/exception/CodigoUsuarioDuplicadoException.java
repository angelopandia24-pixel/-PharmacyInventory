package com.farmacia.domain.exception;

/**
 * Excepción de negocio lanzada cuando se intenta registrar un usuario con un
 * código personal que ya existe en el sistema.
 */
public class CodigoUsuarioDuplicadoException extends RuntimeException {

    public CodigoUsuarioDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public CodigoUsuarioDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public static CodigoUsuarioDuplicadoException porCodigo(String codigoUsuario) {
        return new CodigoUsuarioDuplicadoException(
                "El código de usuario '" + codigoUsuario + "' ya se encuentra registrado en el sistema"
        );
    }

    public static CodigoUsuarioDuplicadoException porCodigo(String codigoUsuario, Throwable causa) {
        return new CodigoUsuarioDuplicadoException(
                "El código de usuario '" + codigoUsuario + "' ya se encuentra registrado en el sistema",
                causa
        );
    }
}
