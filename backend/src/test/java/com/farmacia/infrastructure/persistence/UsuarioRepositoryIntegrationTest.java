package com.farmacia.infrastructure.persistence;

import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración automatizadas para la persistencia de usuarios en SQL Server.
 * Se ejecutan exclusivamente cuando la variable HU01_INTEGRATION_TESTS está configurada en true.
 * Conexión exclusiva a la base de datos de pruebas 'FarmaciaDB_HU01_Test'.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FarmaciaDB_HU01_Test;encrypt=true;trustServerCertificate=true;",
        "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
@EnabledIfEnvironmentVariable(named = "HU01_INTEGRATION_TESTS", matches = "true")
class UsuarioRepositoryIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    private final String passwordHashEjemplo = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW";

    @BeforeEach
    void verificarConexionExclusivaTest() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String dbActual = conn.getCatalog();
            if (!"FarmaciaDB_HU01_Test".equalsIgnoreCase(dbActual)) {
                throw new IllegalStateException("PELIGRO: La conexión no apunta a FarmaciaDB_HU01_Test. Base actual: " + dbActual);
            }
        }
    }

    @Test
    @DisplayName("CP-01: Guardar un usuario nuevo y comprobar que SQL Server genera su ID")
    void cp01_guardarUsuarioNuevoYComprobarIdGenerado() {
        Usuario nuevoUsuario = new Usuario(
                "TEST-CP01",
                "Ana",
                "Flores Mendoza",
                passwordHashEjemplo,
                Rol.ADMINISTRADORA
        );

        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        entityManager.flush();

        assertThat(guardado).isNotNull();
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getId()).isGreaterThan(0L);
        assertThat(guardado.getCodigoUsuario()).isEqualTo("TEST-CP01");
        assertThat(guardado.isActivo()).isTrue();
    }

    @Test
    @DisplayName("CP-02: Recuperar un usuario almacenado mediante su ID")
    void cp02_recuperarUsuarioAlmacenadoMedianteId() {
        Usuario usuario = new Usuario(
                "TEST-CP02",
                "Beatriz",
                "Quispe Cárdenas",
                passwordHashEjemplo,
                Rol.TRABAJADORA
        );
        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> encontrado = usuarioRepository.findById(guardado.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(guardado.getId());
        assertThat(encontrado.get().getCodigoUsuario()).isEqualTo("TEST-CP02");
        assertThat(encontrado.get().getNombres()).isEqualTo("Beatriz");
        assertThat(encontrado.get().getApellidos()).isEqualTo("Quispe Cárdenas");
        assertThat(encontrado.get().getRol()).isEqualTo(Rol.TRABAJADORA);
    }

    @Test
    @DisplayName("CP-03: Recuperar un usuario mediante codigoUsuario")
    void cp03_recuperarUsuarioMedianteCodigoUsuario() {
        Usuario usuario = new Usuario(
                "TEST-CP03",
                "Carmen",
                "Rojas Silva",
                passwordHashEjemplo,
                Rol.ADMINISTRADORA
        );
        usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> encontrado = usuarioRepository.findByCodigoUsuario("TEST-CP03");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigoUsuario()).isEqualTo("TEST-CP03");
        assertThat(encontrado.get().getNombres()).isEqualTo("Carmen");
        assertThat(encontrado.get().getRol()).isEqualTo(Rol.ADMINISTRADORA);
    }

    @Test
    @DisplayName("CP-04: Comprobar la existencia de un código registrado y la inexistencia de otro")
    void cp04_comprobarExistenciaDeCodigoRegistradoEInexistente() {
        Usuario usuario = new Usuario(
                "TEST-CP04-REG",
                "Diana",
                "Gutiérrez Vega",
                passwordHashEjemplo,
                Rol.TRABAJADORA
        );
        usuarioRepository.save(usuario);
        entityManager.flush();

        boolean existeRegistrado = usuarioRepository.existsByCodigoUsuario("TEST-CP04-REG");
        boolean existeInexistente = usuarioRepository.existsByCodigoUsuario("TEST-CP04-NOEXISTE");

        assertThat(existeRegistrado).isTrue();
        assertThat(existeInexistente).isFalse();
    }

    @Test
    @DisplayName("CP-05: Verificar que SQL Server rechaza dos usuarios con el mismo codigoUsuario mediante restricción de unicidad")
    void cp05_verificarRechazoPorCodigoDuplicadoEnSqlServer() {
        String codigoDuplicado = "TEST-CP05-DUP";
        Usuario primerUsuario = new Usuario(
                codigoDuplicado,
                "Elena",
                "Paredes Solís",
                passwordHashEjemplo,
                Rol.TRABAJADORA
        );
        usuarioRepository.save(primerUsuario);
        entityManager.flush();

        Usuario segundoUsuarioConMismoCodigo = new Usuario(
                codigoDuplicado,
                "Fabiola",
                "Torres Meza",
                passwordHashEjemplo,
                Rol.ADMINISTRADORA
        );

        assertThatThrownBy(() -> {
            usuarioRepository.save(segundoUsuarioConMismoCodigo);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
          .satisfies(ex -> {
              boolean esViolacionUnicidadSqlServer = false;
              Throwable causa = ex;
              while (causa != null) {
                  if (causa instanceof SQLException sqlEx) {
                      int errorCode = sqlEx.getErrorCode();
                      if (errorCode == 2627 || errorCode == 2601) {
                          esViolacionUnicidadSqlServer = true;
                          break;
                      }
                  }
                  causa = causa.getCause();
              }
              assertThat(esViolacionUnicidadSqlServer)
                      .as("La excepción debe ser originada por error SQL 2627 o 2601 de SQL Server (violación de clave única)")
                      .isTrue();
          });
    }

    @Test
    @DisplayName("CP-06: Verificar que los nombres y apellidos con tildes y caracteres especiales se almacenan y recuperan correctamente")
    void cp06_verificarAlmacenamientoCaracteresEspecialesYTildes() {
        String nombresConTildes = "María José";
        String apellidosConCaracteres = "Ñúñez Peña";
        Usuario usuario = new Usuario(
                "TEST-CP06-TILDE",
                nombresConTildes,
                apellidosConCaracteres,
                passwordHashEjemplo,
                Rol.ADMINISTRADORA
        );

        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> recuperado = usuarioRepository.findById(guardado.getId());

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getNombres()).isEqualTo("María José");
        assertThat(recuperado.get().getApellidos()).isEqualTo("Ñúñez Peña");
    }

    @Test
    @DisplayName("CP-07: Verificar que el rol, estado activo y passwordHash se conservan correctamente")
    void cp07_verificarConservacionDeRolActivoYPasswordHash() {
        Usuario usuario = new Usuario(
                "TEST-CP07-INTEGRITY",
                "Gabriela",
                "Vargas Castillo",
                passwordHashEjemplo,
                Rol.TRABAJADORA
        );

        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> recuperado = usuarioRepository.findById(guardado.getId());

        assertThat(recuperado).isPresent();
        Usuario obtenido = recuperado.get();
        assertThat(obtenido.getRol()).isEqualTo(Rol.TRABAJADORA);
        assertThat(obtenido.isActivo()).isTrue();
        assertThat(obtenido.getPasswordHash()).isEqualTo(passwordHashEjemplo);
    }

    @Test
    @DisplayName("CP-08: Verificar la reconstrucción correcta de la entidad de dominio Usuario desde la base de datos")
    void cp08_verificarReconstruccionEntidadDominioDesdeBaseDeDatos() {
        Usuario usuario = new Usuario(
                "TEST-CP08-DOMAIN",
                "Hilda",
                "Morales Huamán",
                passwordHashEjemplo,
                Rol.ADMINISTRADORA
        );

        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> recuperado = usuarioRepository.findByCodigoUsuario("TEST-CP08-DOMAIN");

        assertThat(recuperado).isPresent();
        Usuario dominio = recuperado.get();
        assertThat(dominio).isInstanceOf(Usuario.class);
        assertThat(dominio.getId()).isEqualTo(guardado.getId());
        assertThat(dominio.getCodigoUsuario()).isEqualTo("TEST-CP08-DOMAIN");
        assertThat(dominio.getNombres()).isEqualTo("Hilda");
        assertThat(dominio.getApellidos()).isEqualTo("Morales Huamán");
        assertThat(dominio.getPasswordHash()).isEqualTo(passwordHashEjemplo);
        assertThat(dominio.getRol()).isEqualTo(Rol.ADMINISTRADORA);
        assertThat(dominio.isActivo()).isTrue();
    }
}
