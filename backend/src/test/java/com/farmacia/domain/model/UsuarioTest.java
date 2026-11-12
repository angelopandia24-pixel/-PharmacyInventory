package com.farmacia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private final String codigoValido = "USR-001";
    private final String nombresValidos = "María Elena";
    private final String apellidosValidos = "Quispe Flores";
    private final String passwordHashValido = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";

    @Test
    @DisplayName("1. Crear correctamente un usuario con datos válidos")
    void crearCorrectamenteUsuarioConDatosValidos() {
        Usuario usuario = new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        );

        assertThat(usuario).isNotNull();
        assertThat(usuario.getCodigoUsuario()).isEqualTo(codigoValido);
        assertThat(usuario.getNombres()).isEqualTo(nombresValidos);
        assertThat(usuario.getApellidos()).isEqualTo(apellidosValidos);
        assertThat(usuario.getPasswordHash()).isEqualTo(passwordHashValido);
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(usuario.toString()).doesNotContain(passwordHashValido);
    }

    @Test
    @DisplayName("2. Verificar que un usuario nuevo tenga estado activo")
    void verificarUsuarioNuevoTengaEstadoActivo() {
        Usuario usuario = new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        );

        assertThat(usuario.isActivo()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("3. Rechazar codigoUsuario nulo, vacío o con solo espacios")
    void rechazarCodigoUsuarioInvalido(String codigoInvalido) {
        assertThatThrownBy(() -> new Usuario(
                codigoInvalido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("código de usuario");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("4. Rechazar nombres nulos, vacíos o con solo espacios")
    void rechazarNombresInvalidos(String nombresInvalidos) {
        assertThatThrownBy(() -> new Usuario(
                codigoValido,
                nombresInvalidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("nombres");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("5. Rechazar apellidos nulos, vacíos o con solo espacios")
    void rechazarApellidosInvalidos(String apellidosInvalidos) {
        assertThatThrownBy(() -> new Usuario(
                codigoValido,
                nombresValidos,
                apellidosInvalidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apellidos");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("6. Rechazar passwordHash nulo, vacío o con solo espacios")
    void rechazarPasswordHashInvalido(String passwordHashInvalido) {
        assertThatThrownBy(() -> new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashInvalido,
                Rol.ADMINISTRADORA
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("passwordHash");
    }

    @Test
    @DisplayName("7. Rechazar rol nulo")
    void rechazarRolNulo() {
        assertThatThrownBy(() -> new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("rol");
    }

    @Test
    @DisplayName("8. Verificar que se conserva el rol ADMINISTRADORA")
    void verificarConservaRolAdministradora() {
        Usuario usuario = new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        );

        assertThat(usuario.getRol()).isEqualTo(Rol.ADMINISTRADORA);
    }

    @Test
    @DisplayName("9. Verificar que se conserva el rol TRABAJADORA")
    void verificarConservaRolTrabajadora() {
        Usuario usuario = new Usuario(
                "USR-002",
                "Lucía",
                "Rojas Mendoza",
                passwordHashValido,
                Rol.TRABAJADORA
        );

        assertThat(usuario.getRol()).isEqualTo(Rol.TRABAJADORA);
    }

    @Test
    @DisplayName("10. Permitir crear un usuario con id nulo")
    void permitirCrearUsuarioConIdNulo() {
        Usuario usuario = new Usuario(
                codigoValido,
                nombresValidos,
                apellidosValidos,
                passwordHashValido,
                Rol.ADMINISTRADORA
        );

        assertThat(usuario.getId()).isNull();
    }
}
