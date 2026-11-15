package com.farmacia.domain.repository;

public interface TokenBlacklistPort {
    void revocarToken(String token);
    boolean estaRevocado(String token);
}