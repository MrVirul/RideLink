package com.ridelink.account_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
@Schema(name = "User", description = """
        An account, as persisted. This is the internal JPA entity and it is never serialised
        into a response: the BCrypt hash and the `UserDetails` plumbing fields
        (`authorities`, `username`, `accountNonLocked`, ...) are kept off the wire by
        `AuthController.UserResponse`, which is the shape the API actually returns.
        """)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Account id, referenced as `userId` by the other services", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @NotNull
    @Column(nullable = false)
    @Schema(description = "Full name of the account holder", example = "Geeth Perera")
    private String name;

    @NotNull
    @Email
    @Column(nullable = false, unique = true)
    @Schema(description = "Unique email address, doubles as the login username", example = "geeth@example.com")
    private String email;

    @NotNull
    @Column(nullable = false)
    @Schema(description = "BCrypt hash of the password. Never leaves the service - responses use `UserResponse`", example = "$2a$10$abcdefghijklmnopqrstuv")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Schema(description = "Account role", example = "DRIVER")
    private Role role = Role.PASSENGER;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
