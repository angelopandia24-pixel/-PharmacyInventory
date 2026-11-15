package com.farmacia.infrastructure.security;

import com.farmacia.domain.repository.UsuarioAuthPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.farmacia.domain.repository.UsuarioAuthPort;
import com.farmacia.domain.repository.UsuarioAuthPort.UsuarioDatos;

import java.util.Optional;
// TODO [PRODUCCION]: Borrar esta clase y crear el adaptador real conectado a la tabla Usuarios de SQL Server.
@Component

public class UsuarioAuthDummyAdapter implements UsuarioAuthPort {

    private final PasswordEncoder passwordEncoder;

    public UsuarioAuthDummyAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UsuarioDatos> buscarPorCodigoAcceso(String codigoAcceso) {
        // Usuario de prueba temporal (simula la BD de SQL Server)
        if ("DUENA01".equalsIgnoreCase(codigoAcceso)) {
            return Optional.of(new UsuarioDatos(
                    "DUENA01",
                    passwordEncoder.encode("12345678"), // Clave encriptada al vuelo
                    "Administradora Biofarma",
                    "ADMINISTRADORA",
                    true
            ));
        }
        return Optional.empty();
    }
}