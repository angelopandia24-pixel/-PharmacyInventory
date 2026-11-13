package com.farmacia.infrastructure.persistence.adapter;

import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import com.farmacia.infrastructure.persistence.mapper.UsuarioEntityMapper;
import com.farmacia.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de UsuarioRepositoryAdapter (HU-01 / SCRUM-87)")
class UsuarioRepositoryAdapterTest {

    @Mock
    private UsuarioJpaRepository usuarioJpaRepository;

    @Mock
    private UsuarioEntityMapper usuarioEntityMapper;

    @InjectMocks
    private UsuarioRepositoryAdapter usuarioRepositoryAdapter;

    private final String codigoValido = "USR-001";
    private final String passwordHash = "$2a$10$hashEjemplo1234567890123456789012345678901234567890";

    @Nested
    @DisplayName("Operaciones Básicas y Consultas")
    class OperacionesBasicasTests {

        @Test
        @DisplayName("Debe persistir un usuario nuevo y retornar la entidad con el id generado usando saveAndFlush")
        void debeGuardarUsuarioNuevoYRetornarConIdGenerado() {
            Usuario usuarioEntrada = new Usuario(
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA
            );
            UsuarioEntity entitySinId = new UsuarioEntity(
                    null,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );
            UsuarioEntity entityGuardada = new UsuarioEntity(
                    1L,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );
            Usuario usuarioGuardado = new Usuario(
                    1L,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );

            when(usuarioEntityMapper.toEntity(usuarioEntrada)).thenReturn(entitySinId);
            when(usuarioJpaRepository.saveAndFlush(entitySinId)).thenReturn(entityGuardada);
            when(usuarioEntityMapper.toDomain(entityGuardada)).thenReturn(usuarioGuardado);

            Usuario resultado = usuarioRepositoryAdapter.save(usuarioEntrada);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getCodigoUsuario()).isEqualTo(codigoValido);
            verify(usuarioJpaRepository).saveAndFlush(entitySinId);
        }

        @Test
        @DisplayName("Debe consultar un usuario existente mediante su id")
        void debeConsultarUsuarioPorIdCuandoExiste() {
            Long id = 1L;
            UsuarioEntity entity = new UsuarioEntity(
                    id,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );
            Usuario usuario = new Usuario(
                    id,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );

            when(usuarioJpaRepository.findById(id)).thenReturn(Optional.of(entity));
            when(usuarioEntityMapper.toDomain(entity)).thenReturn(usuario);

            Optional<Usuario> resultado = usuarioRepositoryAdapter.findById(id);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(id);
            verify(usuarioJpaRepository).findById(id);
        }

        @Test
        @DisplayName("Debe retornar Optional.empty() cuando un usuario no existe por id")
        void debeRetornarOptionalVacioCuandoNoExistePorId() {
            Long idInexistente = 999L;
            when(usuarioJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

            Optional<Usuario> resultado = usuarioRepositoryAdapter.findById(idInexistente);

            assertThat(resultado).isEmpty();
            verify(usuarioJpaRepository).findById(idInexistente);
        }

        @Test
        @DisplayName("Debe consultar un usuario existente mediante su código de usuario")
        void debeConsultarUsuarioPorCodigoCuandoExiste() {
            UsuarioEntity entity = new UsuarioEntity(
                    1L,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );
            Usuario usuario = new Usuario(
                    1L,
                    codigoValido,
                    "María Elena",
                    "Quispe Flores",
                    passwordHash,
                    Rol.ADMINISTRADORA,
                    true
            );

            when(usuarioJpaRepository.findByCodigoUsuario(codigoValido)).thenReturn(Optional.of(entity));
            when(usuarioEntityMapper.toDomain(entity)).thenReturn(usuario);

            Optional<Usuario> resultado = usuarioRepositoryAdapter.findByCodigoUsuario(codigoValido);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCodigoUsuario()).isEqualTo(codigoValido);
            verify(usuarioJpaRepository).findByCodigoUsuario(codigoValido);
        }

        @Test
        @DisplayName("Debe retornar Optional.empty() cuando un usuario no existe por su código")
        void debeRetornarOptionalVacioCuandoNoExistePorCodigo() {
            String codigoInexistente = "USR-999";
            when(usuarioJpaRepository.findByCodigoUsuario(codigoInexistente)).thenReturn(Optional.empty());

            Optional<Usuario> resultado = usuarioRepositoryAdapter.findByCodigoUsuario(codigoInexistente);

            assertThat(resultado).isEmpty();
            verify(usuarioJpaRepository).findByCodigoUsuario(codigoInexistente);
        }

        @Test
        @DisplayName("Debe retornar true cuando el código de usuario ya existe en la persistencia")
        void debeRetornarTrueCuandoCodigoExiste() {
            when(usuarioJpaRepository.existsByCodigoUsuario(codigoValido)).thenReturn(true);

            boolean existe = usuarioRepositoryAdapter.existsByCodigoUsuario(codigoValido);

            assertThat(existe).isTrue();
            verify(usuarioJpaRepository).existsByCodigoUsuario(codigoValido);
        }

        @Test
        @DisplayName("Debe retornar false cuando el código de usuario no existe en la persistencia")
        void debeRetornarFalseCuandoCodigoNoExiste() {
            String codigoInexistente = "USR-999";
            when(usuarioJpaRepository.existsByCodigoUsuario(codigoInexistente)).thenReturn(false);

            boolean existe = usuarioRepositoryAdapter.existsByCodigoUsuario(codigoInexistente);

            assertThat(existe).isFalse();
            verify(usuarioJpaRepository).existsByCodigoUsuario(codigoInexistente);
        }
    }

    @Nested
    @DisplayName("Manejo y Traducción de Excepciones de Concurrencia y Unicidad SQL Server")
    class ManejoExcepcionesTests {

        @Test
        @DisplayName("1. Debe traducir error SQL 2627 con restricción real UK_28jlrohqs9b6tgmygccr7kdb2 a CodigoUsuarioDuplicadoException conservando la causa")
        void debeTraducirError2627ARestriccionCodigoUsuarioDuplicado() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            SQLException sqlEx = new SQLException(
                    "Violation of UNIQUE KEY constraint 'UK_28jlrohqs9b6tgmygccr7kdb2'. Cannot insert duplicate key in object 'dbo.usuarios'. The duplicate key value is (USR-001).",
                    "23000",
                    2627
            );
            ConstraintViolationException cve = new ConstraintViolationException(
                    "could not execute statement",
                    sqlEx,
                    "UK_28jlrohqs9b6tgmygccr7kdb2"
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Error de integridad de datos", cve);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .hasMessage("El código de usuario '" + codigoValido + "' ya se encuentra registrado en el sistema")
                    .hasCause(dive);
        }

        @Test
        @DisplayName("2. Debe traducir error SQL 2601 asociado a índice único UQ_Usuario_CodigoUsuario debidamente identificado")
        void debeTraducirError2601AIndiceCodigoUsuarioDuplicado() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            SQLException sqlEx = new SQLException(
                    "Cannot insert duplicate key row in object 'dbo.usuarios' with unique index 'UQ_Usuario_CodigoUsuario'. The duplicate key value is (USR-001).",
                    "23000",
                    2601
            );
            ConstraintViolationException cve = new ConstraintViolationException(
                    "could not execute statement",
                    sqlEx,
                    "UQ_Usuario_CodigoUsuario"
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Error de integridad de datos", cve);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .hasMessage("El código de usuario '" + codigoValido + "' ya se encuentra registrado en el sistema")
                    .hasCause(dive);
        }

        @Test
        @DisplayName("3. No debe traducir un error de otra restricción aunque la sentencia SQL contenga la palabra codigo_usuario")
        void noDebeTraducirErrorDeOtraRestriccionAunqueSentenciaSqlContengaCodigoUsuario() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            // Error 2627 en otra restricción 'UK_otra_tabla_ruc', pero el texto del statement SQL menciona 'codigo_usuario'
            SQLException sqlEx = new SQLException(
                    "Violation of UNIQUE KEY constraint 'UK_otra_tabla_ruc'. Cannot insert duplicate key in object 'dbo.proveedores'. Statement: INSERT INTO usuarios (codigo_usuario) VALUES (?)",
                    "23000",
                    2627
            );
            ConstraintViolationException cve = new ConstraintViolationException(
                    "could not execute statement",
                    sqlEx,
                    "UK_otra_tabla_ruc"
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Error en proveedores", cve);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .isEqualTo(dive);
        }

        @Test
        @DisplayName("4. No debe traducir un error cuyo valor duplicado contenga la palabra codigo_usuario o el nombre de nuestra restricción")
        void noDebeTraducirErrorCuyoValorDuplicadoContengaCodigoUsuarioONombreDeRestriccion() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            // Error 2627 en 'UK_roles_nombre', pero el valor duplicado insertado es 'codigo_usuario' o 'UK_28jlrohqs9b6tgmygccr7kdb2'
            SQLException sqlEx = new SQLException(
                    "Violation of UNIQUE KEY constraint 'UK_roles_nombre'. Cannot insert duplicate key in object 'dbo.roles'. The duplicate key value is (codigo_usuario, UK_28jlrohqs9b6tgmygccr7kdb2).",
                    "23000",
                    2627
            );
            ConstraintViolationException cve = new ConstraintViolationException(
                    "could not execute statement",
                    sqlEx,
                    "UK_roles_nombre"
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Error en roles", cve);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .isEqualTo(dive);
        }

        @Test
        @DisplayName("5. No debe traducir conflictos de clave primaria PK")
        void debePropagarDataIntegrityViolationExceptionCuandoEsConflictoDeClavePrimaria() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            SQLException sqlEx = new SQLException(
                    "Violation of PRIMARY KEY constraint 'PK__usuarios__3213E83FCABE1402'. Cannot insert duplicate key in object 'dbo.usuarios'. The duplicate key value is (1).",
                    "23000",
                    2627
            );
            ConstraintViolationException cve = new ConstraintViolationException(
                    "could not execute statement",
                    sqlEx,
                    "PK__usuarios__3213E83FCABE1402"
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Error de clave primaria", cve);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .isEqualTo(dive);
        }

        @Test
        @DisplayName("6. Debe propagar DataIntegrityViolationException cuando es un error de integridad distinto (ej. NOT NULL o CHECK)")
        void debePropagarDataIntegrityViolationExceptionCuandoNoEsUnicidad() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            SQLException sqlEx = new SQLException(
                    "Cannot insert the value NULL into column 'password_hash', table 'FarmaciaDB.dbo.usuarios'.",
                    "23000",
                    515
            );
            DataIntegrityViolationException dive = new DataIntegrityViolationException("Valor nulo no permitido", sqlEx);

            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(dive);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .isEqualTo(dive);
        }

        @Test
        @DisplayName("7. Debe propagar excepciones genéricas de base de datos o conexión sin alterarlas")
        void debePropagarExcepcionesGenericasDeConexion() {
            Usuario usuario = new Usuario(codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA);
            UsuarioEntity entity = new UsuarioEntity(null, codigoValido, "Ana", "Mendoza", passwordHash, Rol.ADMINISTRADORA, true);

            when(usuarioEntityMapper.toEntity(usuario)).thenReturn(entity);

            RuntimeException conexionError = new RuntimeException("Error de comunicación de socket con SQL Server");
            when(usuarioJpaRepository.saveAndFlush(entity)).thenThrow(conexionError);

            assertThatThrownBy(() -> usuarioRepositoryAdapter.save(usuario))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Error de comunicación de socket con SQL Server");
        }
    }
}
