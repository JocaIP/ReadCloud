package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    boolean existsByUsuarioAndLivroAndDevolvidoFalse(Usuario usuario, Livro livro);

    List<Emprestimo> findByUsuarioId(Long usuarioId);

    List<Emprestimo> findByUsuarioIdAndDevolvidoFalse(Long usuarioId);

    List<Emprestimo> findByUsuarioIdAndDevolvidoTrue(Long usuarioId);

    @Query("""
        SELECT e FROM Emprestimo e
        WHERE e.usuario.id = :usuarioId
        AND e.dataDevolucao < :dataAtual
        AND e.devolvido = false
    """)
    List<Emprestimo> findAtrasadosPorUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("dataAtual") LocalDateTime dataAtual
    );

    @Query("""
        SELECT COUNT(e) FROM Emprestimo e
        WHERE e.dataDevolucao < :dataAtual
        AND e.devolvido = false
    """)
    long countByDataPrevistaDevolucaoBeforeAndDevolvidoFalse(
            @Param("dataAtual") LocalDateTime dataAtual
    );

    Optional<Emprestimo> findTopByUsuarioIdAndLivroIdAndDevolvidoFalseOrderByDataEmprestimoDesc(
            Long usuarioId, Long livroId
    );

    List<Emprestimo> findByUsuarioIdAndLivroIdAndDevolvidoTrue(Long usuarioId, Long livroId);

    boolean existsByUsuarioIdAndLivroIdAndDevolvidoTrue(Long usuarioId, Long livroId);

    Optional<Emprestimo> findByIdAndUsuarioId(Long emprestimoId, Long usuarioId);

    List<Emprestimo> findByStatus(StatusEmprestimo status);

    @Query("""
        SELECT e FROM Emprestimo e
        WHERE e.status = 'ATIVO'
        AND e.dataDevolucao < :now
    """)
    List<Emprestimo> findEmprestimosAtrasados(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(e) FROM Emprestimo e WHERE e.status = 'ATIVO'")
    Long countEmprestimosAtivos();

    @Query("SELECT COUNT(e) FROM Emprestimo e WHERE e.status = 'ATRASADO'")
    Long countEmprestimosAtrasados();

    @Query("""
        SELECT COUNT(e) FROM Emprestimo e
        WHERE e.status = 'FINALIZADO'
        AND MONTH(e.dataDevolucaoReal) = MONTH(CURRENT_DATE)
        AND YEAR(e.dataDevolucaoReal) = YEAR(CURRENT_DATE)
    """)
    Long countEmprestimosFinalizadosEsteMes();

    @Query("""
        SELECT COUNT(e) FROM Emprestimo e
        WHERE e.usuario.id = :usuarioId
        AND e.status IN ('ATIVO','ATRASADO')
    """)
    Long countEmprestimosAtivosPorUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
        SELECT e FROM Emprestimo e
        WHERE LOWER(e.usuario.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
        OR LOWER(e.livro.titulo) LIKE LOWER(CONCAT('%', :termo, '%'))
        OR CAST(e.id AS string) LIKE CONCAT('%', :termo, '%')
    """)
    List<Emprestimo> buscarPorTermo(@Param("termo") String termo);

    List<Emprestimo> findByUsuarioIdAndStatus(Long usuarioId, StatusEmprestimo status);
    Optional<Emprestimo> findTopByUsuarioIdOrderByDataEmprestimoDesc(Long usuarioId);

}
