package com.farmacia.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "cambia-esta-clave-en-produccion-minimo-32-caracteres-0123456789");
        ReflectionTestUtils.setField(jwtProvider, "accessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshExpirationMs", 604800000L);
    }

    @Test
    void generarYValidarAccessToken_exitoso() {
        String token = jwtProvider.generateToken("DUENA01", "ADMINISTRADORA");

        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));
        assertEquals("DUENA01", jwtProvider.getUsernameFromToken(token));
        assertEquals("ADMINISTRADORA", jwtProvider.getRolFromToken(token));
        assertFalse(jwtProvider.isRefreshToken(token));
    }

    @Test
    void generarYValidarRefreshToken_exitoso() {
        String refreshToken = jwtProvider.generateRefreshToken("DUENA01");

        assertNotNull(refreshToken);
        assertTrue(jwtProvider.validateToken(refreshToken));
        assertEquals("DUENA01", jwtProvider.getUsernameFromToken(refreshToken));
        assertTrue(jwtProvider.isRefreshToken(refreshToken));
    }

    @Test
    void validarToken_invalidoOManipulado_retornaFalse() {
        assertFalse(jwtProvider.validateToken("token.falso.invalido"));

        String token = jwtProvider.generateToken("DUENA01", "ADMINISTRADORA");
        String tokenAlterado = token.substring(0, token.length() - 4) + "xxxx";
        assertFalse(jwtProvider.validateToken(tokenAlterado));
    }
}