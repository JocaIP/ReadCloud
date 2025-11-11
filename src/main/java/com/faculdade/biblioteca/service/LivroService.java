package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class LivroService {

    @Autowired
    private LivroRepository livroRepository;

    private final Path uploadDir = Paths.get("uploads");

    // ==========================
    // LISTAR TODOS
    // ==========================
    public List<Livro> listarTodos() {
        return livroRepository.findAll();
    }

    // ==========================
    // BUSCAR POR ID
    // ==========================
    public Optional<Livro> buscarPorId(Long id) {
        return livroRepository.findById(id);
    }

    // ==========================
    // SALVAR COM CAPA
    // ==========================
    public Livro salvarComCapa(Livro livro, MultipartFile capa) throws IOException {
        if (capa != null && !capa.isEmpty()) {
            String nomeArquivo = salvarArquivo(capa);
            livro.setImagemCapa(nomeArquivo);
        }
        return livroRepository.save(livro);
    }

    // ==========================
    // ATUALIZAR COM CAPA
    // ==========================
    public Livro atualizarComCapa(Long id, Livro livroAtualizado, MultipartFile capa) throws IOException {
        Livro existente = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        existente.setTitulo(livroAtualizado.getTitulo());
        existente.setAutor(livroAtualizado.getAutor());
        existente.setDescricao(livroAtualizado.getDescricao());
        existente.setEditora(livroAtualizado.getEditora());
        existente.setAnoPublicacao(livroAtualizado.getAnoPublicacao());
        existente.setIsbn(livroAtualizado.getIsbn());
        existente.setQuantidade(livroAtualizado.getQuantidade());
        existente.setCategoria(livroAtualizado.getCategoria());

        if (capa != null && !capa.isEmpty()) {
            String nomeArquivo = salvarArquivo(capa);
            existente.setImagemCapa(nomeArquivo);
        }

        return livroRepository.save(existente);
    }

    // ==========================
    // EXCLUIR LIVRO
    // ==========================
    public void excluirPorId(Long id) {
        if (livroRepository.existsById(id)) {
            livroRepository.deleteById(id);
        } else {
            throw new RuntimeException("Livro não encontrado para exclusão");
        }
    }

    // ==========================
    // CONTADORES
    // ==========================
    public long contarTotalLivros() {
        return livroRepository.count();
    }

    public long contarLivrosDisponiveis() {
        return livroRepository.findAll().stream()
                .filter(l -> l.getQuantidade() != null && l.getQuantidade() > 0)
                .count();
    }

    public long contarLivrosIndisponiveis() {
        return livroRepository.findAll().stream()
                .filter(l -> l.getQuantidade() != null && l.getQuantidade() == 0)
                .count();
    }

    // ==========================
    // MÉTODOS DE BUSCA PERSONALIZADOS
    // ==========================
    public List<Livro> buscarLivrosPopulares() {
        // Aqui você pode implementar lógica real (ex: mais alugados)
        // Por enquanto, apenas retorna os 5 mais recentes
        List<Livro> todos = livroRepository.findAll();
        todos.sort(Comparator.comparing(Livro::getId).reversed());
        return todos.stream().limit(5).toList();
    }

    public List<Livro> pesquisarLivros(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos();
        }
        return livroRepository.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(termo, termo);
    }

    public List<Livro> buscarPorCategoria(Long categoriaId) {
        return livroRepository.findByCategoriaId(categoriaId);
    }

    public List<Livro> buscarLivrosRelacionados(Long categoriaId, Long livroId) {
        return livroRepository.findByCategoriaIdAndIdNot(categoriaId, livroId);
    }

    // ==========================
    // BUSCAR POR TÍTULO OU AUTOR
    // ==========================
    public List<Livro> buscarPorTituloOuAutor(String search) {
        return livroRepository.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(search, search);
    }

    // ==========================
    // UPLOAD DE CAPAS
    // ==========================
    private String salvarArquivo(MultipartFile arquivo) throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String extensao = getExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID() + "." + extensao;

        Path destino = uploadDir.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return nomeArquivo;
    }

    private String getExtensao(String nome) {
        return nome != null && nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1) : "jpg";
    }

    public Object contarLivrosIndisponis() {
        return  livroRepository.findAll().stream().filter(l -> l.getQuantidade() > 0);
    };
}