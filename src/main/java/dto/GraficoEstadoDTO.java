package com.faculdade.biblioteca.dto;

public class GraficoEstadoDTO {

    private String estado;
    private Long total;

    public GraficoEstadoDTO(String estado, Long total) {
        this.estado = estado;
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public Long getTotal() {
        return total;
    }
}
