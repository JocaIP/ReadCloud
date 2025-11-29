package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Notificacao;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.repository.NotificacaoRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // --------------------------
    // ENVIAR NOTIFICAÇÃO
    // --------------------------
    public void enviar(Usuario usuario, String mensagem) {
        Notificacao n = new Notificacao(usuario, mensagem);
        notificacaoRepository.save(n);
    }

    // --------------------------
    // BUSCAR NÃO LIDAS
    // --------------------------
    public List<Notificacao> buscarNaoLidas(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return notificacaoRepository.findByUsuarioAndLidaFalse(u);
    }

    // --------------------------
    // CONTAR NÃO LIDAS
    // --------------------------
    public long contarNaoLidas(Usuario usuario) {
        return notificacaoRepository.countByUsuarioAndLidaFalse(usuario);
    }

    // --------------------------
    // MARCAR TODAS COMO LIDAS
    // --------------------------
    public void marcarTodasComoLidas(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Notificacao> lista = notificacaoRepository.findByUsuarioAndLidaFalse(u);

        for (Notificacao n : lista) {
            n.setLida(true);
        }

        notificacaoRepository.saveAll(lista);
    }

    // --------------------------
    // LISTAR TODAS
    // --------------------------
    public List<Notificacao> listarTodas(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return notificacaoRepository.findByUsuarioOrderByDataDesc(u);
    }
}
