package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.dto.GraficoLivroDTO;
import com.faculdade.biblioteca.dto.GraficoEstadoDTO;
import com.faculdade.biblioteca.dto.GraficoUsuarioDTO;
import com.faculdade.biblioteca.dto.GraficoBaixaSaidaDTO;
import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.StatusEmprestimo;
import org.springframework.data.domain.Pageable;
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

    // ✅ TOP 10 LIVROS
    @Query("""
        SELECT new com.faculdade.biblioteca.dto.GraficoLivroDTO(l.titulo, COUNT(e))
        FROM Emprestimo e JOIN e.livro l
        GROUP BY l.titulo
        ORDER BY COUNT(e) DESC
    """)
    List<GraficoLivroDTO> buscarTop10LivrosMaisAlugados(Pageable pageable);

    // ✅ TOP 10 ESTADOS
    @Query("""
        SELECT new com.faculdade.biblioteca.dto.GraficoEstadoDTO(u.estado, COUNT(e))
        FROM Emprestimo e JOIN e.usuario u
        GROUP BY u.estado
        ORDER BY COUNT(e) DESC
    """)
    List<GraficoEstadoDTO> buscarTop10Estados(Pageable pageable);

    // ✅ TOP 5 USUÁRIOS QUE MAIS ALUGAM
    @Query("""
        SELECT new com.faculdade.biblioteca.dto.GraficoUsuarioDTO(u.nome, COUNT(e))
        FROM Emprestimo e JOIN e.usuario u
        GROUP BY u.nome, u.id
        ORDER BY COUNT(e) DESC
    """)
    List<GraficoUsuarioDTO> buscarTop5Usuarios(Pageable pageable);

    // ✅ LIVROS NUNCA ALUGADOS
    @Query("""
        SELECT l FROM Livro l
        WHERE l.id NOT IN (
            SELECT DISTINCT e.livro.id FROM Emprestimo e
        )
        ORDER BY l.titulo
    """)
    List<Livro> buscarLivrosNuncaAlugados();

    // ✅ EMPRÉSTIMOS POR MÊS (últimos 6 meses)
    @Query(value = """
        SELECT 
            DATE_FORMAT(e.data_emprestimo, '%Y-%m') as mes,
            COUNT(*) as total
        FROM emprestimos e
        WHERE e.data_emprestimo >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
        GROUP BY DATE_FORMAT(e.data_emprestimo, '%Y-%m')
        ORDER BY mes
    """, nativeQuery = true)
    List<Object[]> buscarEmprestimosPorMes();

    // ✅ LIVROS COM BAIXA SAÍDA (menos de 3 empréstimos nos últimos 6 meses)
    @Query("""
        SELECT new com.faculdade.biblioteca.dto.GraficoBaixaSaidaDTO(
            l.titulo, 
            (SELECT COUNT(e) FROM Emprestimo e WHERE e.livro = l AND e.dataEmprestimo >= :dataLimite)
        )
        FROM Livro l
        WHERE (
            SELECT COUNT(e) 
            FROM Emprestimo e 
            WHERE e.livro = l 
            AND e.dataEmprestimo >= :dataLimite
        ) < 3
        ORDER BY l.titulo
    """)
    List<GraficoBaixaSaidaDTO> buscarLivrosComBaixaSaida(@Param("dataLimite") LocalDateTime dataLimite);
}