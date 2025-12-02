package com.faculdade.biblioteca.dto;

public class GraficoBaixaSaidaDTO {
    private String titulo;
    private Long totalEmprestimos;

    public GraficoBaixaSaidaDTO(String titulo, Long totalEmprestimos) {
        this.titulo = titulo;
        this.totalEmprestimos = totalEmprestimos;
    }

    public String getTitulo() { return titulo; }
    public Long getTotalEmprestimos() { return totalEmprestimos; }
}