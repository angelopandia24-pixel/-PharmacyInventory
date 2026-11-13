package com.farmacia.infrastructure.persistence.mapper;

import com.farmacia.domain.model.Usuario;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeador entre la entidad pura de dominio Usuario y la entidad de persistencia UsuarioEntity.
 * Garantiza la conservación íntegra de todos los atributos y desacopla el dominio de JPA.
 */
@Component
public class UsuarioEntityMapper {

    /**
     * Convierte una entidad de dominio Usuario a una entidad JPA UsuarioEntity.
     *
     * @param usuario Entidad de dominio a convertir.
     * @return UsuarioEntity equivalente, o null si la entrada es null.
     */
    public UsuarioEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return new UsuarioEntity(
                usuario.getId(),
                usuario.getCodigoUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getPasswordHash(),
                usuario.getRol(),
                usuario.isActivo()
        );
    }

    /**
     * Convierte una entidad JPA UsuarioEntity a la entidad de dominio Usuario.
     *
     * @param entity Entidad JPA a convertir.
     * @return Usuario de dominio equivalente, o null si la entrada es null.
     */
    public Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Usuario(
                entity.getId(),
                entity.getCodigoUsuario(),
                entity.getNombres(),
                entity.getApellidos(),
                entity.getPasswordHash(),
                entity.getRol(),
                entity.isActivo()
        );
    }
}
