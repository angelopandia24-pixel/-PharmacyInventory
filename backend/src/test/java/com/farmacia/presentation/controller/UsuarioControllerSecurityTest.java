package com.farmacia.presentation.controller;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.application.usecase.RegistrarUsuario;
import com.farmacia.domain.model.Rol;
import com.farmacia.infrastructure.config.SecurityConfig;
import com.farmacia.presentation.advice.UsuarioExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UsuarioController.class, HealthController.class})
@ContextConfiguration(classes = {
        UsuarioController.class,
        HealthController.class,
        SecurityConfig.class,
        UsuarioExceptionHandler.class
})
@DisplayName("Pruebas de Seguridad y Autorización con Spring Security (HU-01 / SCRUM-88 / Etapa 4)")
class UsuarioControllerSecurityTest {

    private static final String USUARIOS_ENDPOINT = "/api/v1/usuarios";
    private static final String HEALTH_ENDPOINT = "/api/v1/health";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrarUsuario registrarUsuario;

    private static String crearJsonRequest(String codigo, String nombres, String apellidos, String password, String rol) {
        return String.format("""
                {
                  "codigoUsuario": "%s",
                  "nombres": "%s",
                  "apellidos": "%s",
                  "password": "%s",
                  "rol": "%s"
                }
                """, codigo, nombres, apellidos, password, rol);
    }

    @Nested
    @DisplayName("1. Acceso Autorizado para Rol ADMINISTRADORA (HTTP 201 Created)")
    class AccesoAutorizadoAdministradoraTests {

        @Test
        @DisplayName("Escenario 1: Una ADMINISTRADORA autenticada puede registrar una TRABAJADORA y recibe HTTP 201")
        void administradoraPuedeRegistrarTrabajadora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-TRAB-01", "Rosa", "Flores", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(1L, "USR-TRAB-01", "Rosa", "Flores", Rol.TRABAJADORA, true));

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("admin1").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.codigoUsuario").value("USR-TRAB-01"))
                    .andExpect(jsonPath("$.rol").value("TRABAJADORA"));

            verify(registrarUsuario, times(1)).ejecutar(any(RegistrarUsuarioCommand.class));
        }

        @Test
        @DisplayName("Escenario 2: Una ADMINISTRADORA autenticada puede registrar otra ADMINISTRADORA y recibe HTTP 201")
        void administradoraPuedeRegistrarOtraAdministradora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ADM-02", "Elena", "Rojas", "ClaveSegura2026!", "ADMINISTRADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(2L, "USR-ADM-02", "Elena", "Rojas", Rol.ADMINISTRADORA, true));

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("admin1").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.codigoUsuario").value("USR-ADM-02"))
                    .andExpect(jsonPath("$.rol").value("ADMINISTRADORA"));

            verify(registrarUsuario, times(1)).ejecutar(any(RegistrarUsuarioCommand.class));
        }

        @Test
        @DisplayName("Escenario 9: Un usuario autenticado con autoridad explícita ROLE_ADMINISTRADORA obtiene acceso autorizado")
        void usuarioConAuthorityRoleAdministradoraObtieneAcceso() throws Exception {
            String jsonRequest = crearJsonRequest("USR-AUT-09", "Lucía", "Pérez", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(9L, "USR-AUT-09", "Lucía", "Pérez", Rol.TRABAJADORA, true));

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("adminExplicit").authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADORA")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(9));

            verify(registrarUsuario, times(1)).ejecutar(any(RegistrarUsuarioCommand.class));
        }

        @Test
        @DisplayName("Escenario 11: La respuesta de una solicitud autorizada no contiene password ni passwordHash")
        void respuestaAutorizadaNoContienePasswordNiHash() throws Exception {
            String jsonRequest = crearJsonRequest("USR-SEC-11", "Ana", "Mendoza", "ClaveSecretaOculta999!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(11L, "USR-SEC-11", "Ana", "Mendoza", Rol.TRABAJADORA, true));

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("admin1").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(content().string(not(containsString("ClaveSecretaOculta999!"))));

            verify(registrarUsuario, times(1)).ejecutar(any(RegistrarUsuarioCommand.class));
        }
    }

    @Nested
    @DisplayName("2. Acceso Denegado por Falta de Privilegios Administrativos (HTTP 403 Forbidden)")
    class AccesoDenegadoTests {

        @Test
        @DisplayName("Escenario 3: Una TRABAJADORA autenticada intenta registrar otra TRABAJADORA y recibe HTTP 403")
        void trabajadoraNoPuedeRegistrarTrabajadora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-TRAB-03", "Beatriz", "Vargas", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("trabajadora1").roles("TRABAJADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(registrarUsuario);
        }

        @Test
        @DisplayName("Escenario 4: Una TRABAJADORA autenticada intenta registrar una ADMINISTRADORA y recibe HTTP 403")
        void trabajadoraNoPuedeRegistrarAdministradora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ADM-04", "Clara", "Salas", "ClaveSegura2026!", "ADMINISTRADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("trabajadora1").roles("TRABAJADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(registrarUsuario);
        }

        @Test
        @DisplayName("Escenario 7: Un usuario autenticado sin ROLE_ADMINISTRADORA recibe HTTP 403")
        void usuarioSinRolAdministradoraRecibe403() throws Exception {
            String jsonRequest = crearJsonRequest("USR-INV-07", "Mario", "Díaz", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("usuarioComun").roles("CLIENTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(registrarUsuario);
        }

        @Test
        @DisplayName("Escenario 8: Un usuario autenticado con autoridad incorrecta ADMINISTRADORA (sin prefijo ROLE_) recibe HTTP 403")
        void usuarioConAuthoritySinPrefijoRoleRecibe403() throws Exception {
            String jsonRequest = crearJsonRequest("USR-NOPRE-08", "Carmen", "Paredes", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("usuarioSinPrefijo").authorities(new SimpleGrantedAuthority("ADMINISTRADORA")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(registrarUsuario);
        }

        @Test
        @DisplayName("Escenario 10: Una solicitud no autorizada con JSON inválido continúa siendo rechazada por seguridad sin ejecutar RegistrarUsuario")
        void solicitudNoAutorizadaConJsonInvalidoEsRechazadaPorSeguridad() throws Exception {
            String jsonInvalido = """
                    {
                      "codigoUsuario": "",
                      "nombres": null,
                      "rol": "INVALIDO"
                    }
                    """;

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .with(user("trabajadora1").roles("TRABAJADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonInvalido))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(registrarUsuario);
        }
    }

    @Nested
    @DisplayName("3. Acceso No Autenticado (HTTP 401 Unauthorized)")
    class AccesoNoAutenticadoTests {

        @Test
        @DisplayName("Escenario 5: Una solicitud anónima intenta registrar una ADMINISTRADORA y recibe HTTP 401")
        void anonimoNoPuedeRegistrarAdministradora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ADM-05", "Sonia", "Castro", "ClaveSegura2026!", "ADMINISTRADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(registrarUsuario);
        }

        @Test
        @DisplayName("Escenario 6: Una solicitud anónima intenta registrar una TRABAJADORA y recibe HTTP 401")
        void anonimoNoPuedeRegistrarTrabajadora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-TRAB-06", "Patricia", "Navarro", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(USUARIOS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(registrarUsuario);
        }
    }

    @Nested
    @DisplayName("4. Rutas Públicas de Diagnóstico y Salud")
    class RutasPublicasTests {

        @Test
        @DisplayName("Escenario 12: La ruta pública GET /api/v1/health continúa accesible sin autenticación y conserva su comportamiento")
        void rutaHealthPermanecePublicaSinAutenticacion() throws Exception {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.servicio").value("farmacia-backend"));

            verifyNoInteractions(registrarUsuario);
        }
    }
}

