package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class BibliotecaController {

    private final LivroService livroService;
    private final CategoriaService categoriaService;
    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    public BibliotecaController(LivroService livroService,
                                CategoriaService categoriaService,
                                EmprestimoService emprestimoService,
                                UsuarioService usuarioService,
                                PasswordEncoder passwordEncoder,
                                AvaliacaoService avaliacaoService) {
        this.livroService = livroService;
        this.categoriaService = categoriaService;
        this.emprestimoService = emprestimoService;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.avaliacaoService = avaliacaoService;
    }

    // =============================
    // HOME
    // =============================
    @GetMapping("/")
    public String paginaInicial(Model model, Authentication authentication) {
        List<Livro> livros = livroService.listarTodos();
        model.addAttribute("livros", livros.size() > 8 ? livros.subList(0, 8) : livros);
        model.addAttribute("livrosPopulares", livroService.buscarLivrosPopulares());
        model.addAttribute("totalLivros", livroService.contarTotalLivros());
        model.addAttribute("livrosDisponiveis", livroService.contarLivrosDisponiveis());
        model.addAttribute("livrosEmprestados", livroService.contarLivrosIndisponis());
        configurarAuth(model, authentication);
        return "public/Home";
    }

    // =============================
    // LOGIN
    // =============================
    @GetMapping("/login")
    public String paginaLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            return isAdmin ? "redirect:/admin/painel" : "redirect:/";
        }
        return "Auth/Login";
    }

    // =============================
    // CADASTRO
    // =============================
    @GetMapping("/cadastro")
    public String paginaCadastro(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/";
        model.addAttribute("usuario", new Usuario());
        return "Auth/Cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrarUsuario(@ModelAttribute Usuario usuario,
                                   RedirectAttributes ra) {
        try {
            if (usuarioService.existePorEmail(usuario.getEmail())) {
                ra.addFlashAttribute("erro", "Este e-mail já está cadastrado!");
                return "redirect:/cadastro";
            }

            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            usuario.setPapel("ROLE_USUARIO");
            usuario.setAtivo(true);
            usuarioService.salvar(usuario);

            ra.addFlashAttribute("sucesso", "Cadastro realizado com sucesso! Faça login para continuar.");
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao cadastrar: " + e.getMessage());
            return "redirect:/cadastro";
        }
    }

    // =============================
    // LIVROS
    // =============================
    @GetMapping("/livros")
    public String paginaLivros(Model model,
                               @RequestParam(required = false) String pesquisa,
                               @RequestParam(required = false) Long categoriaId,
                               Authentication auth) {
        List<Livro> livros;
        List<Categoria> categorias = categoriaService.listarTodas();

        if (pesquisa != null && !pesquisa.trim().isEmpty()) {
            livros = livroService.pesquisarLivros(pesquisa);
        } else if (categoriaId != null) {
            livros = livroService.buscarPorCategoria(categoriaId);
        } else {
            livros = livroService.listarTodos();
        }

        model.addAttribute("livros", livros);
        model.addAttribute("categorias", categorias);
        model.addAttribute("livrosPopulares", livroService.buscarLivrosPopulares());
        configurarAuth(model, auth);
        return "public/livros/livros";
    }

    // =============================
    // DETALHES DO LIVRO
    // =============================
    @GetMapping("/livros/detalhes/{id}")
    public String detalhesLivro(@PathVariable Long id, Model model, Authentication auth) {
        Livro livro = livroService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));
        model.addAttribute("livro", livro);

        if (livro.getCategoria() != null) {
            model.addAttribute("livrosRelacionados",
                    livroService.buscarLivrosRelacionados(livro.getCategoria().getId(), id));
        } else {
            model.addAttribute("livrosRelacionados", List.of());
        }

        model.addAttribute("avaliacoes", avaliacaoService.buscarAvaliacoesPorLivro(id));
        model.addAttribute("mediaAvaliacoes", avaliacaoService.calcularMediaAvaliacoes(id));
        configurarAuth(model, auth);
        return "public/livros/detalhes-livro";
    }

    // ==================================
    // ALUGAR LIVRO (MÉTODO ADICIONADO)
    // ==================================
    @PostMapping("/livros/alugar/{id}")
    public String alugarLivro(@PathVariable("id") Long livroId,
                              Authentication authentication,
                              RedirectAttributes ra) {

        // 1. Verifica se o usuário está logado
        if (authentication == null || !authentication.isAuthenticated()) {
            ra.addFlashAttribute("erro", "Você precisa estar logado para alugar um livro.");
            return "redirect:/login";
        }

        try {
            // 2. Pega o usuário logado
            String email = authentication.getName();
            Usuario usuario = usuarioService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // 3. Tenta realizar o empréstimo (você precisará ter esse método no seu service)
            emprestimoService.realizarEmprestimo(livroId, usuario.getId());

            ra.addFlashAttribute("sucesso", "Livro alugado com sucesso!");

            // 4. Redireciona para o perfil, onde o usuário pode ver seus empréstimos
            // Use "/perfil/meu" (do seu PerfilController)
            return "redirect:/perfil/meu";

        } catch (Exception e) {
            // Em caso de erro (ex: livro indisponível), exibe o erro
            ra.addFlashAttribute("erro", "Erro ao alugar livro: " + e.getMessage());
            // Redireciona de volta para a página do livro
            return "redirect:/livros/detalhes/" + livroId;
        }
    }

    // =============================
    // PERFIL DO USUÁRIO
    // =============================
    @GetMapping("/perfil")
    public String perfilUsuario(@AuthenticationPrincipal Usuario usuarioLogado,
                                Model model,
                                RedirectAttributes ra) {
        if (usuarioLogado == null) {
            ra.addFlashAttribute("erro", "Você precisa estar logado para acessar seu perfil.");
            return "redirect:/login";
        }

        var emprestimosAtivos = emprestimoService.buscarEmprestimosAtivos(usuarioLogado.getId());
        var historico = emprestimoService.buscarHistorico(usuarioLogado.getId());

        model.addAttribute("livrosAlugados", emprestimosAtivos);
        model.addAttribute("historico", historico);
        model.addAttribute("usuario", usuarioLogado);
        return "Usuario/Perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@ModelAttribute Usuario usuarioAtualizado,
                                  @AuthenticationPrincipal Usuario usuarioLogado,
                                  RedirectAttributes ra) {
        try {
            usuarioService.atualizarDados(usuarioLogado.getId(), usuarioAtualizado);
            ra.addFlashAttribute("sucesso", "Dados atualizados com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar: " + e.getMessage());
        }
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/excluir")
    public String excluirConta(@AuthenticationPrincipal Usuario usuarioLogado,
                               RedirectAttributes ra,
                               HttpServletRequest request) {
        if (usuarioLogado == null) {
            ra.addFlashAttribute("erro", "Você precisa estar logado para excluir sua conta.");
            return "redirect:/login";
        }

        try {
            usuarioService.excluirUsuario(usuarioLogado.getId());
            request.logout();
            ra.addFlashAttribute("sucesso", "Sua conta foi excluída com sucesso.");
            return "redirect:/";
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao excluir conta: " + e.getMessage());
            return "redirect:/perfil";
        }
    }

    // =============================
    // CONFIGURA AUTENTICAÇÃO
    // =============================
    private void configurarAuth(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("usuarioLogado", true);
            model.addAttribute("nomeUsuario", auth.getName());
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
        } else {
            model.addAttribute("usuarioLogado", false);
        }
    }
}