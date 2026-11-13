package com.farmacia.presentation.dto;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias de RegistrarUsuarioRequest (HU-01 / SCRUM-88)")
class RegistrarUsuarioRequestTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    private static final String CODIGO_VALIDO = "USR-001";
    private static final String NOMBRES_VALIDOS = "María Carmen";
    private static final String APELLIDOS_VALIDOS = "García Núñez";
    private static final String PASSWORD_VALIDA = "ClaveSegura2026!";
    private static final String ROL_VALIDO = "TRABAJADORA";

    @BeforeAll
    static void setUpValidatorAndMapper() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Creación y Conversión hacia RegistrarUsuarioCommand")
    class ConversionAComandoTests {

        @Test
        @DisplayName("1. Creación del DTO con datos válidos")
        void debeCrearDtoConDatosValidos() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    ROL_VALIDO
            );

            assertThat(request.codigoUsuario()).isEqualTo(CODIGO_VALIDO);
            assertThat(request.nombres()).isEqualTo(NOMBRES_VALIDOS);
            assertThat(request.apellidos()).isEqualTo(APELLIDOS_VALIDOS);
            assertThat(request.password()).isEqualTo(PASSWORD_VALIDA);
            assertThat(request.rol()).isEqualTo(ROL_VALIDO);
        }

        @Test
        @DisplayName("2. Conversión correcta hacia RegistrarUsuarioCommand")
        void debeConvertirCorrectamenteHaciaRegistrarUsuarioCommand() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    "TRABAJADORA"
            );

            RegistrarUsuarioCommand command = request.toCommand();

            assertThat(command).isNotNull();
            assertThat(command.codigoUsuario()).isEqualTo(CODIGO_VALIDO);
            assertThat(command.nombres()).isEqualTo(NOMBRES_VALIDOS);
            assertThat(command.apellidos()).isEqualTo(APELLIDOS_VALIDOS);
            assertThat(command.password()).isEqualTo(PASSWORD_VALIDA);
            assertThat(command.rol()).isEqualTo(Rol.TRABAJADORA);
        }

        @Test
        @DisplayName("3. Conversión estricta del rol ADMINISTRADORA")
        void debeConvertirRolAdministradora() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    "ADMINISTRADORA"
            );

            RegistrarUsuarioCommand command = request.toCommand();

            assertThat(command).isNotNull();
            assertThat(command.rol()).isEqualTo(Rol.ADMINISTRADORA);
        }

        @Test
        @DisplayName("4. Conversión estricta del rol TRABAJADORA")
        void debeConvertirRolTrabajadora() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    "TRABAJADORA"
            );

            RegistrarUsuarioCommand command = request.toCommand();

            assertThat(command).isNotNull();
            assertThat(command.rol()).isEqualTo(Rol.TRABAJADORA);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "admin", "ADMIN", "1", "0", "SUPERUSER", "FARMACEUTICO", "CAJERO", "administradora", "trabajadora", " ADMINISTRADORA "})
        @DisplayName("5. Rechazo de roles desconocidos, numéricos, vacíos, nulos o mal formateados sin exponer valores en el mensaje de error")
        void debeRechazarRolesDesconocidosNumericosVaciosYNulos(String rolInvalido) {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    rolInvalido
            );

            assertThatThrownBy(request::toCommand)
                    .isInstanceOf(DatosUsuarioInvalidosException.class)
                    .hasMessage("El rol especificado es inválido o no reconocido");
        }

        @Test
        @DisplayName("6. Conservación exacta de mayúsculas, tildes y caracteres especiales Unicode")
        void debeConservarMayusculasTildesYCaracteresEspeciales() {
            String nombresConTildes = "María del Carmen Jesús";
            String apellidosConEnye = "García-Núñez y Peña";

            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    "USR-ExactCase",
                    nombresConTildes,
                    apellidosConEnye,
                    PASSWORD_VALIDA,
                    "ADMINISTRADORA"
            );

            RegistrarUsuarioCommand command = request.toCommand();

            assertThat(command.codigoUsuario()).isEqualTo("USR-ExactCase");
            assertThat(command.nombres()).isEqualTo(nombresConTildes);
            assertThat(command.apellidos()).isEqualTo(apellidosConEnye);
        }

        @Test
        @DisplayName("7. Conservación exacta de contraseña con espacios iniciales y finales sin recortar en capa web")
        void debeConservarPasswordExactaConEspacios() {
            String passwordConEspacios = "   ClaveConEspacios2026!   ";

            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    passwordConEspacios,
                    "TRABAJADORA"
            );

            RegistrarUsuarioCommand command = request.toCommand();

            assertThat(command.password()).isEqualTo(passwordConEspacios);
        }
    }

    @Nested
    @DisplayName("Seguridad, Serialización y Protección de Contraseña")
    class SeguridadYSerializacionTests {

        @Test
        @DisplayName("8. Protección de contraseña en toString() reemplazando valor por [PROTEGIDA]")
        void debeProtegerPasswordEnToString() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    "MiContraseniaSuperSecreta123!",
                    ROL_VALIDO
            );

            String representation = request.toString();

            assertThat(representation).doesNotContain("MiContraseniaSuperSecreta123!");
            assertThat(representation).contains("password=[PROTEGIDA]");
        }

        @Test
        @DisplayName("9. Deserialización correcta de una solicitud JSON hacia RegistrarUsuarioRequest")
        void debeDeserializarCorrectamenteDesdeJson() throws Exception {
            String jsonEntrada = """
                    {
                      "codigoUsuario": "USR-100",
                      "nombres": "Carlos Alberto",
                      "apellidos": "López Díaz",
                      "password": "PasswordWeb2026#",
                      "rol": "TRABAJADORA"
                    }
                    """;

            RegistrarUsuarioRequest request = objectMapper.readValue(jsonEntrada, RegistrarUsuarioRequest.class);

            assertThat(request).isNotNull();
            assertThat(request.codigoUsuario()).isEqualTo("USR-100");
            assertThat(request.nombres()).isEqualTo("Carlos Alberto");
            assertThat(request.apellidos()).isEqualTo("López Díaz");
            assertThat(request.password()).isEqualTo("PasswordWeb2026#");
            assertThat(request.rol()).isEqualTo("TRABAJADORA");
        }

        @Test
        @DisplayName("10. Ausencia de password al serializar el DTO web debido a JsonProperty.Access.WRITE_ONLY")
        void debeOmitirPasswordAlSerializarDtoWebAJson() throws Exception {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    "ClaveNuncaExpuesta999!",
                    ROL_VALIDO
            );

            String jsonSalida = objectMapper.writeValueAsString(request);
            JsonNode rootNode = objectMapper.readTree(jsonSalida);

            assertThat(rootNode.has("password")).isFalse();
            assertThat(jsonSalida).doesNotContain("ClaveNuncaExpuesta999!");
            assertThat(rootNode.has("codigoUsuario")).isTrue();
            assertThat(rootNode.has("nombres")).isTrue();
            assertThat(rootNode.has("apellidos")).isTrue();
            assertThat(rootNode.has("rol")).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones de Transporte con Jakarta Bean Validation")
    class ValidacionesJakartaBeanValidationTests {

        @Test
        @DisplayName("11. Request válido no genera violaciones de restricciones")
        void requestValidoNoDebeGenerarViolaciones() {
            RegistrarUsuarioRequest request = new RegistrarUsuarioRequest(
                    CODIGO_VALIDO,
                    NOMBRES_VALIDOS,
                    APELLIDOS_VALIDOS,
                    PASSWORD_VALIDA,
                    ROL_VALIDO
            );

            Set<ConstraintViolation<RegistrarUsuarioRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("12. Rechazo de campos obligatorios nulos, vacíos o en blanco")
        void debeDetectarViolacionesCuandoCamposSonNulosOEnBlanco() {
            RegistrarUsuarioRequest requestInvalido = new RegistrarUsuarioRequest(
                    "   ", // Blanco
                    "",    // Vacío
                    null,  // Nulo
                    "   ", // Blanco
                    null   // Nulo
            );

            Set<ConstraintViolation<RegistrarUsuarioRequest>> violations = validator.validate(requestInvalido);

            assertThat(violations).hasSize(5);
            assertThat(violations)
                    .extracting(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .containsExactlyInAnyOrder("codigoUsuario", "nombres", "apellidos", "password", "rol");
        }
    }

    @Nested
    @DisplayName("Deserialización Estricta sin Coerción de Tipos (Sección 6)")
    class DeserializacionEstrictaTests {

        @Test
        @DisplayName("13. Rechazo de campo rol enviado como número (0 o 1)")
        void debeRechazarRolComoNumero() {
            String jsonConCero = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": "PasswordWeb2026#",
                      "rol": 0
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(jsonConCero, RegistrarUsuarioRequest.class))
                    .isInstanceOf(InvalidFormatException.class);

            String jsonConUno = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": "PasswordWeb2026#",
                      "rol": 1
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(jsonConUno, RegistrarUsuarioRequest.class))
                    .isInstanceOf(InvalidFormatException.class);
        }

        @Test
        @DisplayName("14. Rechazo de campo rol enviado como booleano (true o false)")
        void debeRechazarRolComoBooleano() {
            String jsonConTrue = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": "PasswordWeb2026#",
                      "rol": true
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(jsonConTrue, RegistrarUsuarioRequest.class))
                    .isInstanceOf(InvalidFormatException.class);
        }

        @Test
        @DisplayName("15. Rechazo de campo password enviado como número")
        void debeRechazarPasswordComoNumero() {
            String json = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": 12345678,
                      "rol": "TRABAJADORA"
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(json, RegistrarUsuarioRequest.class))
                    .isInstanceOf(InvalidFormatException.class);
        }

        @Test
        @DisplayName("16. Rechazo de campo codigoUsuario enviado como número")
        void debeRechazarCodigoUsuarioComoNumero() {
            String json = """
                    {
                      "codigoUsuario": 100,
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": "PasswordWeb2026#",
                      "rol": "TRABAJADORA"
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(json, RegistrarUsuarioRequest.class))
                    .isInstanceOf(InvalidFormatException.class);
        }

        @Test
        @DisplayName("17. Rechazo de campo de texto enviado como objeto o arreglo JSON")
        void debeRechazarCamposComoObjetoOArreglo() {
            String jsonObjeto = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": {"valor": "María Carmen"},
                      "apellidos": "García Núñez",
                      "password": "PasswordWeb2026#",
                      "rol": "TRABAJADORA"
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(jsonObjeto, RegistrarUsuarioRequest.class))
                    .isInstanceOf(JsonProcessingException.class);

            String jsonArreglo = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": ["García", "Núñez"],
                      "password": "PasswordWeb2026#",
                      "rol": "TRABAJADORA"
                    }
                    """;

            assertThatThrownBy(() -> objectMapper.readValue(jsonArreglo, RegistrarUsuarioRequest.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("18. Conservación de espacios exactos en contraseña enviada como cadena")
        void debeConservarPasswordExactaConEspaciosAlDeserializar() throws Exception {
            String json = """
                    {
                      "codigoUsuario": "USR-001",
                      "nombres": "María Carmen",
                      "apellidos": "García Núñez",
                      "password": "   ClaveConEspacios2026!   ",
                      "rol": "TRABAJADORA"
                    }
                    """;

            RegistrarUsuarioRequest request = objectMapper.readValue(json, RegistrarUsuarioRequest.class);

            assertThat(request.password()).isEqualTo("   ClaveConEspacios2026!   ");
        }
    }
}
