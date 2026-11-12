package com.farmacia.infrastructure.persistence.adapter;

import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import com.farmacia.infrastructure.persistence.mapper.UsuarioEntityMapper;
import com.farmacia.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAdapterTest {

    @Mock
    private UsuarioJpaRepository usuarioJpaRepository;

    @Mock
    private UsuarioEntityMapper usuarioEntityMapper;

    @InjectMocks
    private UsuarioRepositoryAdapter usuarioRepositoryAdapter;

    private final String codigoValido = "USR-001";
    private final String passwordHash = "$2a$10$hashEjemplo1234567890123456789012345678901234567890";

    @Test
    @DisplayName("Debe persistir un usuario nuevo y retornar la entidad con el id generado")
    void debeGuardarUsuarioNuevoYRetornarConIdGenerado() {
        Usuario usuarioEntrada = new Usuario(
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA
        );
        UsuarioEntity entitySinId = new UsuarioEntity(
                null,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );
        UsuarioEntity entityGuardada = new UsuarioEntity(
                1L,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );
        Usuario usuarioGuardado = new Usuario(
                1L,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );

        when(usuarioEntityMapper.toEntity(usuarioEntrada)).thenReturn(entitySinId);
        when(usuarioJpaRepository.save(entitySinId)).thenReturn(entityGuardada);
        when(usuarioEntityMapper.toDomain(entityGuardada)).thenReturn(usuarioGuardado);

        Usuario resultado = usuarioRepositoryAdapter.save(usuarioEntrada);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCodigoUsuario()).isEqualTo(codigoValido);
        verify(usuarioJpaRepository).save(entitySinId);
    }

    @Test
    @DisplayName("Debe consultar un usuario existente mediante su id")
    void debeConsultarUsuarioPorIdCuandoExiste() {
        Long id = 1L;
        UsuarioEntity entity = new UsuarioEntity(
                id,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );
        Usuario usuario = new Usuario(
                id,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );

        when(usuarioJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(usuarioEntityMapper.toDomain(entity)).thenReturn(usuario);

        Optional<Usuario> resultado = usuarioRepositoryAdapter.findById(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(id);
        verify(usuarioJpaRepository).findById(id);
    }

    @Test
    @DisplayName("Debe retornar Optional.empty() cuando un usuario no existe por id")
    void debeRetornarOptionalVacioCuandoNoExistePorId() {
        Long idInexistente = 999L;
        when(usuarioJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = usuarioRepositoryAdapter.findById(idInexistente);

        assertThat(resultado).isEmpty();
        verify(usuarioJpaRepository).findById(idInexistente);
    }

    @Test
    @DisplayName("Debe consultar un usuario existente mediante su código de usuario")
    void debeConsultarUsuarioPorCodigoCuandoExiste() {
        UsuarioEntity entity = new UsuarioEntity(
                1L,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );
        Usuario usuario = new Usuario(
                1L,
                codigoValido,
                "María Elena",
                "Quispe Flores",
                passwordHash,
                Rol.ADMINISTRADORA,
                true
        );

        when(usuarioJpaRepository.findByCodigoUsuario(codigoValido)).thenReturn(Optional.of(entity));
        when(usuarioEntityMapper.toDomain(entity)).thenReturn(usuario);

        Optional<Usuario> resultado = usuarioRepositoryAdapter.findByCodigoUsuario(codigoValido);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigoUsuario()).isEqualTo(codigoValido);
        verify(usuarioJpaRepository).findByCodigoUsuario(codigoValido);
    }

    @Test
    @DisplayName("Debe retornar Optional.empty() cuando un usuario no existe por su código")
    void debeRetornarOptionalVacioCuandoNoExistePorCodigo() {
        String codigoInexistente = "USR-999";
        when(usuarioJpaRepository.findByCodigoUsuario(codigoInexistente)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = usuarioRepositoryAdapter.findByCodigoUsuario(codigoInexistente);

        assertThat(resultado).isEmpty();
        verify(usuarioJpaRepository).findByCodigoUsuario(codigoInexistente);
    }

    @Test
    @DisplayName("Debe retornar true cuando el código de usuario ya existe en la persistencia")
    void debeRetornarTrueCuandoCodigoExiste() {
        when(usuarioJpaRepository.existsByCodigoUsuario(codigoValido)).thenReturn(true);

        boolean existe = usuarioRepositoryAdapter.existsByCodigoUsuario(codigoValido);

        assertThat(existe).isTrue();
        verify(usuarioJpaRepository).existsByCodigoUsuario(codigoValido);
    }

    @Test
    @DisplayName("Debe retornar false cuando el código de usuario no existe en la persistencia")
    void debeRetornarFalseCuandoCodigoNoExiste() {
        String codigoInexistente = "USR-999";
        when(usuarioJpaRepository.existsByCodigoUsuario(codigoInexistente)).thenReturn(false);

        boolean existe = usuarioRepositoryAdapter.existsByCodigoUsuario(codigoInexistente);

        assertThat(existe).isFalse();
        verify(usuarioJpaRepository).existsByCodigoUsuario(codigoInexistente);
    }
}

