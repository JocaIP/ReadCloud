package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByLivroIdAndAtivaTrueOrderByDataAvaliacaoDesc(Long livroId);

    List<Avaliacao> findByUsuarioIdAndAtivaTrueOrderByDataAvaliacaoDesc(Long usuarioId);

    boolean existsByUsuarioIdAndLivroIdAndAtivaTrue(Long usuarioId, Long livroId);

    Integer countByLivroIdAndAtivaTrue(Long livroId);

    @Query("SELECT AVG(a.rating) FROM Avaliacao a WHERE a.livro.id = :livroId AND a.ativa = true")
    Double calcularMediaAvaliacoesAtivas(@Param("livroId") Long livroId);

    Optional<Avaliacao> findByEmprestimoId(Long emprestimoId);
}
