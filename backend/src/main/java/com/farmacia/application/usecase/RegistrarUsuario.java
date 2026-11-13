package com.farmacia.application.usecase;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.domain.service.PasswordHasher;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Caso de uso para el registro de nuevos usuarios en Biofarma (HU-01 / SCRUM-87).
 * Implementado como clase Java pura conforme a Clean Architecture, sin dependencias
 * de frameworks web ni de persistencia (sin anotaciones de Spring).
 */
public class RegistrarUsuario {

    private static final int MAX_LONGITUD_CODIGO = 50;
    private static final int MAX_LONGITUD_TEXTO = 100;
    private static final int MIN_LONGITUD_PASSWORD = 8;
    private static final int MAX_BYTES_PASSWORD_BCRYPT = 72;
    private static final int MAX_LONGITUD_HASH_DB = 255;

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    /**
     * Inyección de dependencias mediante constructor para desacoplamiento y testeabilidad.
     *
     * @param usuarioRepository Puerto de salida para la persistencia de usuarios.
     * @param passwordHasher    Puerto de salida para el hashing de contraseñas.
     */
    public RegistrarUsuario(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "El repositorio de usuarios no puede ser nulo");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "El hasher de contraseñas no puede ser nulo");
    }

    /**
     * Ejecuta la lógica de negocio para registrar un usuario en el sistema.
     *
     * @param command Comando con los datos requeridos para el registro.
     * @return DTO inmutable con los datos del usuario registrado de manera segura.
     * @throws DatosUsuarioInvalidosException   Si algún dato no cumple las invariantes de negocio.
     * @throws CodigoUsuarioDuplicadoException Si el código de usuario ya se encuentra registrado.
     * @throws IllegalStateException           Si ocurre un error interno en el servicio de hashing o valor inválido de hash.
     */
    public UsuarioRegistradoResponse ejecutar(RegistrarUsuarioCommand command) {
        validarComando(command);

        String codigoNormalizado = validarYNormalizarCodigoUsuario(command.codigoUsuario());
        String nombresNormalizados = validarYNormalizarNombres(command.nombres());
        String apellidosNormalizados = validarYNormalizarApellidos(command.apellidos());
        validarPassword(command.password());
        validarRol(command.rol());

        // Comprobación de unicidad de código previa
        if (usuarioRepository.existsByCodigoUsuario(codigoNormalizado)) {
            throw CodigoUsuarioDuplicadoException.porCodigo(codigoNormalizado);
        }

        // Hashing seguro de la contraseña (se envía la contraseña original exacta sin trim)
        String passwordHash = passwordHasher.hash(command.password());
        validarHashGenerado(passwordHash);

        // Creación de la entidad de dominio y persistencia (conservando el hash exacto sin trim)
        Usuario nuevoUsuario = new Usuario(
                codigoNormalizado,
                nombresNormalizados,
                apellidosNormalizados,
                passwordHash,
                command.rol()
        );

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        return new UsuarioRegistradoResponse(
                usuarioGuardado.getId(),
                usuarioGuardado.getCodigoUsuario(),
                usuarioGuardado.getNombres(),
                usuarioGuardado.getApellidos(),
                usuarioGuardado.getRol(),
                usuarioGuardado.isActivo()
        );
    }

    private void validarComando(RegistrarUsuarioCommand command) {
        if (command == null) {
            throw new DatosUsuarioInvalidosException("El comando de registro no puede ser nulo");
        }
    }

    private String validarYNormalizarCodigoUsuario(String codigoUsuario) {
        if (codigoUsuario == null || codigoUsuario.trim().isEmpty()) {
            throw new DatosUsuarioInvalidosException("El código de usuario es obligatorio y no puede estar vacío");
        }
        String codigoNormalizado = codigoUsuario.trim();
        if (codigoNormalizado.length() > MAX_LONGITUD_CODIGO) {
            throw new DatosUsuarioInvalidosException("El código de usuario no puede exceder " + MAX_LONGITUD_CODIGO + " caracteres");
        }
        return codigoNormalizado;
    }

    private String validarYNormalizarNombres(String nombres) {
        if (nombres == null || nombres.trim().isEmpty()) {
            throw new DatosUsuarioInvalidosException("Los nombres son obligatorios y no pueden estar vacíos");
        }
        String nombresNormalizados = nombres.trim();
        if (nombresNormalizados.length() > MAX_LONGITUD_TEXTO) {
            throw new DatosUsuarioInvalidosException("Los nombres no pueden exceder " + MAX_LONGITUD_TEXTO + " caracteres");
        }
        return nombresNormalizados;
    }

    private String validarYNormalizarApellidos(String apellidos) {
        if (apellidos == null || apellidos.trim().isEmpty()) {
            throw new DatosUsuarioInvalidosException("Los apellidos son obligatorios y no pueden estar vacíos");
        }
        String apellidosNormalizados = apellidos.trim();
        if (apellidosNormalizados.length() > MAX_LONGITUD_TEXTO) {
            throw new DatosUsuarioInvalidosException("Los apellidos no pueden exceder " + MAX_LONGITUD_TEXTO + " caracteres");
        }
        return apellidosNormalizados;
    }

    private void validarPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new DatosUsuarioInvalidosException("La contraseña es obligatoria y no puede estar vacía");
        }
        int codePoints = password.codePointCount(0, password.length());
        if (codePoints < MIN_LONGITUD_PASSWORD) {
            throw new DatosUsuarioInvalidosException("La contraseña debe tener al menos " + MIN_LONGITUD_PASSWORD + " caracteres");
        }
        byte[] bytesUtf8 = password.getBytes(StandardCharsets.UTF_8);
        if (bytesUtf8.length > MAX_BYTES_PASSWORD_BCRYPT) {
            throw new DatosUsuarioInvalidosException("La contraseña no puede exceder " + MAX_BYTES_PASSWORD_BCRYPT + " bytes en formato UTF-8");
        }
    }

    private void validarRol(Rol rol) {
        if (rol == null) {
            throw new DatosUsuarioInvalidosException("El rol es obligatorio y no puede ser nulo");
        }
    }

    private void validarHashGenerado(String passwordHash) {
        if (passwordHash == null
                || passwordHash.isBlank()
                || passwordHash.length() != passwordHash.strip().length()
                || passwordHash.length() > MAX_LONGITUD_HASH_DB) {
            throw new IllegalStateException("Error al generar el hash de la contraseña");
        }
    }
}
