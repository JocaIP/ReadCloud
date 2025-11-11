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

    // Buscar avaliações por livro
    List<Avaliacao> findByLivroId(Long livroId);

    // Buscar avaliações por usuário
    List<Avaliacao> findByUsuarioId(Long usuarioId);

    // Buscar avaliação específica de um usuário para um livro
    Optional<Avaliacao> findByLivroIdAndUsuarioId(Long livroId, Long usuarioId);

    // Buscar avaliações ativas por livro
    List<Avaliacao> findByLivroIdAndAtivaTrue(Long livroId);

    // Buscar avaliações ativas por usuário
    List<Avaliacao> findByUsuarioIdAndAtivaTrue(Long usuarioId);

    // Verificar se usuário já avaliou um livro
    boolean existsByLivroIdAndUsuarioIdAndAtivaTrue(Long livroId, Long usuarioId);

    // Buscar avaliação por empréstimo
    Optional<Avaliacao> findByEmprestimoId(Long emprestimoId);

    // Calcular média de ratings de um livro
    @Query("SELECT AVG(a.rating) FROM Avaliacao a WHERE a.livro.id = :livroId AND a.ativa = true")
    Double calcularMediaRatingPorLivro(@Param("livroId") Long livroId);

    // Contar número de avaliações de um livro
    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.livro.id = :livroId AND a.ativa = true")
    Long contarAvaliacoesPorLivro(@Param("livroId") Long livroId);
}