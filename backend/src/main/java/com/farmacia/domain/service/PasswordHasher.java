package com.farmacia.domain.service;

/**
 * Puerto de salida del dominio para el hashing seguro de contraseñas.
 * Desacopla la lógica de negocio de frameworks criptográficos específicos.
 */
public interface PasswordHasher {

    /**
     * Genera un hash seguro e irreversible a partir de la contraseña original en texto plano.
     *
     * @param rawPassword Contraseña original en texto plano.
     * @return Hash seguro generado mediante el algoritmo configurado.
     */
    String hash(String rawPassword);
}

