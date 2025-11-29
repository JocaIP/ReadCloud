package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    // ============================================================
    // LISTAR / BUSCAR
    // ============================================================

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    public Optional<Categoria> buscarPorNome(String nome) {
        return categoriaRepository.findByNomeIgnoreCase(nome);
    }

    public boolean existsByNome(String nome) {
        return categoriaRepository.existsByNome(nome);
    }

    // ============================================================
    // SALVAR / ATUALIZAR
    // ============================================================

    public Categoria salvar(Categoria categoria) {

        if (categoria.getNome() == null || categoria.getNome().trim().isEmpty()) {
            throw new RuntimeException("O nome da categoria não pode ser vazio.");
        }

        if (categoria.getId() == null && existsByNome(categoria.getNome())) {
            throw new RuntimeException("Já existe uma categoria com este nome.");
        }

        return categoriaRepository.save(categoria);
    }

    // ============================================================
    // EXCLUIR
    // ============================================================

    @Transactional
    public void excluir(Long id) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

        // Futuramente você pode impedir exclusão caso existam livros vinculados à categoria
        // Ex: if (!categoria.getLivros().isEmpty()) { ... }

        categoriaRepository.delete(categoria);
    }
}
