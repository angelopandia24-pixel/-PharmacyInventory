package com.farmacia.application.usecase;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.application.dto.UsuarioRegistradoResponse;
import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.domain.service.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Caso de Uso RegistrarUsuario (HU-01 / SCRUM-87)")
class RegistrarUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private RegistrarUsuario registrarUsuario;

    private static final String CODIGO_VALIDO = "USR-001";
    private static final String NOMBRES_VALIDOS = "María Carmen";
    private static final String APELLIDOS_VALIDOS = "García Núñez";
    private static final String PASSWORD_VALIDA = "ClaveSegura2026!";
    private static final String HASH_VALIDO = "$2a$10$e8O4U.M6N9JmIq9vD.Gv9uQWJk1k/6o2U3R1.yB1K9H5qN9vD.Gv9";

    @Nested
    @DisplayName("Escenario 1: Flujo Exitoso y Verificaciones de Salida")
    class FlujoExitosoTests {

        @Test
        @DisplayName("1. Debe registrar usuario exitosamente con datos válidos")
        void debeRegistrarUsuarioExitosamenteConDatosValidos() {
            // Arrange
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
                Usuario u = invocation.getArgument(0);
                return new Usuario(100L, u.getCodigoUsuario(), u.getNombres(), u.getApellidos(), u.getPasswordHash(), u.getRol(), u.isActivo());
            });

            // Act
            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.codigoUsuario()).isEqualTo(CODIGO_VALIDO);
            assertThat(response.nombres()).isEqualTo(NOMBRES_VALIDOS);
            assertThat(response.apellidos()).isEqualTo(APELLIDOS_VALIDOS);
            assertThat(response.rol()).isEqualTo(Rol.ADMINISTRADORA);
            assertThat(response.activo()).isTrue();

            verify(usuarioRepository).existsByCodigoUsuario(CODIGO_VALIDO);
            verify(passwordHasher).hash(PASSWORD_VALIDA);
            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("17. Debe registrar usuario tanto con rol ADMINISTRADORA como con rol TRABAJADORA")
        void debeRegistrarUsuarioConRolAdministradoraYTrabajadora() {
            for (Rol rol : Rol.values()) {
                RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                        "USR-" + rol.name(),
                        NOMBRES_VALIDOS,
                        APELLIDOS_VALIDOS,
                        PASSWORD_VALIDA,
                        rol
                );

                when(usuarioRepository.existsByCodigoUsuario(command.codigoUsuario())).thenReturn(false);
                when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
                when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
                    Usuario u = invocation.getArgument(0);
                    return new Usuario(200L, u.getCodigoUsuario(), u.getNombres(), u.getApellidos(), u.getPasswordHash(), u.getRol(), u.isActivo());
                });

                UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

                assertThat(response).isNotNull();
                assertThat(response.rol()).isEqualTo(rol);
            }
        }

        @Test
        @DisplayName("18. Debe crear entidad Usuario con id nulo y estado activo true antes de persistir")
        void debeCrearUsuarioConIdNuloYEstadoActivo() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.TRABAJADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
                Usuario u = invocation.getArgument(0);
                return new Usuario(1L, u.getCodigoUsuario(), u.getNombres(), u.getApellidos(), u.getPasswordHash(), u.getRol(), u.isActivo());
            });

            registrarUsuario.ejecutar(command);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario usuarioCreado = captor.getValue();

            assertThat(usuarioCreado.getId()).isNull();
            assertThat(usuarioCreado.isActivo()).isTrue();
        }

        @Test
        @DisplayName("19. Debe persistir el hash devuelto por el hasher y nunca la contraseña en texto plano")
        void debePersistirHashYNuncaPasswordOriginal() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.TRABAJADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            registrarUsuario.ejecutar(command);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario usuarioCreado = captor.getValue();

            assertThat(usuarioCreado.getPasswordHash()).isEqualTo(HASH_VALIDO);
            assertThat(usuarioCreado.getPasswordHash()).isNotEqualTo(PASSWORD_VALIDA);
        }

        @Test
        @DisplayName("20. Debe retornar respuesta segura con id generado y datos persistidos sin exponer password ni hash")
        void debeRetornarRespuestaSeguraConIdYDatosDelGuardadoSinPassword() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(
                    new Usuario(555L, CODIGO_VALIDO, NOMBRES_VALIDOS, APELLIDOS_VALIDOS, HASH_VALIDO, Rol.ADMINISTRADORA, true)
            );

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(555L);
            assertThat(response.codigoUsuario()).isEqualTo(CODIGO_VALIDO);
            assertThat(response.nombres()).isEqualTo(NOMBRES_VALIDOS);
            assertThat(response.apellidos()).isEqualTo(APELLIDOS_VALIDOS);
            assertThat(response.rol()).isEqualTo(Rol.ADMINISTRADORA);
            assertThat(response.activo()).isTrue();
            assertThat(UsuarioRegistradoResponse.class.getRecordComponents())
                    .noneMatch(rc -> rc.getName().toLowerCase().contains("password") || rc.getName().toLowerCase().contains("hash"));
        }
    }

    @Nested
    @DisplayName("Escenario 2: Validaciones de Comando y Código de Usuario")
    class ValidacionesComandoYCodigoTests {

        @Test
        @DisplayName("2. Debe rechazar comando nulo con DatosUsuarioInvalidosException")
        void debeRechazarComandoNulo() {
            assertThatThrownBy(() -> registrarUsuario.ejecutar(null))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("El comando de registro no puede ser nulo");

            verify(usuarioRepository, never()).existsByCodigoUsuario(anyString());
            verify(passwordHasher, never()).hash(anyString());
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("3. Debe rechazar codigoUsuario nulo, vacío o con solo espacios")
        void debeRechazarCodigoUsuarioNuloVacioOConEspacios(String codigoInvalido) {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    codigoInvalido,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("El código de usuario es obligatorio y no puede estar vacío");

            verify(usuarioRepository, never()).existsByCodigoUsuario(anyString());
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("4. Debe rechazar codigoUsuario que supera 50 caracteres y aceptar exactamente 50 caracteres")
        void debeRechazarCodigoUsuarioQueSupera50Caracteres() {
            String codigo51 = "U".repeat(51);
            RegistrarUsuarioCommand commandInvalido = new RegistrarUsuarioCommand(
                    codigo51,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(commandInvalido))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("El código de usuario no puede exceder 50 caracteres");

            // Caso límite válido: exactamente 50 caracteres
            String codigo50 = "U".repeat(50);
            RegistrarUsuarioCommand commandValido = new RegistrarUsuarioCommand(
                    codigo50,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );
            when(usuarioRepository.existsByCodigoUsuario(codigo50)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(commandValido);
            assertThat(response.codigoUsuario()).isEqualTo(codigo50);
        }

        @Test
        @DisplayName("14. Debe normalizar codigoUsuario con trim antes de consultar y guardar sin cambiar mayúsculas/minúsculas")
        void debeNormalizarCodigoConTrimAntesDeConsultarYGuardar() {
            String codigoConEspacios = "   USR-ExactCase-01   ";
            String codigoEsperado = "USR-ExactCase-01";

            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    codigoConEspacios,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.TRABAJADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(codigoEsperado)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            registrarUsuario.ejecutar(command);

            verify(usuarioRepository).existsByCodigoUsuario(codigoEsperado);
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getCodigoUsuario()).isEqualTo(codigoEsperado);
        }
    }

    @Nested
    @DisplayName("Escenario 3: Validaciones de Nombres y Apellidos")
    class ValidacionesNombresYApellidosTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("5. Debe rechazar nombres nulos, vacíos o con solo espacios")
        void debeRechazarNombresNulosVaciosOConEspacios(String nombresInvalidos) {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    nombresInvalidos,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("Los nombres son obligatorios y no pueden estar vacíos");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("6. Debe rechazar nombres que superan 100 caracteres y aceptar exactamente 100 caracteres")
        void debeRechazarNombresQueSuperan100Caracteres() {
            String nombres101 = "N".repeat(101);
            RegistrarUsuarioCommand commandInvalido = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    nombres101,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(commandInvalido))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("Los nombres no pueden exceder 100 caracteres");

            // Caso límite válido: exactamente 100 caracteres
            String nombres100 = "N".repeat(100);
            RegistrarUsuarioCommand commandValido = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    nombres100,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );
            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(commandValido);
            assertThat(response.nombres()).isEqualTo(nombres100);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("7. Debe rechazar apellidos nulos, vacíos o con solo espacios")
        void debeRechazarApellidosNulosVaciosOConEspacios(String apellidosInvalidos) {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    apellidosInvalidos,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("Los apellidos son obligatorios y no pueden estar vacíos");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("8. Debe rechazar apellidos que superan 100 caracteres y aceptar exactamente 100 caracteres")
        void debeRechazarApellidosQueSuperan100Caracteres() {
            String apellidos101 = "A".repeat(101);
            RegistrarUsuarioCommand commandInvalido = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    apellidos101,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(commandInvalido))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("Los apellidos no pueden exceder 100 caracteres");

            // Caso límite válido: exactamente 100 caracteres
            String apellidos100 = "A".repeat(100);
            RegistrarUsuarioCommand commandValido = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    apellidos100,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );
            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(commandValido);
            assertThat(response.apellidos()).isEqualTo(apellidos100);
        }

        @Test
        @DisplayName("15. Debe normalizar nombres y apellidos con trim preservando tildes, eñes y caracteres especiales")
        void debeNormalizarNombresYApellidosConTrimPreservandoTildesYEnye() {
            String nombresConEspacios = "   María de los Ángeles   ";
            String apellidosConEspacios = "   Peña-Núñez y Cañas   ";

            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    nombresConEspacios,
                    apellidosConEspacios,
                    PASSWORD_VALIDA,
                    Rol.TRABAJADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            registrarUsuario.ejecutar(command);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario usuarioCreado = captor.getValue();

            assertThat(usuarioCreado.getNombres()).isEqualTo("María de los Ángeles");
            assertThat(usuarioCreado.getApellidos()).isEqualTo("Peña-Núñez y Cañas");
        }
    }

    @Nested
    @DisplayName("Escenario 4: Validaciones de Contraseña y Rol")
    class ValidacionesPasswordYRolTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", "\u2000", "\u3000"})
        @DisplayName("9. Debe rechazar contraseña nula, vacía o con solo espacios (incluyendo espacios Unicode)")
        void debeRechazarPasswordNulaVaciaOConEspacios(String passwordInvalida) {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    passwordInvalida,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("La contraseña es obligatoria y no puede estar vacía");

            verify(passwordHasher, never()).hash(anyString());
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("10. Debe rechazar contraseña con menos de 8 caracteres y aceptar exactamente 8 caracteres")
        void debeRechazarPasswordConMenosDe8Caracteres() {
            RegistrarUsuarioCommand command7 = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    "1234567",
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command7))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("La contraseña debe tener al menos 8 caracteres");

            // Caso límite válido: exactamente 8 caracteres
            RegistrarUsuarioCommand command8 = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    "12345678",
                    Rol.ADMINISTRADORA
            );
            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash("12345678")).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command8);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("10b. Debe rechazar contraseña de 7 caracteres Unicode suplementarios aunque tengan 14 chars UTF-16")
        void debeRechazarPasswordConMenosDe8CodePointsAunqueTengaMasCaracteresUtf16() {
            // 7 emojis suplementarios (cada uno son 2 char en Java, length() = 14, pero codePointCount = 7)
            String password7CodePoints = "😀😁😂😃😄😅😆";
            assertThat(password7CodePoints.length()).isEqualTo(14);
            assertThat(password7CodePoints.codePointCount(0, password7CodePoints.length())).isEqualTo(7);

            RegistrarUsuarioCommand command7CodePoints = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    password7CodePoints,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command7CodePoints))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("La contraseña debe tener al menos 8 caracteres");
        }

        @Test
        @DisplayName("10c. Debe aceptar contraseña de 8 caracteres Unicode válidos que no superen 72 bytes UTF-8")
        void debeAceptarPasswordCon8CodePointsValidosQueNoSuperen72Bytes() {
            // 8 emojis: codePointCount = 8; en UTF-8 ocupan 8 * 4 = 32 bytes <= 72 bytes
            String password8CodePoints = "😀😁😂😃😄😅😆😇";
            assertThat(password8CodePoints.codePointCount(0, password8CodePoints.length())).isEqualTo(8);
            assertThat(password8CodePoints.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(72);

            RegistrarUsuarioCommand command8CodePoints = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    password8CodePoints,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(password8CodePoints)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command8CodePoints);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("11. Debe verificar estrictamente los límites de 72 bytes (aceptado) y 73 bytes (rechazado)")
        void debeVerificarLimitesDe72Y73BytesUtf8() {
            // Límite rechazado: 73 bytes UTF-8
            String password73Bytes = "A".repeat(73);
            assertThat(password73Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(73);

            RegistrarUsuarioCommand command73 = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    password73Bytes,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command73))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("La contraseña no puede exceder 72 bytes en formato UTF-8");

            // Límite aceptado: exactamente 72 bytes UTF-8
            String password72Bytes = "A".repeat(72);
            assertThat(password72Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);

            RegistrarUsuarioCommand command72 = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    password72Bytes,
                    Rol.ADMINISTRADORA
            );
            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(password72Bytes)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioRegistradoResponse response = registrarUsuario.ejecutar(command72);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("12. Debe rechazar rol nulo con DatosUsuarioInvalidosException")
        void debeRechazarRolNulo() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    null
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("El rol es obligatorio y no puede ser nulo");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("16. Debe conservar la contraseña original exacta sin trim al enviarla al hasher (respetando espacios deliberados)")
        void debeConservarPasswordExactaAlInvocarHasherSinTrim() {
            String passwordConEspaciosIntencionales = "   MiClaveConEspacios123   ";

            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    passwordConEspaciosIntencionales,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(passwordConEspaciosIntencionales)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            registrarUsuario.ejecutar(command);

            verify(passwordHasher).hash(passwordConEspaciosIntencionales);
            verify(passwordHasher, never()).hash(passwordConEspaciosIntencionales.trim());
        }
    }

    @Nested
    @DisplayName("Escenario 5: Unicidad, Integridad y Control de Errores")
    class UnicidadYControlErroresTests {

        @Test
        @DisplayName("13. Debe rechazar codigoUsuario duplicado lanzando CodigoUsuarioDuplicadoException")
        void debeRechazarCodigoUsuarioDuplicado() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .hasMessage("El código de usuario '" + CODIGO_VALIDO + "' ya se encuentra registrado en el sistema");

            verify(usuarioRepository).existsByCodigoUsuario(CODIGO_VALIDO);
            verify(passwordHasher, never()).hash(anyString());
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "   ",
                "\t",
                "\n",
                "\r\n",
                " " + HASH_VALIDO,               // Espacio inicial externo
                HASH_VALIDO + " ",               // Espacio final externo
                "\t" + HASH_VALIDO,              // Tabulación inicial externa
                HASH_VALIDO + "\t",              // Tabulación final externa
                "\n" + HASH_VALIDO,              // Salto de línea inicial externo
                HASH_VALIDO + "\n",              // Salto de línea final externo
                "\r\n" + HASH_VALIDO,            // CRLF inicial externo
                HASH_VALIDO + "\r\n",            // CRLF final externo
                "\u2000" + HASH_VALIDO,          // Espacio Unicode inicial externo
                HASH_VALIDO + "\u3000",          // Espacio ideográfico Unicode final externo
                "   " + HASH_VALIDO + "   "      // Espacios en ambos extremos
        })
        @DisplayName("21a. Debe lanzar IllegalStateException y nunca llamar a save si el hasher devuelve nulo, vacío o con caracteres de espacio externos (espacios, tabulaciones, saltos de línea)")
        void debeLanzarIllegalStateExceptionSiHasherDevuelveNuloVacioOConEspaciosExternos(String hashInvalido) {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(hashInvalido);

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Error al generar el hash de la contraseña");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("21b. Debe lanzar IllegalStateException y nunca llamar a save si el hasher devuelve un valor que excede los 255 caracteres")
        void debeLanzarIllegalStateExceptionSiHasherDevuelveValorQueExcede255Caracteres() {
            String hashExcesivo = "$2a$10$" + "x".repeat(251); // 258 caracteres
            assertThat(hashExcesivo.length()).isGreaterThan(255);

            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(hashExcesivo);

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Error al generar el hash de la contraseña");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("22. Debe propagar CodigoUsuarioDuplicadoException si el repositorio detecta concurrencia al guardar")
        void debePropagarCodigoUsuarioDuplicadoExceptionSiRepositorioFallaAlGuardar() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(false);
            when(passwordHasher.hash(PASSWORD_VALIDA)).thenReturn(HASH_VALIDO);
            when(usuarioRepository.save(any(Usuario.class))).thenThrow(CodigoUsuarioDuplicadoException.porCodigo(CODIGO_VALIDO));

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class)
                    .hasMessage("El código de usuario '" + CODIGO_VALIDO + "' ya se encuentra registrado en el sistema");
        }

        @Test
        @DisplayName("23. No debe invocar usuarioRepository.save() cuando falla cualquier validación previa")
        void noDebeInvocarRepositorioSaveCuandoFallaCualquierValidacion() {
            RegistrarUsuarioCommand commandInvalido = new RegistrarUsuarioCommand(
                    "",
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            assertThatThrownBy(() -> registrarUsuario.ejecutar(commandInvalido))
                    .isInstanceOf(DatosUsuarioInvalidosException.class);

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("24. No debe invocar passwordHasher.hash() cuando el código de usuario ya existe")
        void noDebeInvocarPasswordHasherCuandoCodigoUsuarioYaExiste() {
            RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    Rol.ADMINISTRADORA
            );

            when(usuarioRepository.existsByCodigoUsuario(CODIGO_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> registrarUsuario.ejecutar(command))
                    .isInstanceOf(CodigoUsuarioDuplicadoException.class);

            verify(passwordHasher, never()).hash(anyString());
        }
    }
}
