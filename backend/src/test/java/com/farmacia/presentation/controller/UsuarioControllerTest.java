package com.farmacia.presentation.controller;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.application.usecase.RegistrarUsuario;
import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.farmacia.infrastructure.config.SecurityConfig;
import com.farmacia.presentation.advice.UsuarioExceptionHandler;
import com.farmacia.presentation.dto.RegistrarUsuarioRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@ContextConfiguration(classes = {
        UsuarioController.class,
        SecurityConfig.class,
        UsuarioExceptionHandler.class
})
@DisplayName("Pruebas Web de UsuarioController con MockMvc (HU-01 / SCRUM-88)")
class UsuarioControllerTest {

    private static final String ENDPOINT = "/api/v1/usuarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrarUsuario registrarUsuario;

    @Nested
    @DisplayName("Flujos Exitosos de Registro (HTTP 201 Created)")
    @WithMockUser(username = "admin_user", roles = {"ADMINISTRADORA"})
    class FlujosExitososTests {

        @Test
        @DisplayName("1. Registro exitoso con HTTP 201 Created")
        void debeRegistrarUsuarioConHttp201() throws Exception {
            String jsonRequest = crearJsonRequest("USR-001", "María Carmen", "García Núñez", "ClaveSegura2026!", "TRABAJADORA");

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    1L,
                    "USR-001",
                    "María Carmen",
                    "García Núñez",
                    Rol.TRABAJADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)));
        }

        @Test
        @DisplayName("2. Verificación de los seis campos exactos del JSON de respuesta")
        void debeRetornarSeisCamposDelContratoEnRespuesta() throws Exception {
            String jsonRequest = crearJsonRequest("USR-002", "Ana Beatriz", "Quispe Flores", "ClaveSegura2026!", "ADMINISTRADORA");

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    2L,
                    "USR-002",
                    "Ana Beatriz",
                    "Quispe Flores",
                    Rol.ADMINISTRADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.codigoUsuario").value("USR-002"))
                    .andExpect(jsonPath("$.nombres").value("Ana Beatriz"))
                    .andExpect(jsonPath("$.apellidos").value("Quispe Flores"))
                    .andExpect(jsonPath("$.rol").value("ADMINISTRADORA"))
                    .andExpect(jsonPath("$.activo").value(true));
        }

        @Test
        @DisplayName("3. Ausencia total de password y passwordHash en la respuesta HTTP")
        void noDebeRetornarPasswordNiPasswordHash() throws Exception {
            String jsonRequest = crearJsonRequest("USR-003", "Carlos", "Mendoza", "MiClaveSuperSecreta2026!", "TRABAJADORA");

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    3L,
                    "USR-003",
                    "Carlos",
                    "Mendoza",
                    Rol.TRABAJADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(content().string(not(containsString("MiClaveSuperSecreta2026!"))));
        }

        @Test
        @DisplayName("4. Verificación de que se invoca RegistrarUsuario.ejecutar exactamente una vez")
        void debeInvocarCasoDeUsoExactamenteUnaVez() throws Exception {
            String jsonRequest = crearJsonRequest("USR-004", "Rosa", "Paredes", "ClaveSegura2026!", "TRABAJADORA");

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    4L,
                    "USR-004",
                    "Rosa",
                    "Paredes",
                    Rol.TRABAJADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated());

            verify(registrarUsuario, times(1)).ejecutar(any(RegistrarUsuarioCommand.class));
        }

        @Test
        @DisplayName("5. Verificación del contenido de RegistrarUsuarioCommand mediante ArgumentCaptor")
        void debeMapearCorrectamenteElComandoHaciaElCasoDeUso() throws Exception {
            String jsonRequest = crearJsonRequest("USR-005", "Elena", "Rojas", "ClaveSegura2026!", "ADMINISTRADORA");

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    5L,
                    "USR-005",
                    "Elena",
                    "Rojas",
                    Rol.ADMINISTRADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated());

            ArgumentCaptor<RegistrarUsuarioCommand> captor = ArgumentCaptor.forClass(RegistrarUsuarioCommand.class);
            verify(registrarUsuario).ejecutar(captor.capture());

            RegistrarUsuarioCommand commandCapturado = captor.getValue();
            assertThat(commandCapturado.codigoUsuario()).isEqualTo("USR-005");
            assertThat(commandCapturado.nombres()).isEqualTo("Elena");
            assertThat(commandCapturado.apellidos()).isEqualTo("Rojas");
            assertThat(commandCapturado.password()).isEqualTo("ClaveSegura2026!");
            assertThat(commandCapturado.rol()).isEqualTo(Rol.ADMINISTRADORA);
        }

        @Test
        @DisplayName("6. Conservación exacta de contraseña con espacios iniciales y finales en el comando")
        void debeConservarPasswordConEspaciosHaciaElComando() throws Exception {
            String passwordConEspacios = "   ClaveConEspacios2026!   ";
            String jsonEntrada = """
                    {
                      "codigoUsuario": "USR-006",
                      "nombres": "Lucía",
                      "apellidos": "Vargas",
                      "password": "   ClaveConEspacios2026!   ",
                      "rol": "TRABAJADORA"
                    }
                    """;

            UsuarioRegistradoResponse useCaseResponse = new UsuarioRegistradoResponse(
                    6L,
                    "USR-006",
                    "Lucía",
                    "Vargas",
                    Rol.TRABAJADORA,
                    true
            );

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(useCaseResponse);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonEntrada))
                    .andExpect(status().isCreated());

            ArgumentCaptor<RegistrarUsuarioCommand> captor = ArgumentCaptor.forClass(RegistrarUsuarioCommand.class);
            verify(registrarUsuario).ejecutar(captor.capture());

            assertThat(captor.getValue().password()).isEqualTo(passwordConEspacios);
        }

        @Test
        @DisplayName("7. Registro exitoso solicitado para usuario TRABAJADORA")
        void debeRegistrarUsuarioConRolTrabajadora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-TRAB", "Teresa", "Castro", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any())).thenReturn(new UsuarioRegistradoResponse(
                    7L, "USR-TRAB", "Teresa", "Castro", Rol.TRABAJADORA, true
            ));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rol").value("TRABAJADORA"));
        }

        @Test
        @DisplayName("8. Registro exitoso solicitado para usuario ADMINISTRADORA")
        void debeRegistrarUsuarioConRolAdministradora() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ADMIN", "Silvia", "Torres", "ClaveSegura2026!", "ADMINISTRADORA");

            when(registrarUsuario.ejecutar(any())).thenReturn(new UsuarioRegistradoResponse(
                    8L, "USR-ADMIN", "Silvia", "Torres", Rol.ADMINISTRADORA, true
            ));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rol").value("ADMINISTRADORA"));
        }
    }

    @Nested
    @DisplayName("Control de Duplicados y Conflictos (HTTP 409 Conflict)")
    @WithMockUser(username = "admin_user", roles = {"ADMINISTRADORA"})
    class DuplicadosYConflictosTests {

        @Test
        @DisplayName("9. Código duplicado detectado por el caso de uso retorna HTTP 409 con mensaje seguro")
        void debeRetornarHttp409CuandoCodigoUsuarioYaExiste() throws Exception {
            String jsonRequest = crearJsonRequest("USR-DUP", "María Carmen", "García Núñez", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenThrow(CodigoUsuarioDuplicadoException.porCodigo("USR-DUP"));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.mensaje").value("El código de usuario ya se encuentra registrado"))
                    .andExpect(jsonPath("$.ruta").value(ENDPOINT))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Validación de Entrada y Datos Inválidos (HTTP 400 Bad Request)")
    @WithMockUser(username = "admin_user", roles = {"ADMINISTRADORA"})
    class ValidacionesYDatosInvalidosTests {

        @Test
        @DisplayName("10. Campos obligatorios ausentes, nulos o en blanco retornan HTTP 400")
        void debeRetornarHttp400CuandoHayCamposEnBlancoONulos() throws Exception {
            String jsonInvalido = """
                    {
                      "codigoUsuario": "",
                      "nombres": "  ",
                      "apellidos": null,
                      "password": "",
                      "rol": " "
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonInvalido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.mensaje").exists())
                    .andExpect(jsonPath("$.ruta").value(ENDPOINT));

            verify(registrarUsuario, never()).ejecutar(any());
        }

        @Test
        @DisplayName("11. Datos inválidos reportados por el caso de uso retornan HTTP 400 con mensaje seguro")
        void debeRetornarHttp400CuandoCasoDeUsoReportaDatosInvalidos() throws Exception {
            String jsonRequest = crearJsonRequest("CODIGO_INVALIDO", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenThrow(new DatosUsuarioInvalidosException("El código de usuario no cumple con el formato requerido"));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.mensaje").value("El código de usuario no cumple con el formato requerido"))
                    .andExpect(jsonPath("$.ruta").value(ENDPOINT));
        }

        @Test
        @DisplayName("12. Contraseña inválida reportada por el caso de uso retorna HTTP 400")
        void debeRetornarHttp400CuandoPasswordEsInvalidaSegunCasoDeUso() throws Exception {
            String jsonRequest = crearJsonRequest("USR-001", "Nombres", "Apellidos", "corta", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenThrow(new DatosUsuarioInvalidosException("La contraseña debe tener al menos 8 caracteres"));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener al menos 8 caracteres"));
        }

        @Test
        @DisplayName("13. Rol desconocido, numérico o inválido retorna HTTP 400")
        void debeRetornarHttp400CuandoRolEsInvalidoONumerico() throws Exception {
            // Rol desconocido como cadena
            String jsonRolDesconocido = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María",
                      "apellidos": "García",
                      "password": "ClaveSegura2026!",
                      "rol": "SUPERADMIN"
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRolDesconocido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje").value("El rol especificado es inválido o no reconocido"));

            // Rol numérico 0 rechazado por deserialización estricta
            String jsonRolNumero = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María",
                      "apellidos": "García",
                      "password": "ClaveSegura2026!",
                      "rol": 0
                    }
                    """;

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRolNumero))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud contiene datos inválidos o un formato JSON incorrecto"));

            verify(registrarUsuario, never()).ejecutar(any());
        }

        @Test
        @DisplayName("14. Cuerpo JSON malformado o no parseable retorna HTTP 400 sin exponer trazas")
        void debeRetornarHttp400CuandoJsonEsMalformado() throws Exception {
            String jsonMalformado = "{ malformed json: true, ";

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMalformado))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud contiene datos inválidos o un formato JSON incorrecto"))
                    .andExpect(jsonPath("$.ruta").value(ENDPOINT));

            verify(registrarUsuario, never()).ejecutar(any());
        }

        @Test
        @DisplayName("22. Mensaje desconocido o con datos sensibles en DatosUsuarioInvalidosException es sanitizado con mensaje genérico seguro")
        void debeSanitizarMensajeConDatosSensiblesEnExcepcionDeValidacion() throws Exception {
            String jsonRequest = crearJsonRequest("USR-SENS", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");
            String passwordSensibleFicticia = "ClaveUltraSecretaFicticia999!";

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenThrow(new DatosUsuarioInvalidosException("Fallo en validación con contraseña " + passwordSensibleFicticia));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.mensaje").value("Los datos proporcionados no son válidos"))
                    .andExpect(content().string(not(containsString(passwordSensibleFicticia))));
        }
    }

    @Nested
    @DisplayName("Formatos de Medios y Errores de Servidor (HTTP 415 y HTTP 500)")
    @WithMockUser(username = "admin_user", roles = {"ADMINISTRADORA"})
    class FormatosYErroresServidorTests {

        @Test
        @DisplayName("15. Formato de medios no soportado retorna HTTP 415")
        void debeRetornarHttp415CuandoContentTypeNoEsJson() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("texto plano"))
                    .andExpect(status().isUnsupportedMediaType());

            verify(registrarUsuario, never()).ejecutar(any());
        }

        @Test
        @DisplayName("16. Error inesperado del caso de uso retorna HTTP 500 controlado sin exponer detalles técnicos")
        void debeRetornarHttp500CuandoOcurreErrorInesperado() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ERR", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenThrow(new IllegalStateException("Error de infraestructura crítico en el servicio criptográfico"));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"))
                    .andExpect(content().string(not(containsString("Error de infraestructura crítico"))))
                    .andExpect(content().string(not(containsString("IllegalStateException"))));
        }

        @Test
        @DisplayName("18. Retorno nulo del caso de uso retorna HTTP 500 controlado")
        void debeRetornarHttp500CuandoCasoDeUsoRetornaNull() throws Exception {
            String jsonRequest = crearJsonRequest("USR-NUL", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class))).thenReturn(null);

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"));
        }

        @Test
        @DisplayName("19. Retorno con ID nulo del caso de uso retorna HTTP 500 controlado")
        void debeRetornarHttp500CuandoCasoDeUsoRetornaIdNulo() throws Exception {
            String jsonRequest = crearJsonRequest("USR-NULID", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(null, "USR-NULID", "Nombres", "Apellidos", Rol.TRABAJADORA, true));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"));
        }

        @Test
        @DisplayName("20. Retorno con ID cero del caso de uso retorna HTTP 500 controlado")
        void debeRetornarHttp500CuandoCasoDeUsoRetornaIdCero() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ZERO", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(0L, "USR-ZERO", "Nombres", "Apellidos", Rol.TRABAJADORA, true));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"));
        }

        @Test
        @DisplayName("21. Retorno con ID negativo del caso de uso retorna HTTP 500 controlado")
        void debeRetornarHttp500CuandoCasoDeUsoRetornaIdNegativo() throws Exception {
            String jsonRequest = crearJsonRequest("USR-NEG", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            when(registrarUsuario.ejecutar(any(RegistrarUsuarioCommand.class)))
                    .thenReturn(new UsuarioRegistradoResponse(-1L, "USR-NEG", "Nombres", "Apellidos", Rol.TRABAJADORA, true));

            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"));
        }

        @Test
        @DisplayName("23. Solicitud con método HTTP no admitido retorna HTTP 405 Method Not Allowed")
        void debeRetornarHttp405CuandoMetodoNoEsPost() throws Exception {
            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isMethodNotAllowed());

            verify(registrarUsuario, never()).ejecutar(any());
        }

        @Test
        @DisplayName("24. Solicitud con encabezado Accept incompatible retorna HTTP 406 Not Acceptable")
        void debeRetornarHttp406CuandoAcceptEsIncompatible() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ACC", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .accept(MediaType.APPLICATION_XML)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isNotAcceptable());

            verify(registrarUsuario, never()).ejecutar(any());
        }
    }

    @Nested
    @DisplayName("Seguridad y Autenticación (HTTP 401 Unauthorized)")
    class SeguridadYAutenticacionTests {

        @Test
        @DisplayName("17. Solicitud no autenticada retorna HTTP 401 Unauthorized gestionado por Spring Security")
        void debeRetornarHttp401CuandoNoHayAutenticacion() throws Exception {
            String jsonRequest = crearJsonRequest("USR-ANON", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            // Sin anotación @WithMockUser ni credenciales en la petición
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized());

            verify(registrarUsuario, never()).ejecutar(any());
        }
    }

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
}
