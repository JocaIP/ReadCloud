package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.LivroRelatorio;
import com.faculdade.biblioteca.modelo.UsuarioRelatorio;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RelatorioService {

    public List<LivroRelatorio> getLivrosMaisAlugados(int dias, String categoria) {
        List<LivroRelatorio> relatorio = new ArrayList<>();

        // Dados de exemplo
        relatorio.add(new LivroRelatorio("Dom Casmurro", "Machado de Assis", "Literatura", 45L, "São Paulo", 98.0));
        relatorio.add(new LivroRelatorio("1984", "George Orwell", "Ficção", 38L, "Rio de Janeiro", 95.0));
        relatorio.add(new LivroRelatorio("O Cortiço", "Aluísio Azevedo", "Literatura", 32L, "Belo Horizonte", 97.0));

        if (!"all".equals(categoria)) {
            relatorio.removeIf(livro -> !livro.getCategoria().equalsIgnoreCase(categoria));
        }

        return relatorio;
    }

    public List<UsuarioRelatorio> getUsuariosMaisAtivos(int dias) {
        List<UsuarioRelatorio> relatorio = new ArrayList<>();

        // Dados de exemplo
        relatorio.add(new UsuarioRelatorio("João Silva", "joao@email.com", 15L, 92.5));
        relatorio.add(new UsuarioRelatorio("Maria Santos", "maria@email.com", 12L, 88.0));
        relatorio.add(new UsuarioRelatorio("Pedro Oliveira", "pedro@email.com", 8L, 95.0));

        return relatorio;
    }
}