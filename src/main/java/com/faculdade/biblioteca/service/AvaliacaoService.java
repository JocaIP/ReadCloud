package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Avaliacao;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.repository.AvaliacaoRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;

    @Autowired
    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            LivroRepository livroRepository,
                            EmprestimoRepository emprestimoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.livroRepository = livroRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    // ==============================
    // MÉTODOS DE BUSCA
    // ==============================

    public List<Avaliacao> buscarAvaliacoesPorUsuario(Long usuarioId) {
        return avaliacaoRepository.findByUsuarioIdAndAtivaTrue(usuarioId);
    }

    public List<Avaliacao> buscarAvaliacoesPorLivro(Long livroId) {
        return avaliacaoRepository.findByLivroIdAndAtivaTrue(livroId);
    }

    public Optional<Avaliacao> buscarAvaliacaoPorId(Long id) {
        return avaliacaoRepository.findById(id);
    }

    // ==============================
    // MÉTODOS DE CRIAÇÃO/EDIÇÃO
    // ==============================

    public void adicionarAvaliacao(Long livroId, Usuario usuario, Integer nota, String comentario) {
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        // Verificar se o usuário já avaliou este livro
        if (avaliacaoRepository.existsByLivroIdAndUsuarioIdAndAtivaTrue(livroId, usuario.getId())) {
            throw new RuntimeException("Você já avaliou este livro");
        }

        // Buscar empréstimo ativo para vincular (opcional)
        Optional<Emprestimo> emprestimoAtivo = emprestimoRepository
                .findTopByUsuarioIdAndLivroIdAndDevolvidoFalse(usuario.getId(), livroId);

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setUsuario(usuario);
        avaliacao.setLivro(livro);
        avaliacao.setRating(nota);
        avaliacao.setComentario(comentario);
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        avaliacao.setAtiva(true);

        // Vincular empréstimo se existir
        emprestimoAtivo.ifPresent(avaliacao::setEmprestimo);

        avaliacaoRepository.save(avaliacao);
    }

    public void adicionarAvaliacaoComEmprestimo(Long emprestimoId, Usuario usuario, Integer nota, String comentario) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado"));

        // Verificar se o empréstimo pertence ao usuário
        if (!emprestimo.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Este empréstimo não pertence a você");
        }

        // Verificar se já existe avaliação para este empréstimo
        if (avaliacaoRepository.findByEmprestimoId(emprestimoId).isPresent()) {
            throw new RuntimeException("Você já avaliou este empréstimo");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setUsuario(usuario);
        avaliacao.setLivro(emprestimo.getLivro());
        avaliacao.setEmprestimo(emprestimo);
        avaliacao.setRating(nota);
        avaliacao.setComentario(comentario);
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        avaliacao.setAtiva(true);

        avaliacaoRepository.save(avaliacao);
    }

    public void editarAvaliacao(Long avaliacaoId, Usuario usuario, Integer nota, String comentario) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        // Verificar se a avaliação pertence ao usuário
        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para editar esta avaliação");
        }

        avaliacao.setRating(nota);
        avaliacao.setComentario(comentario);
        avaliacao.setDataAvaliacao(LocalDateTime.now());

        avaliacaoRepository.save(avaliacao);
    }

    // ==============================
    // MÉTODOS DE REMOÇÃO
    // ==============================

    public void removerAvaliacao(Long avaliacaoId, Usuario usuario) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        // Verificar se a avaliação pertence ao usuário
        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para remover esta avaliação");
        }

        // Soft delete - marca como inativa
        avaliacao.setAtiva(false);
        avaliacaoRepository.save(avaliacao);
    }

    public void excluirAvaliacao(Long avaliacaoId, Usuario usuario) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        // Verificar se a avaliação pertence ao usuário
        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir esta avaliação");
        }

        // Hard delete - remove permanentemente
        avaliacaoRepository.delete(avaliacao);
    }

    // ==============================
    // MÉTODOS ESTATÍSTICOS
    // ==============================

    public Double calcularMediaAvaliacoes(Long livroId) {
        Double media = avaliacaoRepository.calcularMediaRatingPorLivro(livroId);
        return media != null ? Math.round(media * 10.0) / 10.0 : 0.0;
    }

    public Long contarAvaliacoesPorLivro(Long livroId) {
        return avaliacaoRepository.contarAvaliacoesPorLivro(livroId);
    }

    public boolean usuarioPodeAvaliarLivro(Long livroId, Long usuarioId) {
        // Verifica se o usuário já alugou o livro (tem empréstimo concluído)
        List<Emprestimo> emprestimosConcluidos = emprestimoRepository
                .findByUsuarioIdAndLivroIdAndDevolvidoTrue(usuarioId, livroId);

        return !emprestimosConcluidos.isEmpty() &&
                !avaliacaoRepository.existsByLivroIdAndUsuarioIdAndAtivaTrue(livroId, usuarioId);
    }
}