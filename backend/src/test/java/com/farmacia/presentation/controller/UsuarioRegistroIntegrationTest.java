package com.farmacia.presentation.controller;

import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de extremo a extremo para la API REST del registro de usuarios.
 * Integra todos los componentes reales de producción:
 * - Spring Security FilterChain (autorización RBAC por rol).
 * - UsuarioController y UsuarioExceptionHandler.
 * - Deserialización y validación estricta de RegistrarUsuarioRequest.
 * - Bean de caso de uso RegistrarUsuario.
 * - Adaptador criptográfico BCryptPasswordHasher (costo 10).
 * - Adaptador de persistencia UsuarioRepositoryAdapter y UsuarioJpaRepository.
 * - Base de datos real SQL Server 2022 ('FarmaciaDB_HU01_Test').
 *
 * Estas pruebas se ejecutan exclusivamente cuando HU01_INTEGRATION_TESTS=true.
 * Todas las transacciones se revierten automáticamente (@Transactional) para mantener la base limpia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FarmaciaDB_HU01_Test;encrypt=true;trustServerCertificate=true;",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
@EnabledIfEnvironmentVariable(named = "HU01_INTEGRATION_TESTS", matches = "true")
@DisplayName("Pruebas de Integración E2E: API REST + BCrypt + SQL Server (HU-01 / SCRUM-89)")
public class UsuarioRegistroIntegrationTest {

    private static final String ENDPOINT = "/api/v1/usuarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordVerifier = new BCryptPasswordEncoder();

    /**
     * Lista sincronizada de códigos generados para verificar su rollback tras la transacción.
     */
    private final List<String> codigosUtilizados = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void verificarAislamientoSeguroTest() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String dbActual = conn.getCatalog();
            if (!"FarmaciaDB_HU01_Test".equalsIgnoreCase(dbActual)) {
                throw new IllegalStateException("PELIGRO DE AISLAMIENTO: La prueba no apunta a FarmaciaDB_HU01_Test. Base: " + dbActual);
            }
        }
        // Confirmar existencia y accesibilidad de la tabla usuarios en SQL Server
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        assertThat(count).isNotNull();
    }

    @AfterTransaction
    void verificarLimpiezaPorRollback() {
        // Confirmar que ningún código generado durante la prueba permaneció en SQL Server tras el rollback
        for (String codigo : codigosUtilizados) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM usuarios WHERE codigo_usuario = ?",
                    Long.class,
                    codigo
            );
            assertThat(count)
                    .as("El código de prueba %s no debe persistir en SQL Server tras el rollback", codigo)
                    .isZero();
        }
        codigosUtilizados.clear();
    }

    // =========================================================================
    // SECCIÓN A: REGISTRO INTEGRAL EXITOSO
    // =========================================================================

    @Nested
    @DisplayName("A. Registro Integral Exitoso")
    class RegistroExitosoTests {

        @Test
        @DisplayName("1. ADMINISTRADORA autenticada registra TRABAJADORA exitosamente en SQL Server")
        void debeRegistrarTrabajadoraExitosamente() throws Exception {
            String codigo = generarCodigoUnico("TRAB");
            String passwordFicticia = "Ficticia2026!Segura";
            String jsonRequest = crearJsonRequest(codigo, "María Lucía", "Peña Ñáñez", passwordFicticia, "TRABAJADORA");

            MvcResult result = mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.codigoUsuario").value(codigo))
                    .andExpect(jsonPath("$.nombres").value("María Lucía"))
                    .andExpect(jsonPath("$.apellidos").value("Peña Ñáñez"))
                    .andExpect(jsonPath("$.rol").value("TRABAJADORA"))
                    .andExpect(jsonPath("$.activo").value(true))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andReturn();

            // Verificar exactamente 6 campos en la respuesta JSON
            JsonNode jsonResponse = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(jsonResponse.size()).isEqualTo(6);
            Long idGenerado = jsonResponse.get("id").asLong();
            assertThat(idGenerado).isPositive();

            // Sincronizar contexto de persistencia para consultar estado real de SQL Server
            entityManager.flush();
            entityManager.clear();

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idGenerado);
            assertThat(usuarioOpt).isPresent();
            Usuario usuarioBD = usuarioOpt.get();
            assertThat(usuarioBD.getCodigoUsuario()).isEqualTo(codigo);
            assertThat(usuarioBD.getNombres()).isEqualTo("María Lucía");
            assertThat(usuarioBD.getApellidos()).isEqualTo("Peña Ñáñez");
            assertThat(usuarioBD.getRol()).isEqualTo(Rol.TRABAJADORA);
            assertThat(usuarioBD.isActivo()).isTrue();
        }

        @Test
        @DisplayName("2. ADMINISTRADORA autenticada registra ADMINISTRADORA exitosamente en SQL Server")
        void debeRegistrarAdministradoraExitosamente() throws Exception {
            String codigo = generarCodigoUnico("ADM2");
            String passwordFicticia = "ClaveAdminFicticia2026!";
            String jsonRequest = crearJsonRequest(codigo, "Valeria Sofía", "Torres Mendoza", passwordFicticia, "ADMINISTRADORA");

            MvcResult result = mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.codigoUsuario").value(codigo))
                    .andExpect(jsonPath("$.rol").value("ADMINISTRADORA"))
                    .andExpect(jsonPath("$.activo").value(true))
                    .andReturn();

            JsonNode jsonResponse = objectMapper.readTree(result.getResponse().getContentAsString());
            Long idGenerado = jsonResponse.get("id").asLong();

            entityManager.flush();
            entityManager.clear();

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idGenerado);
            assertThat(usuarioOpt).isPresent();
            assertThat(usuarioOpt.get().getRol()).isEqualTo(Rol.ADMINISTRADORA);
            assertThat(usuarioOpt.get().isActivo()).isTrue();
        }
    }

    // =========================================================================
    // SECCIÓN B y C: PROTECCIÓN REAL DE CONTRASEÑA EN SQL SERVER
    // =========================================================================

    @Nested
    @DisplayName("B. Protección Real de Contraseña y Verificación BCrypt en SQL Server")
    class ProteccionPasswordTests {

        @Test
        @DisplayName("3. La contraseña se almacena exclusivamente como hash BCrypt con costo 10 y preserva espacios")
        void debeAlmacenarPasswordComoHashBCryptValidoYPreservarEspacios() throws Exception {
            String codigo = generarCodigoUnico("PWD");
            String passwordConEspacios = "  ClaveFicticia_ConEspacios2026!  ";
            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", passwordConEspacios, "TRABAJADORA");

            MvcResult result = mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode jsonResponse = objectMapper.readTree(result.getResponse().getContentAsString());
            Long idGenerado = jsonResponse.get("id").asLong();

            entityManager.flush();
            entityManager.clear();

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idGenerado);
            assertThat(usuarioOpt).isPresent();
            Usuario usuarioBD = usuarioOpt.get();
            String hashPersistido = usuarioBD.getPasswordHash();

            // 1. La contraseña en texto claro nunca se almacena directamente
            assertThat(hashPersistido).isNotEqualTo(passwordConEspacios);

            // 2. Formato BCrypt estándar: prefijo $2a$10$ y longitud 60 caracteres
            assertThat(hashPersistido).startsWith("$2a$10$");
            assertThat(hashPersistido).hasSize(60);

            // 3. Verificación criptográfica con la contraseña exacta
            assertThat(passwordVerifier.matches(passwordConEspacios, hashPersistido)).isTrue();

            // 4. Una contraseña errónea devuelve false
            assertThat(passwordVerifier.matches("PasswordIncorrecta999!", hashPersistido)).isFalse();

            // 5. La contraseña sin sus espacios originales devuelve false (invariante de preservación de espacios)
            assertThat(passwordVerifier.matches(passwordConEspacios.trim(), hashPersistido)).isFalse();
        }
    }

    // =========================================================================
    // SECCIÓN D: CAMPOS OBLIGATORIOS Y VALIDACIONES CON EFECTO REAL EN BD
    // =========================================================================

    @Nested
    @DisplayName("C. Campos Obligatorios y Validaciones Reales sin Inserción en SQL Server")
    class ValidacionesCamposTests {

        @Test
        @DisplayName("4. Código de usuario ausente o vacío retorna HTTP 400 y no inserta fila")
        void debeRetornarHttp400CuandoCodigoEsVacioYSinInsercion() throws Exception {
            long filasAntes = contarTotalUsuarios();
            String jsonRequest = crearJsonRequest("", "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));

            assertThat(contarTotalUsuarios()).isEqualTo(filasAntes);
        }

        @Test
        @DisplayName("5. Nombres vacíos retorna HTTP 400 y no inserta fila")
        void debeRetornarHttp400CuandoNombresSonVaciosYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("VAL-NOM");
            String jsonRequest = crearJsonRequest(codigo, "   ", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("6. Apellidos vacíos retorna HTTP 400 y no inserta fila")
        void debeRetornarHttp400CuandoApellidosSonVaciosYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("VAL-APE");
            String jsonRequest = crearJsonRequest(codigo, "Nombres", "", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("7. Rol desconocido retorna HTTP 400 y no inserta fila")
        void debeRetornarHttp400CuandoRolEsInvalidoYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("VAL-ROL");
            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", "ClaveSegura2026!", "SUPER_ADMIN");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("8. JSON malformado retorna HTTP 400 y no inserta fila")
        void debeRetornarHttp400CuandoJsonEsMalformadoYSinInsercion() throws Exception {
            long filasAntes = contarTotalUsuarios();
            String jsonMalformado = "{ codigoUsuario: malformed json ";

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMalformado))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            assertThat(contarTotalUsuarios()).isEqualTo(filasAntes);
        }
    }

    // =========================================================================
    // SECCIÓN E: LÍMITES DE CONTRASEÑA (8 PUNTOS DE CÓDIGO Y 72 BYTES UTF-8)
    // =========================================================================

    @Nested
    @DisplayName("D. Límites de Contraseña (8 caracteres Unicode y 72 bytes UTF-8)")
    class LimitesPasswordTests {

        @Test
        @DisplayName("9. Contraseña con exactamente 8 puntos de código Unicode es aceptada (HTTP 201)")
        void debeAceptarPasswordDeExactamente8PuntosDeCodigo() throws Exception {
            String codigo = generarCodigoUnico("PW8");
            // Contraseña con exactamente 8 caracteres Unicode (incluyendo tilde)
            String password8Chars = "Clav3_Ñ!";
            assertThat(password8Chars.codePointCount(0, password8Chars.length())).isEqualTo(8);

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", password8Chars, "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber());

            assertThat(usuarioExisteEnBD(codigo)).isTrue();
        }

        @Test
        @DisplayName("10. Contraseña inferior a 8 puntos de código es rechazada (HTTP 400) sin persistencia")
        void debeRechazarPasswordMenorA8PuntosDeCodigo() throws Exception {
            String codigo = generarCodigoUnico("PW7");
            String password7Chars = "Clave1!";
            assertThat(password7Chars.codePointCount(0, password7Chars.length())).isEqualTo(7);

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", password7Chars, "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje").value("La contraseña debe tener al menos 8 caracteres"));

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("11. Contraseña con exactamente 72 bytes UTF-8 es aceptada (HTTP 201) y persiste en SQL Server")
        void debeAceptarPasswordDeExactamente72BytesUtf8() throws Exception {
            String codigo = generarCodigoUnico("PW72B");
            // 72 caracteres ASCII = 72 bytes UTF-8
            String password72Bytes = "P@ssw0rd12345678" + "a".repeat(56);
            assertThat(password72Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", password72Bytes, "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber());

            assertThat(usuarioExisteEnBD(codigo)).isTrue();
        }

        @Test
        @DisplayName("12. Contraseña que excede 72 bytes UTF-8 (73 bytes) es rechazada (HTTP 400) sin persistencia")
        void debeRechazarPasswordQueExcede72BytesUtf8() throws Exception {
            String codigo = generarCodigoUnico("PW73B");
            // 73 caracteres ASCII = 73 bytes UTF-8
            String password73Bytes = "P@ssw0rd12345678" + "a".repeat(57);
            assertThat(password73Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(73);

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", password73Bytes, "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje").value("La contraseña no puede exceder 72 bytes en formato UTF-8"));

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }
    }

    // =========================================================================
    // SECCIÓN F: DUPLICIDAD SECUENCIAL MEDIANTE HTTP
    // =========================================================================

    @Nested
    @DisplayName("E. Duplicidad Secuencial mediante HTTP y Conservación del Registro Original")
    class DuplicidadSecuencialTests {

        @Test
        @DisplayName("13. Segundo registro con código idéntico retorna HTTP 409 Conflict y no altera el usuario original")
        void debeRetornarHttp409YConservarRegistroOriginal() throws Exception {
            String codigoDuplicado = generarCodigoUnico("DUP");

            // 1. Registro inicial exitoso
            String jsonPrimerRegistro = crearJsonRequest(codigoDuplicado, "Nombres Originales", "Apellidos Originales", "ClaveSeguraOriginal2026!", "TRABAJADORA");

            MvcResult primerResult = mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPrimerRegistro))
                    .andExpect(status().isCreated())
                    .andReturn();

            Long idOriginal = objectMapper.readTree(primerResult.getResponse().getContentAsString()).get("id").asLong();

            entityManager.flush();
            entityManager.clear();

            Usuario usuarioOriginalEnBD = usuarioRepository.findById(idOriginal).orElseThrow();
            String hashOriginal = usuarioOriginalEnBD.getPasswordHash();

            // 2. Intento de registro secundario con el mismo código pero datos distintos
            String jsonSegundoRegistro = crearJsonRequest(codigoDuplicado, "Nombres Modificados", "Apellidos Modificados", "OtraClaveDiferente2026!", "ADMINISTRADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonSegundoRegistro))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.mensaje").value("El código de usuario ya se encuentra registrado"))
                    .andExpect(content().string(not(containsString("SQL"))))
                    .andExpect(content().string(not(containsString("SQLException"))))
                    .andExpect(content().string(not(containsString("OtraClaveDiferente2026!"))));

            entityManager.flush();
            entityManager.clear();

            // 3. Confirmar que en SQL Server existe exactamente una fila con ese código
            Long conteo = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM usuarios WHERE codigo_usuario = ?",
                    Long.class,
                    codigoDuplicado
            );
            assertThat(conteo).isEqualTo(1L);

            // 4. Confirmar que los datos del usuario original permanecieron intactos
            Usuario usuarioDespues = usuarioRepository.findById(idOriginal).orElseThrow();
            assertThat(usuarioDespues.getNombres()).isEqualTo("Nombres Originales");
            assertThat(usuarioDespues.getApellidos()).isEqualTo("Apellidos Originales");
            assertThat(usuarioDespues.getRol()).isEqualTo(Rol.TRABAJADORA);
            assertThat(usuarioDespues.getPasswordHash()).isEqualTo(hashOriginal);
            assertThat(usuarioDespues.isActivo()).isTrue();
        }
    }

    // =========================================================================
    // SECCIÓN G: SEGURIDAD CON EFECTOS REALES EN SQL SERVER
    // =========================================================================

    @Nested
    @DisplayName("F. Seguridad y Control de Acceso con Comprobación en SQL Server")
    class SeguridadEfectosRealesTests {

        @Test
        @DisplayName("14. Solicitud anónima retorna HTTP 401 Unauthorized y no inserta en SQL Server")
        void debeRetornarHttp401ParaAnonimoYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("SEC-ANON");
            assertThat(usuarioExisteEnBD(codigo)).isFalse();

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            // Sin post-processor .with(user(...)) -> Anónimo
            mockMvc.perform(post(ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized());

            entityManager.flush();
            entityManager.clear();

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("15. TRABAJADORA intentando registrar TRABAJADORA retorna HTTP 403 Forbidden y no inserta")
        void debeRetornarHttp403ParaTrabajadoraRegistrandoTrabajadoraYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("SEC-TRAB1");
            assertThat(usuarioExisteEnBD(codigo)).isFalse();

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", "ClaveSegura2026!", "TRABAJADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("trabajadora_e2e").roles("TRABAJADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            entityManager.flush();
            entityManager.clear();

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }

        @Test
        @DisplayName("16. TRABAJADORA intentando registrar ADMINISTRADORA retorna HTTP 403 Forbidden y no inserta")
        void debeRetornarHttp403ParaTrabajadoraRegistrandoAdministradoraYSinInsercion() throws Exception {
            String codigo = generarCodigoUnico("SEC-TRAB2");
            assertThat(usuarioExisteEnBD(codigo)).isFalse();

            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", "ClaveSegura2026!", "ADMINISTRADORA");

            mockMvc.perform(post(ENDPOINT)
                            .with(user("trabajadora_e2e").roles("TRABAJADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isForbidden());

            entityManager.flush();
            entityManager.clear();

            assertThat(usuarioExisteEnBD(codigo)).isFalse();
        }
    }

    // =========================================================================
    // SECCIÓN H: SEGURIDAD DE LAS RESPUESTAS HTTP
    // =========================================================================

    @Nested
    @DisplayName("G. Seguridad de las Respuestas HTTP (Ausencia de Datos Sensibles)")
    class SeguridadRespuestasTests {

        @Test
        @DisplayName("17. Respuestas exitosas y de error no exponen contraseñas, hashes, trazas ni sentencias SQL")
        void debeGarantizarAusenciaDeDatosSensiblesEnRespuestas() throws Exception {
            String codigo = generarCodigoUnico("SEC-OUT");
            String passwordFicticia = "ClaveSecretaUnicaFicticia999!";
            String jsonRequest = crearJsonRequest(codigo, "Nombres", "Apellidos", passwordFicticia, "TRABAJADORA");

            // Comprobación en respuesta HTTP 201 Created
            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(not(containsString(passwordFicticia))))
                    .andExpect(content().string(not(containsString("$2a$"))))
                    .andExpect(content().string(not(containsString("passwordHash"))))
                    .andExpect(content().string(not(containsString("password"))));

            // Comprobación en respuesta HTTP 409 Conflict
            mockMvc.perform(post(ENDPOINT)
                            .with(user("admin_e2e").roles("ADMINISTRADORA"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isConflict())
                    .andExpect(content().string(not(containsString(passwordFicticia))))
                    .andExpect(content().string(not(containsString("$2a$"))))
                    .andExpect(content().string(not(containsString("SELECT"))))
                    .andExpect(content().string(not(containsString("INSERT"))))
                    .andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("StackTrace"))));
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private String generarCodigoUnico(String prefijo) {
        String uuidCorto = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String codigo = "E2E-" + prefijo + "-" + uuidCorto;
        // Respetar el límite de 50 caracteres del esquema de SQL Server
        if (codigo.length() > 50) {
            codigo = codigo.substring(0, 50);
        }
        codigosUtilizados.add(codigo);
        return codigo;
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

    private boolean usuarioExisteEnBD(String codigoUsuario) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE codigo_usuario = ?",
                Long.class,
                codigoUsuario
        );
        return count != null && count > 0;
    }

    private long contarTotalUsuarios() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        return count != null ? count : 0L;
    }
}

