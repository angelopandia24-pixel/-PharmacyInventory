package com.farmacia.infrastructure.persistence.mapper;

import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioEntityMapperTest {

    private UsuarioEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UsuarioEntityMapper();
    }

    @Test
    @DisplayName("Debe mapear un Usuario de dominio existente a UsuarioEntity conservando todos los atributos")
    void debeMapearUsuarioDominioAEntityCorrectamente() {
        Usuario usuario = new Usuario(
                1L,
                "USR-001",
                "María Elena",
                "Quispe Flores",
                "$2a$10$hashEjemplo1234567890123456789012345678901234567890",
                Rol.ADMINISTRADORA,
                true
        );

        UsuarioEntity entity = mapper.toEntity(usuario);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCodigoUsuario()).isEqualTo("USR-001");
        assertThat(entity.getNombres()).isEqualTo("María Elena");
        assertThat(entity.getApellidos()).isEqualTo("Quispe Flores");
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$hashEjemplo1234567890123456789012345678901234567890");
        assertThat(entity.getRol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(entity.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Debe mapear un Usuario nuevo sin ID a UsuarioEntity con ID nulo")
    void debeMapearUsuarioNuevoSinIdAEntity() {
        Usuario usuarioNuevo = new Usuario(
                "USR-002",
                "Lucía",
                "Rojas Mendoza",
                "$2a$10$hashEjemplo1234567890123456789012345678901234567890",
                Rol.TRABAJADORA
        );

        UsuarioEntity entity = mapper.toEntity(usuarioNuevo);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCodigoUsuario()).isEqualTo("USR-002");
        assertThat(entity.getNombres()).isEqualTo("Lucía");
        assertThat(entity.getApellidos()).isEqualTo("Rojas Mendoza");
        assertThat(entity.getRol()).isEqualTo(Rol.TRABAJADORA);
        assertThat(entity.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Debe mapear un UsuarioEntity a Usuario de dominio conservando todos los atributos")
    void debeMapearEntityAUsuarioDominioCorrectamente() {
        UsuarioEntity entity = new UsuarioEntity(
                10L,
                "USR-010",
                "Carlos",
                "Vargas Silva",
                "$2a$10$hashEjemplo1234567890123456789012345678901234567890",
                Rol.TRABAJADORA,
                true
        );

        Usuario usuario = mapper.toDomain(entity);

        assertThat(usuario).isNotNull();
        assertThat(usuario.getId()).isEqualTo(10L);
        assertThat(usuario.getCodigoUsuario()).isEqualTo("USR-010");
        assertThat(usuario.getNombres()).isEqualTo("Carlos");
        assertThat(usuario.getApellidos()).isEqualTo("Vargas Silva");
        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$10$hashEjemplo1234567890123456789012345678901234567890");
        assertThat(usuario.getRol()).isEqualTo(Rol.TRABAJADORA);
        assertThat(usuario.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Debe retornar null cuando el Usuario de dominio es null")
    void debeRetornarNullCuandoUsuarioDominioEsNull() {
        UsuarioEntity entity = mapper.toEntity(null);
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Debe retornar null cuando el UsuarioEntity es null")
    void debeRetornarNullCuandoEntityEsNull() {
        Usuario usuario = mapper.toDomain(null);
        assertThat(usuario).isNull();
    }
}

