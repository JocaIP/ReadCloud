package com.faculdade.biblioteca.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "emprestimos")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime dataEmprestimo;

    @Column(nullable = false)
    private LocalDateTime dataDevolucao;

    private LocalDateTime dataDevolucaoReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEmprestimo status;

    private BigDecimal multaAcumulada = BigDecimal.ZERO;

    private Integer renovacoes = 0;

    // 🔹 ADICIONE ESTE CAMPO PARA COMPATIBILIDADE
    @Column(nullable = false)
    private Boolean devolvido = false;

    // Construtores
    public Emprestimo() {}

    public Emprestimo(Livro livro, Usuario usuario, LocalDateTime dataEmprestimo, LocalDateTime dataDevolucao) {
        this.livro = livro;
        this.usuario = usuario;
        this.dataEmprestimo = dataEmprestimo;
        this.dataDevolucao = dataDevolucao;
        this.status = StatusEmprestimo.ATIVO;
        this.devolvido = false;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Livro getLivro() { return livro; }
    public void setLivro(Livro livro) { this.livro = livro; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public void setDataEmprestimo(LocalDateTime dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }

    public LocalDateTime getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDateTime dataDevolucao) { this.dataDevolucao = dataDevolucao; }

    public LocalDateTime getDataDevolucaoReal() { return dataDevolucaoReal; }
    public void setDataDevolucaoReal(LocalDateTime dataDevolucaoReal) { this.dataDevolucaoReal = dataDevolucaoReal; }

    public StatusEmprestimo getStatus() { return status; }
    public void setStatus(StatusEmprestimo status) { this.status = status; }

    public BigDecimal getMultaAcumulada() { return multaAcumulada; }
    public void setMultaAcumulada(BigDecimal multaAcumulada) { this.multaAcumulada = multaAcumulada; }

    public Integer getRenovacoes() { return renovacoes; }
    public void setRenovacoes(Integer renovacoes) { this.renovacoes = renovacoes; }

    // 🔹 GETTER E SETTER PARA devolvido
    public Boolean getDevolvido() { return devolvido; }
    public void setDevolvido(Boolean devolvido) { this.devolvido = devolvido; }

    // Método para compatibilidade
    public boolean isDevolvido() {
        return devolvido != null && devolvido;
    }

    public void setDevolvido(boolean devolvido) {
        this.devolvido = devolvido;
    }

    // Métodos auxiliares para o front-end
    @Transient
    public int getDiasRestantes() {
        if (status != StatusEmprestimo.ATIVO) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dataDevolucao);
    }

    @Transient
    public int getDiasAtraso() {
        if (status != StatusEmprestimo.ATRASADO) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dataDevolucao, LocalDateTime.now());
    }

    @Transient
    public boolean isPodeRenovar() {
        return status == StatusEmprestimo.ATIVO && renovacoes < 3 && getDiasRestantes() > 2;
    }

    public void setStatus(boolean b) {
    }
}