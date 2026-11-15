package com.farmacia.application.service;

import com.farmacia.application.dto.AuthResponse;
import com.farmacia.application.dto.LoginRequest;
import com.farmacia.application.dto.RefreshRequest;
import com.farmacia.domain.exception.AuthException;
import com.farmacia.infrastructure.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.farmacia.domain.repository.UsuarioAuthPort;
import com.farmacia.domain.repository.TokenBlacklistPort;

@Service
public class AuthService {

    private final UsuarioAuthPort usuarioAuthPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistPort blacklistPort;

    public AuthService(UsuarioAuthPort usuarioAuthPort,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       TokenBlacklistPort blacklistPort) {
        this.usuarioAuthPort = usuarioAuthPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.blacklistPort = blacklistPort;
    }

    public AuthResponse login(LoginRequest request) {
        var usuario = usuarioAuthPort.buscarPorCodigoAcceso(request.getCodigoAcceso())
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));

        if (!usuario.activo()) {
            throw new AuthException("El usuario se encuentra deshabilitado");
        }

        if (!passwordEncoder.matches(request.getContrasena(), usuario.contrasenaHash())) {
            throw new AuthException("Credenciales inválidas");
        }

        String accessToken = jwtProvider.generateToken(usuario.codigoAcceso(), usuario.rol());
        String refreshToken = jwtProvider.generateRefreshToken(usuario.codigoAcceso());

        return new AuthResponse(accessToken, refreshToken, usuario.nombreCompleto(), usuario.rol());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || blacklistPort.estaRevocado(refreshToken) ||
                !jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new AuthException("Refresh token inválido o expirado");
        }

        String codigoAcceso = jwtProvider.getUsernameFromToken(refreshToken);
        var usuario = usuarioAuthPort.buscarPorCodigoAcceso(codigoAcceso)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!usuario.activo()) {
            throw new AuthException("Usuario deshabilitado");
        }

        blacklistPort.revocarToken(refreshToken);
        String nuevoAccess = jwtProvider.generateToken(usuario.codigoAcceso(), usuario.rol());
        String nuevoRefresh = jwtProvider.generateRefreshToken(usuario.codigoAcceso());

        return new AuthResponse(nuevoAccess, nuevoRefresh, usuario.nombreCompleto(), usuario.rol());
    }

    public void logout(String accessToken, String refreshToken) {
        blacklistPort.revocarToken(accessToken);
        blacklistPort.revocarToken(refreshToken);
    }
}