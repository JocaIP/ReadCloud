package com.faculdade.biblioteca.dto;

public class GraficoLivroDTO {

    private String titulo;
    private Long total;

    public GraficoLivroDTO(String titulo, Long total) {
        this.titulo = titulo;
        this.total = total;
    }

    public String getTitulo() {
        return titulo;
    }

    public Long getTotal() {
        return total;
    }
}
