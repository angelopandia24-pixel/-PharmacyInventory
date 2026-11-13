package com.farmacia.infrastructure.config;

import com.farmacia.application.usecase.RegistrarUsuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.domain.service.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Spring para instanciar e inyectar casos de uso de usuario.
 * Conecta los puertos de dominio (UsuarioRepository y PasswordHasher) con el
 * caso de uso de aplicación RegistrarUsuario, manteniendo el dominio y la aplicación
 * libres de anotaciones de framework según Clean Architecture (HU-01 / SCRUM-88).
 */
@Configuration
public class UsuarioUseCaseConfig {

    /**
     * Declara el bean del caso de uso {@link RegistrarUsuario}.
     *
     * @param usuarioRepository adaptador que implementa el puerto {@link UsuarioRepository}
     * @param passwordHasher    adaptador que implementa el puerto {@link PasswordHasher}
     * @return instancia configurada de {@link RegistrarUsuario}
     */
    @Bean
    public RegistrarUsuario registrarUsuario(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        return new RegistrarUsuario(usuarioRepository, passwordHasher);
    }
}
