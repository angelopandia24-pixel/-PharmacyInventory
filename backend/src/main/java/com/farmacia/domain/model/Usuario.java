package com.farmacia.domain.model;

import java.util.Objects;

/**
 * Entidad de dominio pura para Usuario en Biofarma.
 * Desacoplada totalmente de Spring Boot, JPA, Hibernate y SQL Server.
 */
public class Usuario {

    private final Long id;
    private final String codigoUsuario;
    private final String nombres;
    private final String apellidos;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;

    /**
     * Constructor para la creación de un nuevo usuario en el sistema.
     * Asigna id en null y estado activo en true por defecto.
     *
     * @param codigoUsuario Código personal único del usuario.
     * @param nombres       Nombres del usuario.
     * @param apellidos     Apellidos del usuario.
     * @param passwordHash  Hash de la contraseña (nunca texto plano).
     * @param rol           Rol asignado (ADMINISTRADORA o TRABAJADORA).
     */
    public Usuario(String codigoUsuario, String nombres, String apellidos, String passwordHash, Rol rol) {
        this(null, codigoUsuario, nombres, apellidos, passwordHash, rol, true);
    }

    /**
     * Constructor completo para reconstitución de la entidad existente.
     *
     * @param id            Identificador único asignado por la base de datos.
     * @param codigoUsuario Código personal único del usuario.
     * @param nombres       Nombres del usuario.
     * @param apellidos     Apellidos del usuario.
     * @param passwordHash  Hash de la contraseña.
     * @param rol           Rol asignado.
     * @param activo        Estado de actividad del usuario.
     */
    public Usuario(Long id, String codigoUsuario, String nombres, String apellidos, String passwordHash, Rol rol, boolean activo) {
        validarInvariantes(codigoUsuario, nombres, apellidos, passwordHash, rol);
        this.id = id;
        this.codigoUsuario = codigoUsuario.trim();
        this.nombres = nombres.trim();
        this.apellidos = apellidos.trim();
        this.passwordHash = passwordHash.trim();
        this.rol = rol;
        this.activo = activo;
    }

    private void validarInvariantes(String codigoUsuario, String nombres, String apellidos, String passwordHash, Rol rol) {
        if (codigoUsuario == null || codigoUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de usuario es obligatorio y no puede estar vacío");
        }
        if (nombres == null || nombres.trim().isEmpty()) {
            throw new IllegalArgumentException("Los nombres son obligatorios y no pueden estar vacíos");
        }
        if (apellidos == null || apellidos.trim().isEmpty()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios y no pueden estar vacíos");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("El passwordHash es obligatorio y no puede estar vacío");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio y no puede ser nulo");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigoUsuario() {
        return codigoUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id) && Objects.equals(codigoUsuario, usuario.codigoUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigoUsuario);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", codigoUsuario='" + codigoUsuario + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", rol=" + rol +
                ", activo=" + activo +
                '}';
    }
}
