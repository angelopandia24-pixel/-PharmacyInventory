package com.farmacia.infrastructure.persistence.adapter;

import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import com.farmacia.infrastructure.persistence.mapper.UsuarioEntityMapper;
import com.farmacia.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de infraestructura que implementa el puerto UsuarioRepository de la capa de dominio.
 * Conecta las operaciones del dominio con Spring Data JPA y el mapeador de entidades.
 */
@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final UsuarioEntityMapper usuarioEntityMapper;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository, UsuarioEntityMapper usuarioEntityMapper) {
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.usuarioEntityMapper = usuarioEntityMapper;
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = usuarioEntityMapper.toEntity(usuario);
        UsuarioEntity savedEntity = usuarioJpaRepository.save(entity);
        return usuarioEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioJpaRepository.findById(id)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByCodigoUsuario(String codigoUsuario) {
        return usuarioJpaRepository.findByCodigoUsuario(codigoUsuario)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public boolean existsByCodigoUsuario(String codigoUsuario) {
        return usuarioJpaRepository.existsByCodigoUsuario(codigoUsuario);
    }
}
