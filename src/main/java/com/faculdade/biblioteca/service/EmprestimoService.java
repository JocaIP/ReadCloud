package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;

    @Autowired
    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             LivroRepository livroRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
    }

    // ==========================================================
    // 🔹 ALUGAR LIVRO
    // ==========================================================
    @Transactional
    public void alugarLivro(Long livroId, Usuario usuario) {
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        // 🔸 Verifica se há exemplares disponíveis
        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Este livro não está disponível no momento.");
        }

        // 🔸 Verifica se o usuário já possui o mesmo livro alugado e não devolvido
        boolean jaTemEmprestimoAtivo = emprestimoRepository
                .existsByUsuarioAndLivroAndDevolvidoFalse(usuario, livro);

        if (jaTemEmprestimoAtivo) {
            throw new RuntimeException("Você já possui este livro alugado atualmente.");
        }

        // 🔸 Cria o novo empréstimo
        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setUsuario(usuario);
        emprestimo.setLivro(livro);
        emprestimo.setDataEmprestimo(LocalDate.now());
        emprestimo.setDataPrevistaDevolucao(LocalDate.now().plusDays(7)); // 7 dias para devolver
        emprestimo.setDevolvido(false);

        // 🔸 Salva o empréstimo e atualiza a quantidade de livros disponíveis
        emprestimoRepository.save(emprestimo);
        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);
    }

    // ==========================================================
    // 🔹 DEVOLVER LIVRO
    // ==========================================================
    @Transactional
    public void devolverLivro(Long emprestimoId, Usuario usuario) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        // 🔸 Verifica se pertence ao usuário
        if (!emprestimo.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para devolver este livro.");
        }

        // 🔸 Verifica se já foi devolvido
        if (emprestimo.isDevolvido()) {
            throw new RuntimeException("Este livro já foi devolvido.");
        }

        // 🔸 Marca como devolvido
        emprestimo.setDevolvido(true);
        emprestimo.setDataDevolucao(LocalDate.now());
        emprestimoRepository.save(emprestimo);

        // 🔸 Devolve 1 exemplar ao estoque
        Livro livro = emprestimo.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);
    }

    // ==========================================================
    // 🔹 BUSCAR TODOS OS EMPRÉSTIMOS DE UM USUÁRIO
    // ==========================================================
    public List<Emprestimo> buscarPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioId(usuarioId);
    }

    // ==========================================================
    // 🔹 BUSCAR EMPRÉSTIMOS ATIVOS (não devolvidos)
    // ==========================================================
    public List<Emprestimo> buscarEmprestimosAtivos(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndDevolvidoFalse(usuarioId);
    }

    // ==========================================================
    // 🔹 BUSCAR HISTÓRICO DE EMPRÉSTIMOS (já devolvidos)
    // ==========================================================
    public List<Emprestimo> buscarHistorico(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndDevolvidoTrue(usuarioId);
    }

    // ==========================================================
    // 🔹 BUSCAR ATRASOS POR USUÁRIO
    // ==========================================================
    public List<Emprestimo> buscarAtrasosPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndDataPrevistaDevolucaoBeforeAndDevolvidoFalse(
                usuarioId, LocalDate.now());
    }

    // ==========================================================
    // 🔹 CONTAR EMPRÉSTIMOS ATRASADOS (para painel admin)
    // ==========================================================
    public long contarEmprestimosAtrasados() {
        return emprestimoRepository.countByDataPrevistaDevolucaoBeforeAndDevolvidoFalse(LocalDate.now());
    }

    // ==========================================================
    // 🔹 LISTAR TODOS (ADMIN)
    // ==========================================================
    public List<Emprestimo> listarTodos() {
        return emprestimoRepository.findAll();
    }

    public long contarEmprestimosAtivos() {
        return emprestimoRepository.findAll().stream()
                .filter(e -> !e.isDevolvido())
                .count();
    }

    public void realizarEmprestimo(Long livroId, Long id) {
    }
}