package com.farmacia.infrastructure.persistence.repository;

import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad UsuarioEntity.
 */
@Repository
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    /**
     * Consulta una entidad de usuario por su código personal.
     *
     * @param codigoUsuario Código único del usuario.
     * @return Optional con la entidad si se encuentra, o vacío si no existe.
     */
    Optional<UsuarioEntity> findByCodigoUsuario(String codigoUsuario);

    /**
     * Comprueba si existe una entidad de usuario con el código especificado.
     *
     * @param codigoUsuario Código a verificar.
     * @return true si ya existe en la base de datos; false en caso contrario.
     */
    boolean existsByCodigoUsuario(String codigoUsuario);
}

