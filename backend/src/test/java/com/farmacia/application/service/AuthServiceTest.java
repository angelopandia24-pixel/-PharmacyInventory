package com.farmacia.application.service;

import com.farmacia.application.dto.AuthResponse;
import com.farmacia.application.dto.LoginRequest;
import com.farmacia.application.dto.RefreshRequest;
import com.farmacia.domain.exception.AuthException;
import com.farmacia.domain.repository.TokenBlacklistPort;
import com.farmacia.domain.repository.UsuarioAuthPort;
import com.farmacia.domain.repository.UsuarioAuthPort.UsuarioDatos;
import com.farmacia.infrastructure.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioAuthPort usuarioAuthPort;

    @Mock
    private TokenBlacklistPort blacklistPort;

    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "cambia-esta-clave-en-produccion-minimo-32-caracteres-0123456789");
        ReflectionTestUtils.setField(jwtProvider, "accessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshExpirationMs", 604800000L);
        authService = new AuthService(usuarioAuthPort, passwordEncoder, jwtProvider, blacklistPort);
    }

    @Test
    void login_credencialesValidas_retornaTokensYDatos() {
        String hash = passwordEncoder.encode("12345678");
        UsuarioDatos usuario = new UsuarioDatos("DUENA01", hash, "Dueña Biofarma", "ADMINISTRADORA", true);
        when(usuarioAuthPort.buscarPorCodigoAcceso("DUENA01")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setCodigoAcceso("DUENA01");
        request.setContrasena("12345678");

        AuthResponse response = authService.login(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Dueña Biofarma", response.getNombreCompleto());
        assertEquals("ADMINISTRADORA", response.getRol());
    }

    @Test
    void login_contrasenaIncorrecta_lanzaAuthException() {
        String hash = passwordEncoder.encode("12345678");
        UsuarioDatos usuario = new UsuarioDatos("DUENA01", hash, "Dueña Biofarma", "ADMINISTRADORA", true);
        when(usuarioAuthPort.buscarPorCodigoAcceso("DUENA01")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setCodigoAcceso("DUENA01");
        request.setContrasena("claveErronea");

        assertThrows(AuthException.class, () -> authService.login(request));
    }

    @Test
    void login_usuarioInactivo_lanzaAuthException() {
        String hash = passwordEncoder.encode("12345678");
        UsuarioDatos usuario = new UsuarioDatos("TRAB01", hash, "Trabajadora Inactiva", "TRABAJADORA", false);
        when(usuarioAuthPort.buscarPorCodigoAcceso("TRAB01")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest();
        request.setCodigoAcceso("TRAB01");
        request.setContrasena("12345678");

        assertThrows(AuthException.class, () -> authService.login(request));
    }

    @Test
    void refresh_tokenValido_revocaAnteriorYGeneraNuevosTokens() {
        String refreshToken = jwtProvider.generateRefreshToken("DUENA01");
        UsuarioDatos usuario = new UsuarioDatos("DUENA01", "hash", "Dueña Biofarma", "ADMINISTRADORA", true);

        when(blacklistPort.estaRevocado(refreshToken)).thenReturn(false);
        when(usuarioAuthPort.buscarPorCodigoAcceso("DUENA01")).thenReturn(Optional.of(usuario));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        AuthResponse response = authService.refresh(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(blacklistPort).revocarToken(refreshToken);
    }

    @Test
    void refresh_tokenRevocado_lanzaAuthException() {
        String refreshToken = jwtProvider.generateRefreshToken("DUENA01");
        when(blacklistPort.estaRevocado(refreshToken)).thenReturn(true);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThrows(AuthException.class, () -> authService.refresh(request));
    }

    @Test
    void logout_revocaAccessYRefreshToken() {
        authService.logout("access-token-123", "refresh-token-456");

        verify(blacklistPort).revocarToken("access-token-123");
        verify(blacklistPort).revocarToken("refresh-token-456");
    }
}