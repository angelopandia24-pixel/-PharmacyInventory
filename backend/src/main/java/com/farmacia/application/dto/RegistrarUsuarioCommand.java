package com.farmacia.application.dto;

import com.farmacia.domain.model.Rol;

/**
 * Comando inmutable que transporta los datos de entrada requeridos para registrar un usuario.
 * Garantiza que la contraseña original nunca sea expuesta en logs o representaciones en texto.
 */
public record RegistrarUsuarioCommand(
        String codigoUsuario,
        String nombres,
        String apellidos,
        String password,
        Rol rol
) {

    @Override
    public String toString() {
        return "RegistrarUsuarioCommand[" +
                "codigoUsuario=" + codigoUsuario +
                ", nombres=" + nombres +
                ", apellidos=" + apellidos +
                ", password=[PROTEGIDA]" +
                ", rol=" + rol +
                ']';
    }
}
