package com.faculdade.biblioteca.dto;

public class GraficoUsuarioDTO {
    private String nome;
    private Long total;

    public GraficoUsuarioDTO(String nome, Long total) {
        this.nome = nome;
        this.total = total;
    }

    // Getters
    public String getNome() { return nome; }
    public Long getTotal() { return total; }
}