package com.farmacia.presentation.advice;

import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.exception.DatosUsuarioInvalidosException;
import com.farmacia.presentation.controller.UsuarioController;
import com.farmacia.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manejador de excepciones específico y limitado exclusivamente a {@link UsuarioController}.
 * Garantiza respuestas de error HTTP seguras, estructuradas y sin exposición de trazas,
 * consultas SQL, credenciales ni valores sensibles (HU-01 / SCRUM-88).
 */
@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioExceptionHandler {

    private static final String MENSAJE_ERROR_VALIDACION_GENERICO = "Los datos proporcionados no son válidos";

    /**
     * Catálogo cerrado de mensajes de validación conocidos y seguros, originados
     * exclusivamente en las reglas de negocio de RegistrarUsuario y RegistrarUsuarioRequest.
     */
    private static final Set<String> MENSAJES_VALIDACION_PERMITIDOS = Set.of(
            "El comando de registro no puede ser nulo",
            "El código de usuario es obligatorio y no puede estar vacío",
            "El código de usuario no puede exceder 50 caracteres",
            "El código de usuario no cumple con el formato requerido",
            "Los nombres son obligatorios y no pueden estar vacíos",
            "Los nombres no pueden exceder 100 caracteres",
            "Los apellidos son obligatorios y no pueden estar vacíos",
            "Los apellidos no pueden exceder 100 caracteres",
            "La contraseña es obligatoria y no puede estar vacía",
            "La contraseña debe tener al menos 8 caracteres",
            "La contraseña no puede exceder 72 bytes en formato UTF-8",
            "El rol es obligatorio y no puede ser nulo",
            "El rol del usuario es obligatorio",
            "El rol especificado es inválido o no reconocido"
    );

    @ExceptionHandler(DatosUsuarioInvalidosException.class)
    public ResponseEntity<ErrorResponse> manejarDatosUsuarioInvalidos(
            DatosUsuarioInvalidosException ex,
            HttpServletRequest request
    ) {
        String mensaje = (ex.getMessage() != null && MENSAJES_VALIDACION_PERMITIDOS.contains(ex.getMessage()))
                ? ex.getMessage()
                : MENSAJE_ERROR_VALIDACION_GENERICO;

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining("; "));

        if (mensaje.isBlank()) {
            mensaje = MENSAJE_ERROR_VALIDACION_GENERICO;
        }

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la solicitud contiene datos inválidos o un formato JSON incorrecto",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CodigoUsuarioDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarCodigoUsuarioDuplicado(
            CodigoUsuarioDuplicadoException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "El código de usuario ya se encuentra registrado",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                "El contenido de la solicitud debe estar en formato application/json",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErroresInesperados(
            Exception ex,
            HttpServletRequest request
    ) throws Exception {
        if (ex instanceof AuthenticationException || ex instanceof AccessDeniedException) {
            throw ex;
        }
        if (ex instanceof org.springframework.web.ErrorResponse) {
            throw ex;
        }

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error interno en el servidor",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
