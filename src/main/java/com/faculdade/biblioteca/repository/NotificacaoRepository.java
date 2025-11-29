package com.faculdade.biblioteca.repository;

import com.faculdade.biblioteca.modelo.Notificacao;
import com.faculdade.biblioteca.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioAndLidaFalse(Usuario usuario);

    List<Notificacao> findByUsuarioOrderByDataDesc(Usuario usuario);

    long countByUsuarioAndLidaFalse(Usuario usuario);
}
