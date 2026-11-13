package com.farmacia.application.dto;

import com.farmacia.domain.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrarUsuarioCommandTest {

    @Test
    @DisplayName("1. Debe conservar correctamente todos los datos recibidos")
    void debeConservarCorrectamenteDatosRecibidos() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "USR-001",
                "María Elena",
                "Quispe Flores",
                "MiPasswordSegura#2026",
                Rol.ADMINISTRADORA
        );

        assertThat(command.codigoUsuario()).isEqualTo("USR-001");
        assertThat(command.nombres()).isEqualTo("María Elena");
        assertThat(command.apellidos()).isEqualTo("Quispe Flores");
        assertThat(command.password()).isEqualTo("MiPasswordSegura#2026");
        assertThat(command.rol()).isEqualTo(Rol.ADMINISTRADORA);
    }

    @Test
    @DisplayName("2. Debe conservar la contraseña original sin aplicar trim ni normalización")
    void debeConservarPasswordOriginalSinAplicarTrimNiNormalizacion() {
        String passwordConEspacios = "  clave Con Espacios  ";
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "USR-002",
                "Lucía",
                "Rojas Mendoza",
                passwordConEspacios,
                Rol.TRABAJADORA
        );

        assertThat(command.password())
                .isEqualTo(passwordConEspacios)
                .startsWith("  ")
                .endsWith("  ");
    }

    @Test
    @DisplayName("3. El método toString no debe contener la contraseña original")
    void debeEvitarExposicionDePasswordEnMetodoToString() {
        String passwordSecreta = "MiClaveSuperSecreta!123";
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "USR-003",
                "Carmen",
                "Solís Vega",
                passwordSecreta,
                Rol.ADMINISTRADORA
        );

        String representacion = command.toString();

        assertThat(representacion)
                .doesNotContain(passwordSecreta)
                .contains("USR-003")
                .contains("Carmen")
                .contains("Solís Vega")
                .contains("ADMINISTRADORA");
    }

    @Test
    @DisplayName("4. Debe conservar correctamente el rol asignado (ADMINISTRADORA y TRABAJADORA)")
    void debeConservarCorrectamenteRolAsignado() {
        RegistrarUsuarioCommand commandAdmin = new RegistrarUsuarioCommand(
                "USR-ADM",
                "Ana",
                "Paredes",
                "password123",
                Rol.ADMINISTRADORA
        );
        RegistrarUsuarioCommand commandTrabajadora = new RegistrarUsuarioCommand(
                "USR-TRAB",
                "Rosa",
                "Mamani",
                "password456",
                Rol.TRABAJADORA
        );

        assertThat(commandAdmin.rol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(commandTrabajadora.rol()).isEqualTo(Rol.TRABAJADORA);
    }
}

