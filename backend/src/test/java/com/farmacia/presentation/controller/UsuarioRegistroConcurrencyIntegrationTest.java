package com.farmacia.presentation.controller;

import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración para verificar la concurrencia real y la protección física
 * contra registros duplicados en SQL Server (HU-01 / SCRUM-89 / Etapa 3).
 *
 * IMPORTANTE:
 * - NO se utiliza @Transactional a nivel de clase ni métodos para permitir transacciones
 *   independientes y confirmadas en los hilos concurrentes del ExecutorService.
 * - Limpieza explícita en @AfterEach mediante borrado idempotente por código de usuario.
 * - Incluye dos escenarios:
 *   1. Concurrencia funcional: dos solicitudes simultáneas sin sincronización artificial.
 *   2. Carrera controlada: sincronización determinista que demuestra que ambas peticiones
 *      superan existsByCodigoUsuario=false y colisionan físicamente en la restricción
 *      UK_28jlrohqs9b6tgmygccr7kdb2 de SQL Server, siendo traducida por el adaptador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(UsuarioRegistroConcurrencyIntegrationTest.ConcurrenciaTestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FarmaciaDB_HU01_Test;encrypt=true;trustServerCertificate=true;",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@EnabledIfEnvironmentVariable(named = "HU01_INTEGRATION_TESTS", matches = "true")
@DisplayName("Pruebas de Concurrencia Real del Registro de Usuarios (HU-01 / SCRUM-89 / Etapa 3)")
public class UsuarioRegistroConcurrencyIntegrationTest {

    private static final String ENDPOINT = "/api/v1/usuarios";
    private static final String RESTRICCION_REAL_SQLSERVER = "uk_28jlrohqs9b6tgmygccr7kdb2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ConcurrenciaUsuarioRepositoryDecorador decorador;

    private final BCryptPasswordEncoder passwordVerifier = new BCryptPasswordEncoder();
    private final List<String> codigosParaLimpiar = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void verificarAislamientoYRestriccionSQLServer() throws SQLException {
        // 1. Verificar catálogo exclusivo de pruebas
        try (Connection conn = dataSource.getConnection()) {
            String dbActual = conn.getCatalog();
            if (!"FarmaciaDB_HU01_Test".equalsIgnoreCase(dbActual)) {
                throw new IllegalStateException("PELIGRO DE AISLAMIENTO: Conexión no apunta a FarmaciaDB_HU01_Test. Base: " + dbActual);
            }
        }

        // 2. Verificar existencia de la tabla usuarios
        Long totalUsuarios = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        assertThat(totalUsuarios).isNotNull();

        // 3. Confirmar que la restricción única física existe en SQL Server
        List<String> restricciones = jdbcTemplate.query(
                """
                SELECT kc.name
                FROM sys.key_constraints kc
                JOIN sys.tables t ON kc.parent_object_id = t.object_id
                WHERE t.name = 'usuarios' AND kc.type = 'UQ'
                """,
                (rs, rowNum) -> rs.getString("name").toLowerCase()
        );
        assertThat(restricciones)
                .as("La restricción única real debe existir en FarmaciaDB_HU01_Test")
                .contains(RESTRICCION_REAL_SQLSERVER);

        // 4. Reiniciar estado del decorador
        decorador.reiniciar();
    }

    @AfterEach
    void tearDown() {
        decorador.reiniciar();
        // Limpieza explícita y segura: eliminar exclusivamente los registros generados en las pruebas
        for (String codigo : codigosParaLimpiar) {
            jdbcTemplate.update("DELETE FROM usuarios WHERE codigo_usuario = ?", codigo);
        }
        codigosParaLimpiar.clear();
    }

    // =========================================================================
    // ESCENARIO 1: CONCURRENCIA FUNCIONAL
    // =========================================================================

    @Test
    @DisplayName("1. Concurrencia Funcional: Dos solicitudes simultáneas con el mismo código producen exactamente 1 HTTP 201 y 1 HTTP 409")
    void debeGestionarConcurrenciaFuncionalSimultaneaSinDuplicados() throws Exception {
        String codigoCompartido = generarCodigoUnico("FUNC");

        // Confirmar que el código no existe antes de la prueba
        assertThat(contarUsuariosPorCodigo(codigoCompartido)).isZero();

        String passwordA = "ClaveConcurrenteA2026!";
        String passwordB = "ClaveConcurrenteB2026!";

        String jsonA = crearJsonRequest(codigoCompartido, "Maria Concurrente", "Prueba A", passwordA, "TRABAJADORA");
        String jsonB = crearJsonRequest(codigoCompartido, "Ana Concurrente", "Prueba B", passwordB, "ADMINISTRADORA");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Callable<MvcResult> tareaA = () -> {
                readyLatch.countDown();
                if (!startLatch.await(25, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout esperando startLatch para tarea A");
                }
                // Autenticación administrativa configurada individualmente en la petición MockMvc
                return mockMvc.perform(post(ENDPOINT)
                                .with(user("admin_conc_a").roles("ADMINISTRADORA"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonA))
                        .andReturn();
            };

            Callable<MvcResult> tareaB = () -> {
                readyLatch.countDown();
                if (!startLatch.await(25, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout esperando startLatch para tarea B");
                }
                return mockMvc.perform(post(ENDPOINT)
                                .with(user("admin_conc_b").roles("ADMINISTRADORA"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonB))
                        .andReturn();
            };

            Future<MvcResult> futureA = executor.submit(tareaA);
            Future<MvcResult> futureB = executor.submit(tareaB);

            // Esperar a que ambos hilos estén listos
            assertThat(readyLatch.await(25, TimeUnit.SECONDS))
                    .as("Ambos hilos deben estar preparados antes del disparo")
                    .isTrue();

            // Disparo simultáneo
            startLatch.countDown();

            MvcResult resultA = futureA.get(30, TimeUnit.SECONDS);
            MvcResult resultB = futureB.get(30, TimeUnit.SECONDS);

            int statusA = resultA.getResponse().getStatus();
            int statusB = resultB.getResponse().getStatus();

            List<Integer> statuses = List.of(statusA, statusB);

            // Verificación 1: Exactamente un 201 y un 409
            assertThat(statuses)
                    .as("Debe haber exactamente un HTTP 201 y un HTTP 409")
                    .containsExactlyInAnyOrder(201, 409);

            // Verificación 2: Ningún error HTTP 500
            assertThat(statuses)
                    .as("Ninguna solicitud debe fallar con error interno HTTP 500")
                    .doesNotContain(500);

            MvcResult result201 = (statusA == 201) ? resultA : resultB;
            MvcResult result409 = (statusA == 409) ? resultA : resultB;

            // Verificación 3: Estructura segura de la respuesta HTTP 201
            JsonNode json201 = objectMapper.readTree(result201.getResponse().getContentAsString());
            assertThat(json201.get("id").asLong()).isPositive();
            assertThat(json201.get("codigoUsuario").asText()).isEqualTo(codigoCompartido);
            assertThat(json201.has("password")).isFalse();
            assertThat(json201.has("passwordHash")).isFalse();

            // Verificación 4: Estructura segura de la respuesta HTTP 409
            JsonNode json409 = objectMapper.readTree(result409.getResponse().getContentAsString());
            assertThat(json409.get("status").asInt()).isEqualTo(409);
            assertThat(json409.get("mensaje").asText()).contains("digo de usuario ya se encuentra registrado");
            assertThat(result409.getResponse().getContentAsString())
                    .doesNotContain("SQL")
                    .doesNotContain("Exception")
                    .doesNotContain(passwordA)
                    .doesNotContain(passwordB);

            // Verificación 5: Confirmación en SQL Server (exactamente 1 fila)
            assertThat(contarUsuariosPorCodigo(codigoCompartido))
                    .as("SQL Server debe contener exactamente 1 fila con ese código")
                    .isEqualTo(1L);

            // Verificación 6: Los datos persistidos corresponden a la solicitud ganadora
            Usuario usuarioGanador = decorador.findByCodigoUsuario(codigoCompartido).orElseThrow();
            if (json201.get("nombres").asText().equals("Maria Concurrente")) {
                assertThat(usuarioGanador.getNombres()).isEqualTo("Maria Concurrente");
                assertThat(usuarioGanador.getApellidos()).isEqualTo("Prueba A");
                assertThat(usuarioGanador.getRol()).isEqualTo(Rol.TRABAJADORA);
                assertThat(passwordVerifier.matches(passwordA, usuarioGanador.getPasswordHash())).isTrue();
                assertThat(passwordVerifier.matches(passwordB, usuarioGanador.getPasswordHash())).isFalse();
            } else {
                assertThat(usuarioGanador.getNombres()).isEqualTo("Ana Concurrente");
                assertThat(usuarioGanador.getApellidos()).isEqualTo("Prueba B");
                assertThat(usuarioGanador.getRol()).isEqualTo(Rol.ADMINISTRADORA);
                assertThat(passwordVerifier.matches(passwordB, usuarioGanador.getPasswordHash())).isTrue();
                assertThat(passwordVerifier.matches(passwordA, usuarioGanador.getPasswordHash())).isFalse();
            }

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    // =========================================================================
    // ESCENARIO 2: CARRERA CONTROLADA Y PROTECCIÓN FÍSICA EN SQL SERVER
    // =========================================================================

    @Test
    @DisplayName("2. Carrera Controlada: Ambas solicitudes obtienen exists=false y colisionan físicamente en la restricción única de SQL Server")
    void debeForzarCarreraControladaYVerificarColisionFisicaEnSqlServer() throws Exception {
        String codigoCarrera = generarCodigoUnico("RACE");

        // Confirmar ausencia previa
        assertThat(contarUsuariosPorCodigo(codigoCarrera)).isZero();

        // Configurar el decorador para sincronizar deterministamente la carrera
        decorador.prepararCarreraControlada(codigoCarrera);

        String passwordA = "PasswordCarreraA2026!";
        String passwordB = "PasswordCarreraB2026!";

        String jsonA = crearJsonRequest(codigoCarrera, "Carlos Carrera", "Controlada A", passwordA, "TRABAJADORA");
        String jsonB = crearJsonRequest(codigoCarrera, "Diana Carrera", "Controlada B", passwordB, "ADMINISTRADORA");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Callable<MvcResult> tareaA = () -> {
                readyLatch.countDown();
                if (!startLatch.await(25, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout esperando startLatch para tarea A");
                }
                return mockMvc.perform(post(ENDPOINT)
                                .with(user("admin_race_a").roles("ADMINISTRADORA"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonA))
                        .andReturn();
            };

            Callable<MvcResult> tareaB = () -> {
                readyLatch.countDown();
                if (!startLatch.await(25, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout esperando startLatch para tarea B");
                }
                return mockMvc.perform(post(ENDPOINT)
                                .with(user("admin_race_b").roles("ADMINISTRADORA"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonB))
                        .andReturn();
            };

            Future<MvcResult> futureA = executor.submit(tareaA);
            Future<MvcResult> futureB = executor.submit(tareaB);

            assertThat(readyLatch.await(25, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            MvcResult resultA = futureA.get(30, TimeUnit.SECONDS);
            MvcResult resultB = futureB.get(30, TimeUnit.SECONDS);

            int statusA = resultA.getResponse().getStatus();
            int statusB = resultB.getResponse().getStatus();

            // =================================================================
            // EVIDENCIAS OBLIGATORIAS DE LA COLISIÓN FÍSICA
            // =================================================================

            // 1. Dos consultas reales de existencia retornaron false contra SQL Server
            assertThat(decorador.getConteoExistsFalse())
                    .as("Exactamente dos consultas de existencia debieron retornar false")
                    .isEqualTo(2);

            // 2. Ambas solicitudes superaron la barrera e intentaron ejecutar save en el repositorio real
            assertThat(decorador.getConteoIntentosSave())
                    .as("Ambas solicitudes debieron intentar ejecutar save() en el repositorio")
                    .isEqualTo(2);

            // 3. Una inserción fue confirmada exitosamente
            assertThat(decorador.getInsercionesExitosas())
                    .as("Exactamente una inserción debió tener éxito")
                    .isEqualTo(1);

            // 4. La otra fue rechazada por SQL Server y capturada por el adaptador
            assertThat(decorador.getExcepcionesCapturadas())
                    .as("Exactamente una excepción de base de datos debió ser capturada")
                    .hasSize(1);

            Exception excepcionCapturada = decorador.getExcepcionesCapturadas().get(0);

            // 5. El adaptador tradujo la excepción hacia CodigoUsuarioDuplicadoException
            assertThat(excepcionCapturada)
                    .as("La excepción capturada debe ser CodigoUsuarioDuplicadoException de dominio")
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class);

            // 6. Verificar la cadena de causas físicas de SQL Server
            Throwable causa = excepcionCapturada.getCause();
            assertThat(causa)
                    .as("La causa debe provenir de Spring Data JPA (DataIntegrityViolationException)")
                    .isInstanceOf(DataIntegrityViolationException.class);

            // Inspeccionar la causa raíz (SQLException del driver de SQL Server)
            boolean encontroSqlException2627o2601 = false;
            boolean identificoRestriccionReal = false;
            int codigoErrorSqlServer = -1;

            Throwable cursor = causa;
            while (cursor != null) {
                if (cursor instanceof SQLException sqlEx) {
                    codigoErrorSqlServer = sqlEx.getErrorCode();
                    if (codigoErrorSqlServer == 2627 || codigoErrorSqlServer == 2601) {
                        encontroSqlException2627o2601 = true;
                    }
                    String mensaje = sqlEx.getMessage();
                    if (mensaje != null && mensaje.toLowerCase().contains(RESTRICCION_REAL_SQLSERVER)) {
                        identificoRestriccionReal = true;
                    }
                }
                cursor = cursor.getCause();
            }

            assertThat(encontroSqlException2627o2601)
                    .as("Debe provenir del error 2627 o 2601 de SQL Server (código detectado: %d)", codigoErrorSqlServer)
                    .isTrue();

            assertThat(identificoRestriccionReal)
                    .as("El error debe identificar la restricción única física '%s'", RESTRICCION_REAL_SQLSERVER)
                    .isTrue();

            // 7. Resultados HTTP devueltos por la API REST
            List<Integer> statuses = List.of(statusA, statusB);
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(statuses).doesNotContain(500);

            // 8. Confirmación en SQL Server: exactamente 1 fila persistida
            assertThat(contarUsuariosPorCodigo(codigoCarrera))
                    .as("SQL Server debe contener exactamente 1 fila confirmada")
                    .isEqualTo(1L);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private String generarCodigoUnico(String prefijo) {
        String uuidCompleto = UUID.randomUUID().toString();
        // Prefijo + UUID: "E2E-CONC-FUNC-" + 36 chars = 50 chars exactos (máximo esquema SQL Server)
        String codigo = "E2E-C-" + prefijo + "-" + uuidCompleto;
        if (codigo.length() > 50) {
            codigo = codigo.substring(0, 50);
        }
        codigosParaLimpiar.add(codigo);
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

    private long contarUsuariosPorCodigo(String codigoUsuario) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE codigo_usuario = ?",
                Long.class,
                codigoUsuario
        );
        return count != null ? count : 0L;
    }

    // =========================================================================
    // CONFIGURACIÓN Y DECORADOR DE PRUEBA LOCALIZADO
    // =========================================================================

    @TestConfiguration
    public static class ConcurrenciaTestConfig {

        @Bean
        @Primary
        public ConcurrenciaUsuarioRepositoryDecorador decoradorUsuarioRepository(
                @Qualifier("usuarioRepositoryAdapter") UsuarioRepository delegadoReal
        ) {
            return new ConcurrenciaUsuarioRepositoryDecorador(delegadoReal);
        }
    }

    /**
     * Decorador de prueba de {@link UsuarioRepository}.
     * Delega el 100% de las operaciones a {@code usuarioRepositoryAdapter} real de producción,
     * sin fabricar datos ni simular excepciones.
     * En el escenario de carrera controlada, sincroniza mediante un {@link CyclicBarrier}
     * a las solicitudes que ya obtuvieron {@code existsByCodigoUsuario = false} para forzar
     * la colisión determinista en el {@code saveAndFlush} de SQL Server.
     */
    public static class ConcurrenciaUsuarioRepositoryDecorador implements UsuarioRepository {

        private final UsuarioRepository delegado;

        private volatile boolean activoCarrera = false;
        private volatile String codigoActivoCarrera = null;
        private volatile CyclicBarrier barreraSincronizacion = null;

        private final AtomicInteger conteoExistsFalse = new AtomicInteger(0);
        private final AtomicInteger conteoIntentosSave = new AtomicInteger(0);
        private final AtomicInteger insercionesExitosas = new AtomicInteger(0);
        private final List<Exception> excepcionesCapturadas = Collections.synchronizedList(new ArrayList<>());

        public ConcurrenciaUsuarioRepositoryDecorador(UsuarioRepository delegado) {
            this.delegado = delegado;
        }

        public void prepararCarreraControlada(String codigoCarrera) {
            this.reiniciar();
            this.codigoActivoCarrera = codigoCarrera;
            this.barreraSincronizacion = new CyclicBarrier(2);
            this.activoCarrera = true;
        }

        public void reiniciar() {
            this.activoCarrera = false;
            this.codigoActivoCarrera = null;
            this.barreraSincronizacion = null;
            this.conteoExistsFalse.set(0);
            this.conteoIntentosSave.set(0);
            this.insercionesExitosas.set(0);
            this.excepcionesCapturadas.clear();
        }

        @Override
        public boolean existsByCodigoUsuario(String codigoUsuario) {
            // Ejecutar la consulta real en SQL Server
            boolean exists = delegado.existsByCodigoUsuario(codigoUsuario);

            // Si la carrera controlada está activa para este código específico
            if (activoCarrera && codigoUsuario != null && codigoUsuario.equals(codigoActivoCarrera)) {
                if (!exists) {
                    conteoExistsFalse.incrementAndGet();
                    try {
                        // Ambas solicitudes se detienen aquí habiendo confirmado exists=false
                        // y se liberan simultáneamente hacia la etapa de guardado
                        barreraSincronizacion.await(25, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException("Error en barrera de sincronización concurrente", e);
                    }
                }
            }
            return exists;
        }

        @Override
        public Usuario save(Usuario usuario) {
            if (activoCarrera && usuario != null && usuario.getCodigoUsuario().equals(codigoActivoCarrera)) {
                conteoIntentosSave.incrementAndGet();
                try {
                    Usuario guardado = delegado.save(usuario);
                    insercionesExitosas.incrementAndGet();
                    return guardado;
                } catch (Exception ex) {
                    // Capturar la excepción real generada por SQL Server y traducida por el adaptador
                    excepcionesCapturadas.add(ex);
                    // Re-lanzar intacta para no alterar el flujo de producción
                    throw ex;
                }
            }
            return delegado.save(usuario);
        }

        @Override
        public Optional<Usuario> findById(Long id) {
            return delegado.findById(id);
        }

        @Override
        public Optional<Usuario> findByCodigoUsuario(String codigoUsuario) {
            return delegado.findByCodigoUsuario(codigoUsuario);
        }

        public int getConteoExistsFalse() {
            return conteoExistsFalse.get();
        }

        public int getConteoIntentosSave() {
            return conteoIntentosSave.get();
        }

        public int getInsercionesExitosas() {
            return insercionesExitosas.get();
        }

        public List<Exception> getExcepcionesCapturadas() {
            return Collections.unmodifiableList(excepcionesCapturadas);
        }
    }
}

