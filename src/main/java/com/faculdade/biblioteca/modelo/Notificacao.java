package com.faculdade.biblioteca.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String mensagem;

    private boolean lida = false;

    private LocalDateTime data = LocalDateTime.now();

    public Notificacao() {}

    public Notificacao(Usuario usuario, String mensagem) {
        this.usuario = usuario;
        this.mensagem = mensagem;
        this.lida = false;
        this.data = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getData() { return data; }

    public void setLida(boolean lida) { this.lida = lida; }
}
