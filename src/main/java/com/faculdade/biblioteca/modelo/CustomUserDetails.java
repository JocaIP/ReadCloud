package com.faculdade.biblioteca.modelo;

import com.faculdade.biblioteca.modelo.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return usuario.getAuthorities().stream()
                .map(auth -> (GrantedAuthority) () ->
                        auth.getAuthority().startsWith("ROLE_")
                                ? auth.getAuthority()
                                : "ROLE_" + auth.getAuthority())
                .toList();
    }

    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // usa getAtivo() que existe no seu model perfil
        return usuario.getAtivo() != null ? usuario.getAtivo() : false;
    }

    // opcional: expor id
    public Long getId() {
        return usuario.getId();
    }
}
