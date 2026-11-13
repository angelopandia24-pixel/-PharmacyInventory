package com.farmacia.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias del Adaptador BCryptPasswordHasher (HU-01 / SCRUM-87)")
class BCryptPasswordHasherTest {

    private BCryptPasswordHasher passwordHasher;
    private BCryptPasswordEncoder verifierEncoder;

    private static final String PASSWORD_PRUEBA = "FarmaciaSegura2026!";

    @BeforeEach
    void setUp() {
        passwordHasher = new BCryptPasswordHasher();
        verifierEncoder = new BCryptPasswordEncoder();
    }

    @Nested
    @DisplayName("Generación y Formato de Hashes BCrypt")
    class GeneracionYFormatoTests {

        @Test
        @DisplayName("1. Debe generar un hash BCrypt no nulo y no vacío")
        void debeGenerarHashBCryptValido() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(hash).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("2. El hash generado debe ser diferente de la contraseña original en texto plano")
        void debeGenerarHashDiferenteDePasswordOriginal() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(hash).isNotEqualTo(PASSWORD_PRUEBA);
        }

        @Test
        @DisplayName("3. El hash debe cumplir el formato estándar de BCrypt (longitud 60 y prefijo $2a$)")
        void debeTenerFormatoBCryptEsperado() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(hash).hasSize(60);
            assertThat(hash).matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
        }

        @Test
        @DisplayName("4. El factor de costo configurado debe ser exactamente 10")
        void debeUtilizarFactorDeCosto10() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(hash).startsWith("$2a$10$");
        }
    }

    @Nested
    @DisplayName("Verificación Criptográfica y Comprobación con BCryptPasswordEncoder")
    class VerificacionCriptograficaTests {

        @Test
        @DisplayName("5. BCryptPasswordEncoder.matches debe confirmar que el hash corresponde a la contraseña original")
        void debeCoincidirConPasswordOriginalUsandoBCryptPasswordEncoder() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(verifierEncoder.matches(PASSWORD_PRUEBA, hash)).isTrue();
        }

        @Test
        @DisplayName("6. Una contraseña diferente no debe coincidir con el hash generado")
        void noDebeCoincidirConPasswordDiferente() {
            String hash = passwordHasher.hash(PASSWORD_PRUEBA);
            String passwordErronea = "OtraPasswordCompletamenteDistinta999";

            assertThat(verifierEncoder.matches(passwordErronea, hash)).isFalse();
        }

        @Test
        @DisplayName("7. Dos hashes generados para la misma contraseña deben ser diferentes debido al salt aleatorio")
        void debeGenerarHashesDiferentesParaMismaPasswordPorSaltAleatorio() {
            String hash1 = passwordHasher.hash(PASSWORD_PRUEBA);
            String hash2 = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("8. Ambos hashes diferentes deben validar exitosamente con la misma contraseña original")
        void debenCoincidirAmbosHashesDiferentesConMismaPassword() {
            String hash1 = passwordHasher.hash(PASSWORD_PRUEBA);
            String hash2 = passwordHasher.hash(PASSWORD_PRUEBA);

            assertThat(verifierEncoder.matches(PASSWORD_PRUEBA, hash1)).isTrue();
            assertThat(verifierEncoder.matches(PASSWORD_PRUEBA, hash2)).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones de Entrada, Límites y Preservación")
    class ValidacionesEntradaTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", "\r\n"})
        @DisplayName("9. Debe rechazar contraseñas nulas, vacías o compuestas únicamente por espacios")
        void debeRechazarPasswordNulaVaciaOConSoloEspacios(String rawPasswordInvalida) {
            assertThatThrownBy(() -> passwordHasher.hash(rawPasswordInvalida))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La contraseña no puede ser nula, vacía ni contener solo espacios");
        }

        @Test
        @DisplayName("10. Debe rechazar contraseñas que superen los 72 bytes en formato UTF-8 (límite de corte de BCrypt)")
        void debeRechazarPasswordSuperiorA72BytesUtf8() {
            // 73 caracteres ASCII de 1 byte cada uno = 73 bytes
            String password73Bytes = "A".repeat(73);
            assertThat(password73Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(73);

            assertThatThrownBy(() -> passwordHasher.hash(password73Bytes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La contraseña no puede exceder 72 bytes en formato UTF-8");
        }

        @Test
        @DisplayName("11. Debe aceptar contraseñas en el límite exacto de 72 bytes en formato UTF-8")
        void debeAceptarPasswordEnLimiteExactoDe72BytesUtf8() {
            // 36 caracteres 'ñ' ocupan exactamente 72 bytes en UTF-8 (2 bytes por carácter)
            String password72Bytes = "ñ".repeat(36);
            assertThat(password72Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);

            String hash = passwordHasher.hash(password72Bytes);

            assertThat(hash).isNotNull().hasSize(60);
            assertThat(verifierEncoder.matches(password72Bytes, hash)).isTrue();
        }

        @Test
        @DisplayName("12. Debe conservar intactos los espacios iniciales y finales intencionados en la contraseña")
        void debeConservarPasswordConEspaciosInicialesOFinales() {
            String passwordConEspacios = "   ClaveConEspaciosDeliberados2026   ";

            String hash = passwordHasher.hash(passwordConEspacios);

            // Coincide con la contraseña exacta con espacios
            assertThat(verifierEncoder.matches(passwordConEspacios, hash)).isTrue();
            // No coincide con la versión recortada
            assertThat(verifierEncoder.matches(passwordConEspacios.trim(), hash)).isFalse();
        }

        @Test
        @DisplayName("13. Debe conservar contraseñas con caracteres Unicode, tildes, eñes y caracteres especiales")
        void debeConservarPasswordConCaracteresUnicodeTildesYSimbolos() {
            String passwordEspecial = "F@rm4cia_Ñandú_2026#¡!";

            String hash = passwordHasher.hash(passwordEspecial);

            assertThat(verifierEncoder.matches(passwordEspecial, hash)).isTrue();
        }
    }
}

