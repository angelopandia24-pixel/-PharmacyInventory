package com.farmacia.infrastructure.persistence.adapter;

import com.farmacia.domain.exception.CodigoUsuarioDuplicadoException;
import com.farmacia.domain.model.Usuario;
import com.farmacia.domain.repository.UsuarioRepository;
import com.farmacia.infrastructure.persistence.entity.UsuarioEntity;
import com.farmacia.infrastructure.persistence.mapper.UsuarioEntityMapper;
import com.farmacia.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador de infraestructura que implementa el puerto UsuarioRepository de la capa de dominio.
 * Conecta las operaciones del dominio con Spring Data JPA y el mapeador de entidades,
 * gestionando la traducción de excepciones de persistencia de SQL Server a excepciones de dominio.
 */
@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private static final int SQL_SERVER_UNIQUE_CONSTRAINT_ERROR = 2627;
    private static final int SQL_SERVER_UNIQUE_INDEX_ERROR = 2601;

    /**
     * Nombres de restricciones o índices únicos verificados en SQL Server para la columna 'codigo_usuario'.
     * UK_28jlrohqs9b6tgmygccr7kdb2 corresponde al nombre real generado por Hibernate en FarmaciaDB_HU01_Test.
     */
    private static final Set<String> RESTRICCIONES_CODIGO_USUARIO = Set.of(
            "uk_28jlrohqs9b6tgmygccr7kdb2",
            "uq_usuario_codigousuario"
    );

    /**
     * Expresión regular para extraer de manera precisa el nombre de la restricción o índice
     * reportado entre comillas simples en los mensajes oficiales de SQL Server (errores 2627 y 2601).
     * Evita falsos positivos causados por el texto del query o valores de parámetros que contengan 'codigo_usuario'.
     */
    private static final Pattern PATTERN_CONSTRAINT_SQLSERVER =
            Pattern.compile("(?i)(?:constraint|unique index)\\s+'(?:[^.']+\\.)?([^']+)'");

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final UsuarioEntityMapper usuarioEntityMapper;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository, UsuarioEntityMapper usuarioEntityMapper) {
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.usuarioEntityMapper = usuarioEntityMapper;
    }

    @Override
    public Usuario save(Usuario usuario) {
        try {
            UsuarioEntity entity = usuarioEntityMapper.toEntity(usuario);
            UsuarioEntity savedEntity = usuarioJpaRepository.saveAndFlush(entity);
            return usuarioEntityMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionUnicidadCodigoUsuario(ex)) {
                throw CodigoUsuarioDuplicadoException.porCodigo(usuario.getCodigoUsuario(), ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioJpaRepository.findById(id)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByCodigoUsuario(String codigoUsuario) {
        return usuarioJpaRepository.findByCodigoUsuario(codigoUsuario)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public boolean existsByCodigoUsuario(String codigoUsuario) {
        return usuarioJpaRepository.existsByCodigoUsuario(codigoUsuario);
    }

    private boolean esViolacionUnicidadCodigoUsuario(DataIntegrityViolationException ex) {
        Throwable current = ex;
        boolean esCodigoDuplicadoSql = false;
        boolean esRestriccionCodigoUsuario = false;

        while (current != null) {
            // 1. Priorizar ConstraintViolationException de Hibernate
            if (current instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (esNombreRestriccionCodigoUsuario(constraintName)) {
                    esRestriccionCodigoUsuario = true;
                }
            }

            // 2. Analizar SQLException del driver JDBC (código 2627 o 2601 e identificar nombre exacto en el mensaje)
            if (current instanceof SQLException sqlEx) {
                int errorCode = sqlEx.getErrorCode();
                if (errorCode == SQL_SERVER_UNIQUE_CONSTRAINT_ERROR || errorCode == SQL_SERVER_UNIQUE_INDEX_ERROR) {
                    esCodigoDuplicadoSql = true;
                }
                String mensaje = sqlEx.getMessage();
                if (mensajeIdentificaRestriccionCodigoUsuario(mensaje)) {
                    esRestriccionCodigoUsuario = true;
                }
            }
            current = current.getCause();
        }

        return esCodigoDuplicadoSql && esRestriccionCodigoUsuario;
    }

    private boolean esNombreRestriccionCodigoUsuario(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return false;
        }
        String nombreNormalizado = normalizarNombreRestriccion(constraintName);
        return RESTRICCIONES_CODIGO_USUARIO.contains(nombreNormalizado);
    }

    private boolean mensajeIdentificaRestriccionCodigoUsuario(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return false;
        }
        Matcher matcher = PATTERN_CONSTRAINT_SQLSERVER.matcher(mensaje);
        if (matcher.find()) {
            String nombreExtraido = normalizarNombreRestriccion(matcher.group(1));
            return RESTRICCIONES_CODIGO_USUARIO.contains(nombreExtraido);
        }
        return false;
    }

    private String normalizarNombreRestriccion(String constraintName) {
        String limpio = constraintName.trim().toLowerCase();
        int puntoIndex = limpio.lastIndexOf('.');
        if (puntoIndex >= 0 && puntoIndex < limpio.length() - 1) {
            limpio = limpio.substring(puntoIndex + 1);
        }
        return limpio;
    }
}
