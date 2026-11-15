package com.farmacia.domain.repository;

import java.util.Optional;

public interface UsuarioAuthPort {

    // Estructura mínima que el Login necesita leer de la BD
    record UsuarioDatos(
            String codigoAcceso,
            String contrasenaHash,
            String nombreCompleto,
            String rol,
            boolean activo
    ) {}

    Optional<UsuarioDatos> buscarPorCodigoAcceso(String codigoAcceso);
}