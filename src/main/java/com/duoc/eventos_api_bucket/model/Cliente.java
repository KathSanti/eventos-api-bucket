package com.duoc.eventos_api_bucket.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario del sistema (cliente o administrador).
 * Implementa UserDetails para que Spring Security pueda autenticarlo directamente.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cliente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    // unique = true: la BD garantiza que no haya dos cuentas con el mismo email,
    // incluso si llegan dos registros al mismo tiempo.
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    // Siempre se guarda el hash BCrypt, nunca la contraseña en texto plano.
    @NotBlank
    @Column(nullable = false)
    private String password;

    // STRING guarda "CLIENTE"/"ADMIN". El valor por defecto (ORDINAL) guarda 0/1,
    // y si el enum cambia de orden los roles de la BD cambiarían de significado.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    private LocalDateTime bloqueadoHasta;

    // Método del contrato UserDetails: Spring lo llama para conocer los permisos.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    // Lombok genera getPassword() a partir del campo "password".

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Bloqueada mientras no haya pasado la fecha de "bloqueadoHasta".
    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
