package com.faculdade.biblioteca.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================
    // RELACIONAMENTOS
    // ========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore // evita loop infinito na serialização
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id", nullable = false)
    @JsonIgnore
    private Livro livro;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id", unique = true)
    @JsonIgnore
    private Emprestimo emprestimo;

    // ========================
    // CAMPOS
    // ========================
    @Column(nullable = false)
    private int rating; // nota (1–5 estrelas)

    @Column(length = 1000)
    private String comentario;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;

    @Column(nullable = false)
    private boolean ativa = true;

    // ========================
    // CONSTRUTORES
    // ========================
    public Avaliacao() {}

    public Avaliacao(Usuario usuario, Livro livro, int rating, String comentario) {
        this.usuario = usuario;
        this.livro = livro;
        this.rating = rating;
        this.comentario = comentario;
        this.dataAvaliacao = LocalDateTime.now();
        this.ativa = true;
    }

    // ========================
    // MÉTODOS AUXILIARES
    // ========================
    @PrePersist
    public void prePersist() {
        if (this.dataAvaliacao == null) {
            this.dataAvaliacao = LocalDateTime.now();
        }
    }

    // ========================
    // GETTERS E SETTERS
    // ========================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Livro getLivro() { return livro; }
    public void setLivro(Livro livro) { this.livro = livro; }

    public Emprestimo getEmprestimo() { return emprestimo; }
    public void setEmprestimo(Emprestimo emprestimo) { this.emprestimo = emprestimo; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
