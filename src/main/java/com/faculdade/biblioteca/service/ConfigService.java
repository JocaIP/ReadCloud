package com.faculdade.biblioteca.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ConfigService {

    public ConfigEmprestimo obterConfiguracoes() {
        return new ConfigEmprestimo(
                14,
                new BigDecimal("2.00"),
                3
        );
    }

    public void salvarConfiguracoes(int prazoDevolucaoDias, BigDecimal multaDiaria, int limiteLivros) {
        System.out.println("Configurações salvas: " + prazoDevolucaoDias + " dias, " +
                multaDiaria + " multa diária, " + limiteLivros + " livros por usuário");
    }
}
