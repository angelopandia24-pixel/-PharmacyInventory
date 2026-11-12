package com.farmacia.infrastructure.persistence.entity;

import com.farmacia.domain.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad JPA para la tabla 'usuarios' en SQL Server.
 * Representa el modelo relacional en la capa de infraestructura.
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo_usuario", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String codigoUsuario;

    @Column(name = "nombres", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String apellidos;

    @Column(name = "password_hash", nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    /**
     * Constructor requerido por JPA / Hibernate.
     */
    public UsuarioEntity() {
    }

    /**
     * Constructor completo para inicialización y mapeo de entidades persistentes.
     */
    public UsuarioEntity(Long id, String codigoUsuario, String nombres, String apellidos, String passwordHash, Rol rol, boolean activo) {
        this.id = id;
        this.codigoUsuario = codigoUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoUsuario() {
        return codigoUsuario;
    }

    public void setCodigoUsuario(String codigoUsuario) {
        this.codigoUsuario = codigoUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioEntity that = (UsuarioEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(codigoUsuario, that.codigoUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigoUsuario);
    }

    @Override
    public String toString() {
        return "UsuarioEntity{" +
                "id=" + id +
                ", codigoUsuario='" + codigoUsuario + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", rol=" + rol +
                ", activo=" + activo +
                '}';
    }
}

