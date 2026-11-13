package com.farmacia.domain.repository;

import com.farmacia.domain.model.Usuario;

import java.util.Optional;

/**
 * Puerto de salida del dominio para la persistencia y consulta de usuarios.
 * Define el contrato de operaciones necesarias para el registro y consulta
 * sin acoplarse a tecnologías, bases de datos o frameworks de infraestructura.
 */
public interface UsuarioRepository {

    /**
     * Almacena un usuario en el medio de persistencia.
     *
     * @param usuario Entidad de dominio a persistir.
     * @return Entidad persistida, incluyendo el identificador generado por el sistema.
     */
    Usuario save(Usuario usuario);

    /**
     * Consulta un usuario mediante su identificador único.
     *
     * @param id Identificador único del usuario.
     * @return Optional con el usuario si existe, o vacío si no se encuentra.
     */
    Optional<Usuario> findById(Long id);

    /**
     * Consulta un usuario mediante su código personal único.
     *
     * @param codigoUsuario Código personal del usuario.
     * @return Optional con el usuario si existe, o vacío si no se encuentra.
     */
    Optional<Usuario> findByCodigoUsuario(String codigoUsuario);

    /**
     * Comprueba si ya existe un usuario registrado con el código especificado.
     * Permite validar la regla de negocio de no duplicidad de códigos antes de persistir.
     *
     * @param codigoUsuario Código personal del usuario a verificar.
     * @return true si ya existe un usuario con dicho código; false en caso contrario.
     */
    boolean existsByCodigoUsuario(String codigoUsuario);
}

