package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LivroRepository livroRepository;

    public boolean existePorEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }

    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public void atualizarDados(Long id, Usuario usuarioAtualizado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuarioRepository.delete(usuario);
    }

    public long contarTotalUsuarios() {
        return usuarioRepository.count();
    }

    public long contarUsuariosAtivos() {
        return usuarioRepository.countByAtivo(true);
    }

    // =============================
    // FUNÇÕES DE LISTA DE DESEJOS
    // =============================
    public void adicionarDesejo(Usuario usuario, Long livroId) {
        if (usuario == null) return;
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));
        usuario.getListaDesejos().add(livro);
        usuarioRepository.save(usuario);
    }

    public void removerDesejo(Usuario usuario, Long livroId) {
        if (usuario == null) return;
        usuario.getListaDesejos().removeIf(l -> Objects.equals(l.getId(), livroId));
        usuarioRepository.save(usuario);
    }

    public List<Livro> buscarDesejos(Usuario usuario) {
        return new ArrayList<>(usuario.getListaDesejos());
    }

    @Transactional
    public void alterarStatus(Long id, boolean novoStatus) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuario.setAtivo(novoStatus);
        usuarioRepository.save(usuario);
    }
}
