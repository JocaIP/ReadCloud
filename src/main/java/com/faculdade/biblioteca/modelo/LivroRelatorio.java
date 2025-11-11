package com.faculdade.biblioteca.modelo;

public class LivroRelatorio {

    private String titulo;
    private String autor;
    private String categoria;
    private Long totalEmprestimos;
    private String localMaisFrequente;
    private Double taxaRetorno;

    public LivroRelatorio() {}

    public LivroRelatorio(String titulo, String autor, String categoria,
                          Long totalEmprestimos, String localMaisFrequente, Double taxaRetorno) {
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.totalEmprestimos = totalEmprestimos;
        this.localMaisFrequente = localMaisFrequente;
        this.taxaRetorno = taxaRetorno;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Long getTotalEmprestimos() { return totalEmprestimos; }
    public void setTotalEmprestimos(Long totalEmprestimos) { this.totalEmprestimos = totalEmprestimos; }

    public String getLocalMaisFrequente() { return localMaisFrequente; }
    public void setLocalMaisFrequente(String localMaisFrequente) { this.localMaisFrequente = localMaisFrequente; }

    public Double getTaxaRetorno() { return taxaRetorno; }
    public void setTaxaRetorno(Double taxaRetorno) { this.taxaRetorno = taxaRetorno; }
}