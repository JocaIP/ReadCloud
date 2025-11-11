package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    List<Livro> findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(String titulo, String autor);

    List<Livro> findByCategoriaId(Long categoriaId);

    List<Livro> findByCategoriaIdAndIdNot(Long categoriaId, Long id);
}