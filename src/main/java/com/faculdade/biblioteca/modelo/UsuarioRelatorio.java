package com.faculdade.biblioteca.modelo;

public class UsuarioRelatorio {

    private String nome;
    private String email;
    private Long totalEmprestimos;
    private Double taxaPontualidade;

    public UsuarioRelatorio() {}

    public UsuarioRelatorio(String nome, String email, Long totalEmprestimos, Double taxaPontualidade) {
        this.nome = nome;
        this.email = email;
        this.totalEmprestimos = totalEmprestimos;
        this.taxaPontualidade = taxaPontualidade;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getTotalEmprestimos() { return totalEmprestimos; }
    public void setTotalEmprestimos(Long totalEmprestimos) { this.totalEmprestimos = totalEmprestimos; }

    public Double getTaxaPontualidade() { return taxaPontualidade; }
    public void setTaxaPontualidade(Double taxaPontualidade) { this.taxaPontualidade = taxaPontualidade; }
}