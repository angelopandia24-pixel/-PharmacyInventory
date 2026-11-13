package com.farmacia.presentation.dto;

import com.farmacia.application.dto.RegistrarUsuarioCommand;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.domain.model.Rol;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.constraints.NotBlank;

import java.io.IOException;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud web de registro de usuarios.
 * Pertenece a la capa de presentación en Clean Architecture (HU-01 / SCRUM-88).
 */
public record RegistrarUsuarioRequest(
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "El código de usuario es obligatorio")
        String codigoUsuario,

        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "El rol es obligatorio")
        String rol
) {

    /**
     * Convierte la solicitud web hacia el comando de aplicación {@link RegistrarUsuarioCommand}.
     * Valida de manera estricta que el rol corresponda a los valores permitidos del enum {@link Rol}
     * (ADMINISTRADORA o TRABAJADORA).
     *
     * @return instancia de {@link RegistrarUsuarioCommand}
     * @throws DatosUsuarioInvalidosException si el rol es nulo, vacío, o no coincide exactamente
     *                                        con un valor válido de {@link Rol}
     */
    public RegistrarUsuarioCommand toCommand() {
        if (rol == null || rol.isBlank()) {
            throw new DatosUsuarioInvalidosException("El rol especificado es inválido o no reconocido");
        }

        Rol rolEnum;
        try {
            rolEnum = Rol.valueOf(rol);
        } catch (IllegalArgumentException ex) {
            throw new DatosUsuarioInvalidosException("El rol especificado es inválido o no reconocido");
        }

        return new RegistrarUsuarioCommand(codigoUsuario, nombres, apellidos, password, rolEnum);
    }

    /**
     * Sobrescribe toString() para garantizar que la contraseña nunca sea expuesta
     * en registros de auditoría o trazas de depuración de la capa web.
     */
    @Override
    public String toString() {
        return "RegistrarUsuarioRequest[" +
                "codigoUsuario=" + codigoUsuario +
                ", nombres=" + nombres +
                ", apellidos=" + apellidos +
                ", password=[PROTEGIDA]" +
                ", rol=" + rol +
                ']';
    }

    /**
     * Deserializador estricto de cadenas para evitar coerción automática de tipos
     * numéricos, booleanos, objetos o arreglos hacia String en Jackson.
     */
    public static class StrictStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken currentToken = parser.currentToken();
            if (currentToken == JsonToken.VALUE_STRING) {
                return parser.getText();
            }
            if (currentToken == JsonToken.VALUE_NULL) {
                return null;
            }
            throw InvalidFormatException.from(
                    parser,
                    "El campo debe ser una cadena de texto JSON",
                    currentToken.name(),
                    String.class
            );
        }
    }
}
