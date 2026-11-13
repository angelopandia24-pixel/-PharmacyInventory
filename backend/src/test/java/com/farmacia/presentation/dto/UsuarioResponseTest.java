package com.farmacia.presentation.dto;

import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.domain.model.Rol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias de UsuarioResponse (HU-01 / SCRUM-88)")
class UsuarioResponseTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("1. Mapeo correcto desde UsuarioRegistradoResponse")
    void debeMapearCorrectamenteDesdeUsuarioRegistradoResponse() {
        UsuarioRegistradoResponse domainResponse = new UsuarioRegistradoResponse(
                10L,
                "USR-001",
                "María Carmen",
                "García Núñez",
                Rol.ADMINISTRADORA,
                true
        );

        UsuarioResponse webResponse = UsuarioResponse.from(domainResponse);

        assertThat(webResponse).isNotNull();
        assertThat(webResponse.id()).isEqualTo(10L);
        assertThat(webResponse.codigoUsuario()).isEqualTo("USR-001");
        assertThat(webResponse.nombres()).isEqualTo("María Carmen");
        assertThat(webResponse.apellidos()).isEqualTo("García Núñez");
        assertThat(webResponse.rol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(webResponse.activo()).isTrue();
    }

    @Test
    @DisplayName("2. Conservación del ID generado por la persistencia")
    void debeConservarIdGenerado() {
        UsuarioRegistradoResponse domainResponse = new UsuarioRegistradoResponse(
                9999L,
                "USR-9999",
                "Beatriz",
                "Quispe",
                Rol.TRABAJADORA,
                true
        );

        UsuarioResponse webResponse = UsuarioResponse.from(domainResponse);

        assertThat(webResponse.id()).isEqualTo(9999L);
    }

    @Test
    @DisplayName("3. Conservación de nombres, apellidos y código de usuario")
    void debeConservarNombresApellidosYCodigoUsuario() {
        UsuarioRegistradoResponse domainResponse = new UsuarioRegistradoResponse(
                1L,
                "USR-EXACT",
                "Ana María de los Ángeles",
                "García-Peña y Cañas",
                Rol.TRABAJADORA,
                true
        );

        UsuarioResponse webResponse = UsuarioResponse.from(domainResponse);

        assertThat(webResponse.codigoUsuario()).isEqualTo("USR-EXACT");
        assertThat(webResponse.nombres()).isEqualTo("Ana María de los Ángeles");
        assertThat(webResponse.apellidos()).isEqualTo("García-Peña y Cañas");
    }

    @Test
    @DisplayName("4. Conservación del rol de dominio (ADMINISTRADORA y TRABAJADORA)")
    void debeConservarElRol() {
        for (Rol rol : Rol.values()) {
            UsuarioRegistradoResponse domainResponse = new UsuarioRegistradoResponse(
                    1L,
                    "USR-" + rol.name(),
                    "Nombres",
                    "Apellidos",
                    rol,
                    true
            );

            UsuarioResponse webResponse = UsuarioResponse.from(domainResponse);

            assertThat(webResponse.rol()).isEqualTo(rol);
        }
    }

    @Test
    @DisplayName("5. Conservación del estado activo e inactivo")
    void debeConservarEstadoActivoEInactivo() {
        UsuarioRegistradoResponse activo = new UsuarioRegistradoResponse(1L, "U1", "N", "A", Rol.TRABAJADORA, true);
        UsuarioRegistradoResponse inactivo = new UsuarioRegistradoResponse(2L, "U2", "N", "A", Rol.TRABAJADORA, false);

        assertThat(UsuarioResponse.from(activo).activo()).isTrue();
        assertThat(UsuarioResponse.from(inactivo).activo()).isFalse();
    }

    @Test
    @DisplayName("6. Serialización JSON con exactamente los seis campos del contrato aprobado")
    void debeSerializarExactamenteLosSeisCamposDelContrato() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "USR-001",
                "María Carmen",
                "García Núñez",
                Rol.TRABAJADORA,
                true
        );

        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        List<String> fieldNames = new ArrayList<>();
        root.fieldNames().forEachRemaining(fieldNames::add);

        assertThat(fieldNames).containsExactlyInAnyOrder(
                "id",
                "codigoUsuario",
                "nombres",
                "apellidos",
                "rol",
                "activo"
        );
        assertThat(root.get("id").asLong()).isEqualTo(1L);
        assertThat(root.get("codigoUsuario").asText()).isEqualTo("USR-001");
        assertThat(root.get("nombres").asText()).isEqualTo("María Carmen");
        assertThat(root.get("apellidos").asText()).isEqualTo("García Núñez");
        assertThat(root.get("rol").asText()).isEqualTo("TRABAJADORA");
        assertThat(root.get("activo").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("7. Ausencia total de password y passwordHash en atributos, accesores y JSON")
    void noDebeContenerPasswordNiPasswordHash() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "USR-001",
                "María",
                "García",
                Rol.ADMINISTRADORA,
                true
        );

        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.has("password")).isFalse();
        assertThat(root.has("passwordHash")).isFalse();
        assertThat(UsuarioResponse.class.getRecordComponents())
                .noneMatch(rc -> rc.getName().toLowerCase().contains("password") || rc.getName().toLowerCase().contains("hash"));
    }

    @Test
    @DisplayName("8. Ausencia de información adicional no autorizada (sin entidades JPA ni datos internos)")
    void noDebeContenerInformacionInternaNoAutorizada() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "USR-001",
                "María",
                "García",
                Rol.ADMINISTRADORA,
                true
        );

        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.size()).isEqualTo(6);
    }
}

