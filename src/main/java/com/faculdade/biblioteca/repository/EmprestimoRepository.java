package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    // 🔹 Verifica se o usuário já possui um empréstimo ativo do mesmo livro
    boolean existsByUsuarioAndLivroAndDevolvidoFalse(Usuario usuario, Livro livro);

    // 🔹 Busca todos os empréstimos de um usuário
    List<Emprestimo> findByUsuarioId(Long usuarioId);

    // 🔹 Busca todos os empréstimos ativos (não devolvidos)
    List<Emprestimo> findByUsuarioIdAndDevolvidoFalse(Long usuarioId);

    // 🔹 Busca histórico (já devolvidos)
    List<Emprestimo> findByUsuarioIdAndDevolvidoTrue(Long usuarioId);

    // 🔹 Busca atrasados (prazo vencido e ainda não devolvidos)
    List<Emprestimo> findByUsuarioIdAndDataPrevistaDevolucaoBeforeAndDevolvidoFalse(
            Long usuarioId, LocalDate dataAtual);

    // 🔹 Conta empréstimos atrasados (para painel admin)
    long countByDataPrevistaDevolucaoBeforeAndDevolvidoFalse(LocalDate dataAtual);

    // ==============================
    // NOVOS MÉTODOS PARA AVALIAÇÕES
    // ==============================

    // 🔹 Buscar empréstimo ativo por usuário e livro
    @Query("SELECT e FROM Emprestimo e WHERE e.usuario.id = :usuarioId AND e.livro.id = :livroId AND e.devolvido = false")
    Optional<Emprestimo> findTopByUsuarioIdAndLivroIdAndDevolvidoFalse(@Param("usuarioId") Long usuarioId, @Param("livroId") Long livroId);

    // 🔹 Buscar empréstimos concluídos por usuário e livro
    @Query("SELECT e FROM Emprestimo e WHERE e.usuario.id = :usuarioId AND e.livro.id = :livroId AND e.devolvido = true")
    List<Emprestimo> findByUsuarioIdAndLivroIdAndDevolvidoTrue(@Param("usuarioId") Long usuarioId, @Param("livroId") Long livroId);

    // 🔹 Buscar empréstimo por ID verificando se pertence ao usuário
    @Query("SELECT e FROM Emprestimo e WHERE e.id = :emprestimoId AND e.usuario.id = :usuarioId")
    Optional<Emprestimo> findByIdAndUsuarioId(@Param("emprestimoId") Long emprestimoId, @Param("usuarioId") Long usuarioId);

    // 🔹 Verificar se usuário já alugou o livro (tem empréstimo concluído)
    @Query("SELECT COUNT(e) > 0 FROM Emprestimo e WHERE e.usuario.id = :usuarioId AND e.livro.id = :livroId AND e.devolvido = true")
    boolean existsByUsuarioIdAndLivroIdAndDevolvidoTrue(@Param("usuarioId") Long usuarioId, @Param("livroId") Long livroId);
}