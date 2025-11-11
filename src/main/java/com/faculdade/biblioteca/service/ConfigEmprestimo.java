package com.faculdade.biblioteca.service;

import java.math.BigDecimal;

public class ConfigEmprestimo {

    private int prazoDevolucaoDias;
    private BigDecimal multaDiaria;
    private int limiteLivrosPorUsuario;

    public ConfigEmprestimo() {}

    public ConfigEmprestimo(int prazoDevolucaoDias, BigDecimal multaDiaria, int limiteLivrosPorUsuario) {
        this.prazoDevolucaoDias = prazoDevolucaoDias;
        this.multaDiaria = multaDiaria;
        this.limiteLivrosPorUsuario = limiteLivrosPorUsuario;
    }

    public int getPrazoDevolucaoDias() { return prazoDevolucaoDias; }
    public void setPrazoDevolucaoDias(int prazoDevolucaoDias) { this.prazoDevolucaoDias = prazoDevolucaoDias; }

    public BigDecimal getMultaDiaria() { return multaDiaria; }
    public void setMultaDiaria(BigDecimal multaDiaria) { this.multaDiaria = multaDiaria; }

    public int getLimiteLivrosPorUsuario() { return limiteLivrosPorUsuario; }
    public void setLimiteLivrosPorUsuario(int limiteLivrosPorUsuario) { this.limiteLivrosPorUsuario = limiteLivrosPorUsuario; }
}
