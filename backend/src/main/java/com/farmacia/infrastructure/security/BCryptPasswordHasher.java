package com.farmacia.infrastructure.security;

import com.farmacia.domain.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Adaptador de infraestructura para el hashing seguro de contraseñas mediante BCrypt.
 * Implementa el puerto del dominio PasswordHasher utilizando BCryptPasswordEncoder
 * de Spring Security Crypto con factor de costo 10 y generación automática de salt aleatorio.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final int FACTOR_COSTO = 10;
    private static final int MAX_BYTES_BCRYPT = 72;

    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Constructor por defecto utilizado por el contenedor de inyección de dependencias de Spring.
     * Inicializa BCryptPasswordEncoder con factor de costo 10.
     */
    public BCryptPasswordHasher() {
        this(new BCryptPasswordEncoder(FACTOR_COSTO));
    }

    /**
     * Constructor para inyección explícita del codificador (útil en pruebas unitarias y configuración avanzada).
     *
     * @param passwordEncoder Instancia configurada de BCryptPasswordEncoder.
     */
    public BCryptPasswordHasher(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "El codificador BCrypt no puede ser nulo");
    }

    /**
     * Genera un hash BCrypt seguro e irreversible a partir de la contraseña original en texto plano.
     * Respeta exactamente los caracteres y espacios deliberados sin recortar ni normalizar.
     *
     * @param rawPassword Contraseña original en texto plano.
     * @return Hash seguro estándar de 60 caracteres generado por el algoritmo BCrypt.
     * @throws IllegalArgumentException Si la contraseña es nula, vacía, compuesta únicamente por espacios
     *                                  o excede los 72 bytes en formato UTF-8.
     */
    @Override
    public String hash(String rawPassword) {
        validarEntrada(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    private void validarEntrada(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula, vacía ni contener solo espacios");
        }
        byte[] bytesUtf8 = rawPassword.getBytes(StandardCharsets.UTF_8);
        if (bytesUtf8.length > MAX_BYTES_BCRYPT) {
            throw new IllegalArgumentException("La contraseña no puede exceder " + MAX_BYTES_BCRYPT + " bytes en formato UTF-8");
        }
    }
}
