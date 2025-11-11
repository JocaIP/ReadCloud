package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);

    // MÉTODO ADICIONADO QUE FALTAVA:
    long countByAtivo(boolean ativo);
}