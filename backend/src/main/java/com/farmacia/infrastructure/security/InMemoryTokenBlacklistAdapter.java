package com.farmacia.infrastructure.security;

import com.farmacia.domain.repository.TokenBlacklistPort;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Lista negra en memoria (sin tabla en SQL Server) con limpieza automática de tokens expirados.
@Component

public class InMemoryTokenBlacklistAdapter implements TokenBlacklistPort {

    private final Set<String> tokensRevocados = ConcurrentHashMap.newKeySet();

    @Override
    public void revocarToken(String token) {
        if (token != null && !token.isBlank()) {
            tokensRevocados.add(token);
        }
    }

    @Override
    public boolean estaRevocado(String token) {
        return tokensRevocados.contains(token);
    }
}