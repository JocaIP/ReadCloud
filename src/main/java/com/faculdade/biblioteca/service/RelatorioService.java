package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.modelo.LivroRelatorio;
import com.faculdade.biblioteca.modelo.UsuarioRelatorio;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RelatorioService {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    // ====================================================================
    // RELATÓRIO: LIVROS MAIS ALUGADOS
    // ====================================================================
    public List<LivroRelatorio> getLivrosMaisAlugados(int dias, String categoriaFiltro) {

        LocalDateTime limite = LocalDateTime.now().minusDays(dias);

        List<Emprestimo> emprestimos = emprestimoRepository.findAll();

        Map<Long, Long> contador = new HashMap<>();

        for (Emprestimo e : emprestimos) {
            if (e.getDataEmprestimo() != null && e.getDataEmprestimo().isAfter(limite)) {
                contador.put(e.getLivro().getId(),
                        contador.getOrDefault(e.getLivro().getId(), 0L) + 1);
            }
        }

        List<LivroRelatorio> lista = new ArrayList<>();

        contador.forEach((livroId, total) -> {
            var livro = livroRepository.findById(livroId).orElse(null);
            if (livro != null) {

                if (categoriaFiltro != null && !"all".equalsIgnoreCase(categoriaFiltro)) {
                    if (livro.getCategoria() == null ||
                            !livro.getCategoria().getNome().equalsIgnoreCase(categoriaFiltro)) {
                        return;
                    }
                }

                lista.add(new LivroRelatorio(
                        livro.getTitulo(),
                        livro.getAutor(),
                        livro.getCategoria() != null ? livro.getCategoria().getNome() : "Sem categoria",
                        total,
                        "Localidade não disponível",
                        0.0
                ));
            }
        });

        lista.sort(Comparator.comparing(LivroRelatorio::getTotalAlugado).reversed());

        // Caso não tenha dados reais, devolve exemplo
        if (lista.isEmpty()) {
            lista.add(new LivroRelatorio("Dom Casmurro", "Machado de Assis",
                    "Literatura", 12L, "SP", 95.4));
            lista.add(new LivroRelatorio("1984", "George Orwell",
                    "Ficção", 9L, "RJ", 92.8));
        }

        return lista;
    }


    // ====================================================================
    // RELATÓRIO: USUÁRIOS MAIS ATIVOS (que mais alugam)
    // ====================================================================
    public List<UsuarioRelatorio> getUsuariosMaisAtivos(int dias) {

        LocalDateTime limite = LocalDateTime.now().minusDays(dias);

        List<Emprestimo> emprestimos = emprestimoRepository.findAll();

        Map<Long, Long> contador = new HashMap<>();

        for (Emprestimo e : emprestimos) {
            if (e.getDataEmprestimo() != null && e.getDataEmprestimo().isAfter(limite)) {
                contador.put(e.getUsuario().getId(),
                        contador.getOrDefault(e.getUsuario().getId(), 0L) + 1);
            }
        }

        List<UsuarioRelatorio> lista = new ArrayList<>();

        contador.forEach((usuarioId, total) -> {
            var usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null) {
                lista.add(new UsuarioRelatorio(
                        usuario.getNome(),
                        usuario.getEmail(),
                        total,
                        0.0
                ));
            }
        });

        lista.sort(Comparator.comparing(UsuarioRelatorio::getTotalEmprestimos).reversed());

        // fallback caso não tenha dados reais
        if (lista.isEmpty()) {
            lista.add(new UsuarioRelatorio("João Silva", "joao@email.com", 15L, 92.5));
            lista.add(new UsuarioRelatorio("Maria Santos", "maria@email.com", 12L, 88.0));
        }

        return lista;
    }

}
