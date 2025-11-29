package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import com.faculdade.biblioteca.modelo.CustomUserDetails;

import java.util.*;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    // ============================================================
    // 🔍 VERIFICAÇÕES
    // ============================================================
    public boolean existePorEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }

    // ============================================================
    // 💾 SALVAR / CADASTRAR USUÁRIO
    // ============================================================
    public Usuario salvar(Usuario usuario) {

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new RuntimeException("O email não pode ser vazio.");
        }

        if (existePorEmail(usuario.getEmail())) {
            throw new RuntimeException("Já existe um usuário com este email.");
        }

        usuario.setAtivo(true); // padrão
        return usuarioRepository.save(usuario);
    }

    // ============================================================
    // 🔎 BUSCAS
    // ============================================================
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public long contarTotalUsuarios() {
        return usuarioRepository.count();
    }

    public long contarUsuariosAtivos() {
        return usuarioRepository.countByAtivo(true);
    }

    // ============================================================
    // ✏ ATUALIZAR DADOS DO USUÁRIO
    // ============================================================
    @Transactional
    public void atualizarDados(Long id, Usuario dadosNovos) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Evitar email duplicado
        if (!usuario.getEmail().equals(dadosNovos.getEmail())) {
            if (existePorEmail(dadosNovos.getEmail())) {
                throw new RuntimeException("Já existe outro usuário com este email.");
            }
        }

        usuario.setNome(dadosNovos.getNome());
        usuario.setEmail(dadosNovos.getEmail());

        if (dadosNovos.getTelefone() != null) {
            usuario.setTelefone(dadosNovos.getTelefone());
        }

        usuarioRepository.save(usuario);
    }

    // ============================================================
    // 🗑 EXCLUIR USUÁRIO (Seguro)
    // ============================================================
    @Transactional
    public void excluirUsuario(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        long emprestimosAtivos = emprestimoRepository.countEmprestimosAtivosPorUsuario(id);

        if (emprestimosAtivos > 0) {
            throw new RuntimeException(
                    "Não é possível excluir o usuário. Ele possui empréstimos ativos."
            );
        }

        usuarioRepository.delete(usuario);
    }

    // ============================================================
    // 🔄 ALTERAR STATUS (Ativo / Inativo)
    // ============================================================
    @Transactional
    public void alterarStatus(Long id, boolean novoStatus) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        usuario.setAtivo(novoStatus);
        usuarioRepository.save(usuario);
    }

    // ============================================================
    // ❤️ LISTA DE DESEJOS
    // ============================================================
    @Transactional
    public void adicionarDesejo(Usuario usuario, Long livroId) {

        if (usuario == null) {
            throw new RuntimeException("Usuário inválido.");
        }

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        if (!usuario.getListaDesejos().contains(livro)) {
            usuario.getListaDesejos().add(livro);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void removerDesejo(Usuario usuario, Long livroId) {

        if (usuario == null) {
            throw new RuntimeException("Usuário inválido.");
        }

        usuario.getListaDesejos()
                .removeIf(l -> Objects.equals(l.getId(), livroId));

        usuarioRepository.save(usuario);
    }

    public List<Livro> buscarDesejos(Usuario usuario) {
        return new ArrayList<>(usuario.getListaDesejos());
    }

    // ============================================================
    // 🆕 LISTAR RECENTES
    // ============================================================
    public List<Usuario> listarRecentes(int limite) {
        List<Usuario> todos = usuarioRepository.findAll();
        todos.sort(Comparator.comparing(Usuario::getId).reversed());
        return todos.stream().limit(limite).toList();
    }
    public List<Livro> listarDesejosDoUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return new ArrayList<>(usuario.getListaDesejos());
    }

    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }

    public Usuario getUsuarioLogado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomUserDetails cud) {
            return usuarioRepository.findById(cud.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        }

        throw new RuntimeException("Usuário não autenticado");
    }

}
