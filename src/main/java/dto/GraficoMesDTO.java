package com.faculdade.biblioteca.dto;

public class GraficoMesDTO {
    private String mes;
    private Long total;

    public GraficoMesDTO(String mes, Long total) {
        this.mes = mes;
        this.total = total;
    }

    public String getMes() { return mes; }
    public Long getTotal() { return total; }
}