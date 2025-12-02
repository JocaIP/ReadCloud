package com.faculdade.biblioteca.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livros")
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Integer quantidade = 0;

    @Column(name = "quantidade_total", nullable = false)
    private Integer quantidadeTotal = 0;

    @Column(length = 20)
    private String isbn;

    @Column(name = "ano_publicacao")
    private Integer anoPublicacao;

    @Column(length = 100)
    private String editora;

    // campo para armazenar caminho/URL da imagem (ou nome do arquivo)
    @Column(name = "imagem_capa", length = 255)
    private String imagemCapa;

    private Double mediaAvaliacoes = 0.0;
    private Long totalAvaliacoes = 0L;

    @ManyToOne(fetch = FetchType.EAGER)

    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // relacionamentos simples (omitidos getters/setters de coleções se não usar)
    public Livro() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getQuantidadeTotal() { return quantidadeTotal; }
    public void setQuantidadeTotal(Integer quantidadeTotal) { this.quantidadeTotal = quantidadeTotal; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getAnoPublicacao() { return anoPublicacao; }
    public void setAnoPublicacao(Integer anoPublicacao) { this.anoPublicacao = anoPublicacao; }

    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }

    public String getImagemCapa() { return imagemCapa; }
    public void setImagemCapa(String imagemCapa) { this.imagemCapa = imagemCapa; }

    public Double getMediaAvaliacoes() { return mediaAvaliacoes; }
    public void setMediaAvaliacoes(Double mediaAvaliacoes) { this.mediaAvaliacoes = mediaAvaliacoes; }

    public Long getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(Long totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public boolean isDisponivel() {
        return this.quantidade != null && this.quantidade > 0;
    }

    /**
     * Verifica se há exemplares emprestados
     */
    public boolean hasExemplaresEmprestados() {
        return this.quantidadeTotal != null &&
                this.quantidade != null &&
                this.quantidade < this.quantidadeTotal;
    }

    /**
     * Retorna a quantidade de exemplares emprestados
     */
    public Integer getQuantidadeEmprestada() {
        if (this.quantidadeTotal == null || this.quantidade == null) {
            return 0;
        }
        return this.quantidadeTotal - this.quantidade;
    }

    public void setAnoPublicicao(Integer anoPublicacao) {

    }
}