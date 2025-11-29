package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Avaliacao;
import com.faculdade.biblioteca.repository.AvaliacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    // ============================================================
    // SALVAR / CRIAR
    // ============================================================
    public Avaliacao salvar(Avaliacao avaliacao) {

        if (avaliacao.getUsuario() == null || avaliacao.getUsuario().getId() == null) {
            throw new RuntimeException("Avaliação sem usuário válido.");
        }

        if (avaliacao.getLivro() == null || avaliacao.getLivro().getId() == null) {
            throw new RuntimeException("Avaliação sem livro válido.");
        }

        // Evita avaliar o mesmo livro mais de uma vez
        boolean existe = avaliacaoRepository.existsByUsuarioIdAndLivroIdAndAtivaTrue(
                avaliacao.getUsuario().getId(),
                avaliacao.getLivro().getId()
        );

        if (existe) {
            throw new RuntimeException("Você já avaliou este livro anteriormente.");
        }

        return avaliacaoRepository.save(avaliacao);
    }

    // ============================================================
    // BUSCAR
    // ============================================================
    public Optional<Avaliacao> buscarPorId(Long id) {
        return avaliacaoRepository.findById(id);
    }

    public List<Avaliacao> buscarAvaliacoesPorLivro(Long livroId) {
        return avaliacaoRepository.findByLivroIdAndAtivaTrueOrderByDataAvaliacaoDesc(livroId);
    }

    public List<Avaliacao> buscarAvaliacoesPorUsuario(Long usuarioId) {
        return avaliacaoRepository.findByUsuarioIdAndAtivaTrueOrderByDataAvaliacaoDesc(usuarioId);
    }

    // ============================================================
    // MÉDIA DE AVALIAÇÕES
    // ============================================================
    public Double calcularMediaAvaliacoes(Long livroId) {
        Double media = avaliacaoRepository.calcularMediaAvaliacoesAtivas(livroId);
        return media != null ? media : 0.0;
    }

    public boolean usuarioJaAvaliouLivro(Long usuarioId, Long livroId) {
        return avaliacaoRepository.existsByUsuarioIdAndLivroIdAndAtivaTrue(usuarioId, livroId);
    }

    public Integer contarAvaliacoesPorLivro(Long livroId) {
        return avaliacaoRepository.countByLivroIdAndAtivaTrue(livroId);
    }

    // ============================================================
    // EXCLUSÃO / DESATIVAÇÃO
    // ============================================================
    @Transactional
    public void excluir(Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada."));
        avaliacaoRepository.delete(avaliacao);
    }

    @Transactional
    public void desativarAvaliacao(Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada."));
        avaliacao.setAtiva(false);
        avaliacaoRepository.save(avaliacao);
    }
}
