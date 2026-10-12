package com.example.ticket.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.ticket.entity.user.UserEntity;
import com.example.ticket.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(UserEntity user) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getFullName(),
            user.getRole(),
            Collections.singletonList(authority)
        );
    }

    public static UserPrincipal createFromClaims(Long id, String email, String fullName, UserRole role) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());
        return new UserPrincipal(
            id,
            email,
            null,
            fullName,
            role,
            Collections.singletonList(authority)
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
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
