package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.CategoriaRepository;
import jakarta.transaction.Transactional;
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

    @Autowired
    private CategoriaRepository categoriaRepository;

    private final Path uploadDir = Paths.get("uploads");

    // ============================================================
    // LISTAGENS
    // ============================================================

    public List<Livro> listarTodos() {
        return livroRepository.findAll();
    }

    public Optional<Livro> buscarPorId(Long id) {
        return livroRepository.findById(id);
    }

    public List<Livro> buscarDisponiveis() {
        return livroRepository.buscarDisponiveis();
    }

    public List<Livro> buscarIndisponiveis() {
        return livroRepository.buscarIndisponiveis();
    }

    public List<Livro> buscarPorStatus(boolean disponivel) {
        return disponivel ? buscarDisponiveis() : buscarIndisponiveis();
    }


    public long contarLivrosDisponiveis() {
        return livroRepository.buscarDisponiveis().size();
    }

    public long contarLivrosIndisponiveis() {
        return livroRepository.buscarIndisponiveis().size();
    }

    public long contarTotalLivros() {
        return livroRepository.count();
    }

    public List<Livro> buscarLivrosPopulares() {
        List<Livro> todos = livroRepository.findAll();
        todos.sort(Comparator.comparing(Livro::getId).reversed());
        return todos.stream().limit(8).toList();
    }

    public List<Livro> listarRecentes(int limite) {
        List<Livro> todos = livroRepository.findAll();
        todos.sort(Comparator.comparing(Livro::getId).reversed());
        return todos.stream().limit(limite).toList();
    }

    // ============================================================
    // BUSCAS
    // ============================================================

    public List<Livro> pesquisarLivros(String termo) {
        return livroRepository.findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(termo, termo);
    }

    public List<Livro> buscarPorCategoria(Long categoriaId) {
        return livroRepository.findByCategoriaId(categoriaId);
    }

    public List<Livro> buscarPorIsbn(String isbn) {
        return livroRepository.findByIsbn(isbn);
    }

    public List<Livro> buscarPorAnoPublicacao(Integer ano) {
        return livroRepository.findByAnoPublicacao(ano);
    }

    public List<Livro> buscarPorEditora(String editora) {
        return livroRepository.findByEditoraContainingIgnoreCase(editora);
    }

    public List<Livro> buscarLivrosRelacionados(Long categoriaId, Long livroId) {
        return livroRepository.findByCategoriaIdAndIdNot(categoriaId, livroId);
    }

    // ============================================================
    // BUSCA INTELIGENTE
    // ============================================================

    public List<Livro> buscaInteligente(String termo) {

        if (termo == null || termo.trim().isEmpty())
            return listarTodos();

        termo = termo.trim();

        // ID direto
        if (termo.matches("\\d+")) {
            Optional<Livro> porId = buscarPorId(Long.parseLong(termo));
            if (porId.isPresent()) return List.of(porId.get());
        }

        // ISBN
        List<Livro> isbn = buscarPorIsbn(termo);
        if (!isbn.isEmpty()) return isbn;

        // Ano
        try {
            Integer ano = Integer.parseInt(termo);
            List<Livro> porAno = buscarPorAnoPublicacao(ano);
            if (!porAno.isEmpty()) return porAno;
        } catch (Exception ignored) {}

        // Filtro avançado geral
        return livroRepository.buscarAvancado(termo);
    }

    // ============================================================
    // FILTRO COMPLETO (BANCO)
    // ============================================================

    public List<Livro> filtrarLivros(String busca, Long categoriaId, String disponibilidade) {

        if ((busca == null || busca.isBlank()) &&
                categoriaId == null &&
                (disponibilidade == null || disponibilidade.isBlank())) {

            return listarTodos();
        }

        return livroRepository.filtrarLivros(busca, categoriaId, disponibilidade);
    }

    // ============================================================
    // FILTRO CLIENT-SIDE (Java)
    // ============================================================

    public List<Livro> buscarComFiltros(String titulo, String autor, String editora,
                                        Integer anoMin, Integer anoMax) {

        return livroRepository.findAll().stream()
                .filter(l -> titulo == null || titulo.isBlank() || l.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                .filter(l -> autor == null || autor.isBlank() || l.getAutor().toLowerCase().contains(autor.toLowerCase()))
                .filter(l -> editora == null || editora.isBlank() || l.getEditora().toLowerCase().contains(editora.toLowerCase()))
                .filter(l -> anoMin == null || (l.getAnoPublicacao() != null && l.getAnoPublicacao() >= anoMin))
                .filter(l -> anoMax == null || (l.getAnoPublicacao() != null && l.getAnoPublicacao() <= anoMax))
                .toList();
    }

    // ============================================================
    // UPLOAD DE IMAGENS
    // ============================================================

    public Livro salvarComCapa(Livro livro, MultipartFile capa) throws IOException {
        if (capa != null && !capa.isEmpty()) {
            livro.setImagemCapa(salvarArquivo(capa));
        }
        return livroRepository.save(livro);
    }

    @Transactional
    public Livro atualizarComCapa(Long id, Livro atual, MultipartFile capa) throws IOException {

        Livro existente = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        existente.setTitulo(atual.getTitulo());
        existente.setAutor(atual.getAutor());
        existente.setDescricao(atual.getDescricao());
        existente.setEditora(atual.getEditora());
        existente.setAnoPublicacao(atual.getAnoPublicacao());
        existente.setIsbn(atual.getIsbn());
        existente.setQuantidade(atual.getQuantidade());
        existente.setCategoria(atual.getCategoria());

        if (capa != null && !capa.isEmpty()) {
            existente.setImagemCapa(salvarArquivo(capa));
        }

        return livroRepository.save(existente);
    }

    // ============================================================
    // EXCLUSÃO
    // ============================================================

    public void excluir(Long id) {
        Livro livro = buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));
        livroRepository.delete(livro);
    }

    // ============================================================
    // UTILITÁRIOS DE ARQUIVO
    // ============================================================

    private String salvarArquivo(MultipartFile arquivo) throws IOException {

        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        String extensao = getExtensao(arquivo.getOriginalFilename());
        String nome = UUID.randomUUID() + "." + extensao;

        Path destino = uploadDir.resolve(nome);
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return nome;
    }

    private String getExtensao(String nome) {
        if (nome == null || !nome.contains(".")) return "jpg";
        return nome.substring(nome.lastIndexOf('.') + 1);
    }
}
