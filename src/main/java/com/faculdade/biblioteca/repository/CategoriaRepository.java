package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByNome(String nome);
    Optional<Categoria> findByNomeIgnoreCase(String nome);
    boolean existsByNome(String nome);
}
