package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    @Query("""
        SELECT l FROM Livro l
        WHERE LOWER(l.titulo) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR LOWER(l.isbn) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR LOWER(l.descricao) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR CAST(l.anoPublicacao AS string) LIKE CONCAT('%', :termo, '%')
           OR CAST(l.id AS string) LIKE CONCAT('%', :termo, '%')
    """)
    List<Livro> buscarAvancado(@Param("termo") String termo);

    List<Livro> findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(
            String titulo, String autor
    );

    List<Livro> findByCategoriaId(Long categoriaId);

    List<Livro> findByCategoriaIdAndIdNot(Long categoriaId, Long id);

    List<Livro> findByIsbn(String isbn);

    List<Livro> findByAnoPublicacao(Integer anoPublicacao);

    List<Livro> findByEditoraContainingIgnoreCase(String editora);

    @Query("SELECT l FROM Livro l WHERE l.quantidade > 0")
    List<Livro> buscarDisponiveis();

    @Query("SELECT l FROM Livro l WHERE l.quantidade = 0")
    List<Livro> buscarIndisponiveis();

    @Query("""
        SELECT l FROM Livro l
        WHERE (:busca IS NULL OR
              LOWER(l.titulo) LIKE LOWER(CONCAT('%', :busca, '%')) OR
              LOWER(l.autor) LIKE LOWER(CONCAT('%', :busca, '%')) OR
              LOWER(l.editora) LIKE LOWER(CONCAT('%', :busca, '%')))
          AND (:categoriaId IS NULL OR l.categoria.id = :categoriaId)
          AND (
                :disponibilidade IS NULL OR
                (:disponibilidade = 'disponivel' AND l.quantidade > 0) OR
                (:disponibilidade = 'indisponivel' AND l.quantidade = 0)
              )
    """)
    List<Livro> filtrarLivros(
            @Param("busca") String busca,
            @Param("categoriaId") Long categoriaId,
            @Param("disponibilidade") String disponibilidade
    );
}
