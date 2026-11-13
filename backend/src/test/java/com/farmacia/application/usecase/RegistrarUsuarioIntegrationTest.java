package com.farmacia.application.usecase;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.infrastructure.security.BCryptPasswordHasher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de extremo a extremo para el caso de uso RegistrarUsuario.
 * Conecta RegistrarUsuario con el adaptador BCrypt real, el adaptador de persistencia real y SQL Server.
 * Se ejecuta exclusivamente en la base de datos de pruebas 'FarmaciaDB_HU01_Test' cuando HU01_INTEGRATION_TESTS=true.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FarmaciaDB_HU01_Test;encrypt=true;trustServerCertificate=true;",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
@EnabledIfEnvironmentVariable(named = "HU01_INTEGRATION_TESTS", matches = "true")
@DisplayName("Pruebas de Integración E2E: RegistrarUsuario + BCrypt + SQL Server (HU-01 / SCRUM-87)")
class RegistrarUsuarioIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordHasher passwordHasher;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    private RegistrarUsuario registrarUsuario;
    private BCryptPasswordEncoder verifierEncoder;

    private static final String PASSWORD_PRUEBA = "Segura_Biofarma2026!";

    @BeforeEach
    void setUp() throws SQLException {
        // Garantizar aislamiento estricto: verificación de conexión exclusiva a base de datos de test
        try (Connection conn = dataSource.getConnection()) {
            String dbActual = conn.getCatalog();
            if (!"FarmaciaDB_HU01_Test".equalsIgnoreCase(dbActual)) {
                throw new IllegalStateException("ALERTA DE SEGURIDAD: Conexión no apunta a FarmaciaDB_HU01_Test. Base: " + dbActual);
            }
        }

        // Instanciar caso de uso con dependencias reales sin configuración Spring contaminante
        this.registrarUsuario = new RegistrarUsuario(usuarioRepository, passwordHasher);
        this.verifierEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("1. Registro exitoso de usuario con ID generado por SQL Server")
    void debeRegistrarUsuarioExitosamenteConIdGeneradoPorSqlServer() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "IT-USR-001",
                "Lucía Gabriela",
                "Morales Quispe",
                PASSWORD_PRUEBA,
                Rol.ADMINISTRADORA
        );

        UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);
        entityManager.flush();

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull().isGreaterThan(0L);
        assertThat(response.codigoUsuario()).isEqualTo("IT-USR-001");
        assertThat(response.nombres()).isEqualTo("Lucía Gabriela");
        assertThat(response.apellidos()).isEqualTo("Morales Quispe");
        assertThat(response.rol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(response.activo()).isTrue();
    }

    @Test
    @DisplayName("2. Persistencia real en SQL Server de un hash BCrypt válido que verifica con la contraseña original")
    void debePersistirHashBCryptValidoEnSqlServer() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "IT-USR-002",
                "Valeria",
                "Huamán Soto",
                PASSWORD_PRUEBA,
                Rol.TRABAJADORA
        );

        UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> usuarioPersistido = usuarioRepository.findById(response.id());
        assertThat(usuarioPersistido).isPresent();

        String hashAlmacenado = usuarioPersistido.get().getPasswordHash();
        assertThat(hashAlmacenado).isNotNull().hasSize(60).startsWith("$2a$10$");
        assertThat(hashAlmacenado).isNotEqualTo(PASSWORD_PRUEBA);

        // Verificación criptográfica con BCrypt: la contraseña original coincide con el hash almacenado en SQL Server
        assertThat(verifierEncoder.matches(PASSWORD_PRUEBA, hashAlmacenado)).isTrue();
        assertThat(verifierEncoder.matches("PasswordErronea", hashAlmacenado)).isFalse();
    }

    @Test
    @DisplayName("3. Rechazo de registro cuando el código de usuario ya se encuentra registrado")
    void debeRechazarRegistroConCodigoUsuarioDuplicado() {
        RegistrarUsuarioCommand command1 = new RegistrarUsuarioCommand(
                "IT-USR-DUP",
                "Camila",
                "Salazar Ramos",
                PASSWORD_PRUEBA,
                Rol.ADMINISTRADORA
        );
        registrarUsuario.ejecutar(command1);
        entityManager.flush();

        RegistrarUsuarioCommand commandDuplicado = new RegistrarUsuarioCommand(
                "IT-USR-DUP",
                "Diana",
                "Palomino Vega",
                PASSWORD_PRUEBA,
                Rol.TRABAJADORA
        );

        assertThatThrownBy(() -> registrarUsuario.ejecutar(commandDuplicado))
                .isInstanceOf(CodigoUsuarioDuplicadoException.class)
                .hasMessage("El código de usuario 'IT-USR-DUP' ya se encuentra registrado en el sistema");
    }

    @Test
    @DisplayName("4. Conservación del rol y estado activo en la base de datos relacional")
    void debeConservarRolYEstadoActivoEnBaseDeDatos() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "IT-USR-ROL",
                "Andrea",
                "Cárdenas Silva",
                PASSWORD_PRUEBA,
                Rol.TRABAJADORA
        );

        UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> encontrado = usuarioRepository.findById(response.id());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getRol()).isEqualTo(Rol.TRABAJADORA);
        assertThat(encontrado.get().isActivo()).isTrue();
    }

    @Test
    @DisplayName("5. El resultado retornado es seguro y no expone contraseña ni hash")
    void debeRetornarResultadoSeguroSinPasswordNiHash() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                "IT-USR-SEG",
                "Rosa",
                "Navarro Pinto",
                PASSWORD_PRUEBA,
                Rol.ADMINISTRADORA
        );

        UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

        assertThat(response).isNotNull();
        // Inspección por reflexión de los componentes de UsuarioRegistradoResponse
        assertThat(UsuarioRegistradoResponse.class.getRecordComponents())
                .noneMatch(rc -> rc.getName().toLowerCase().contains("password") || rc.getName().toLowerCase().contains("hash"));
    }

    @Test
    @DisplayName("6. Rechazo de entradas inválidas sin crear registros en la base de datos")
    void debeRechazarEntradasInvalidasSinCrearRegistros() {
        RegistrarUsuarioCommand commandInvalido = new RegistrarUsuarioCommand(
                "IT-INVALIDO",
                "Nombres",
                "Apellidos",
                "corta", // Menos de 8 caracteres
                Rol.ADMINISTRADORA
        );

        assertThatThrownBy(() -> registrarUsuario.ejecutar(commandInvalido))
                .isInstanceOf(DatosUsuarioInvalidosException.class)
                .hasMessage("La contraseña debe tener al menos 8 caracteres");

        boolean existeEnDb = usuarioRepository.existsByCodigoUsuario("IT-INVALIDO");
        assertThat(existeEnDb).isFalse();
    }
}

