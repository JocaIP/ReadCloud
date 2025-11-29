package com.faculdade.biblioteca.controller;
import com.faculdade.biblioteca.service.NotificacaoService;
import com.faculdade.biblioteca.modelo.Notificacao;
import com.faculdade.biblioteca.modelo.Livro;
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

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BibliotecaController {

    private final LivroService livroService;
    private final CategoriaService categoriaService;
    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    private NotificacaoService notificacaoService;
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


    /* ============================================================
       HOME
    ============================================================ */
    @GetMapping("/")
    public String paginaInicial(Model model, Authentication authentication) {

        List<Livro> livros = livroService.listarTodos();
        model.addAttribute("livros", livros.size() > 8 ? livros.subList(0, 8) : livros);

        model.addAttribute("livrosPopulares", livroService.buscarLivrosPopulares());
        model.addAttribute("totalLivros", livroService.contarTotalLivros());
        model.addAttribute("livrosDisponiveis", livroService.contarLivrosDisponiveis());
        model.addAttribute("livrosEmprestados", livroService.contarLivrosIndisponiveis());

        configurarAuth(model, authentication);
        return "public/Home";
    }


    /* ============================================================
       LOGIN E CADASTRO
    ============================================================ */
    @GetMapping("/login")
    public String paginaLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities()
                    .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            return isAdmin ? "redirect:/admin/painel" : "redirect:/";
        }
        return "Auth/Login";
    }

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
            if (usuario.getNome() == null || usuario.getNome().isBlank()) {
                ra.addFlashAttribute("erro", "Nome é obrigatório");
                return "redirect:/cadastro";
            }

            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                ra.addFlashAttribute("erro", "Email é obrigatório");
                return "redirect:/cadastro";
            }

            if (usuario.getSenha() == null || usuario.getSenha().length() < 6) {
                ra.addFlashAttribute("erro", "Senha deve ter ao menos 6 caracteres");
                return "redirect:/cadastro";
            }

            if (usuarioService.existePorEmail(usuario.getEmail())) {
                ra.addFlashAttribute("erro", "Este email já está cadastrado");
                return "redirect:/cadastro";
            }

            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            usuario.setPapel("ROLE_USUARIO");
            usuario.setAtivo(true);

            usuarioService.salvar(usuario);

            ra.addFlashAttribute("sucesso", "Cadastro concluído! Faça login.");
            return "redirect:/login";

        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao cadastrar: " + e.getMessage());
            return "redirect:/cadastro";
        }
    }


    /* ============================================================
       LISTA DE LIVROS
    ============================================================ */
    @GetMapping("/livros")
    public String paginaLivros(Model model,
                               @RequestParam(required = false) String pesquisa,
                               @RequestParam(required = false) Long categoriaId,
                               @RequestParam(required = false) String disponibilidade,
                               Authentication auth) {

        List<Livro> livros = livroService.filtrarLivros(
                (pesquisa != null && !pesquisa.isBlank()) ? pesquisa.trim() : null,
                categoriaId,
                disponibilidade
        );

        model.addAttribute("livros", livros);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("termoPesquisa", pesquisa);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("disponibilidade", disponibilidade);

        configurarAuth(model, auth);
        return "public/livros/livros";
    }


    /* ============================================================
       BUSCA AVANÇADA
    ============================================================ */
    @GetMapping("/livros/busca")
    public String buscaAvancada(@RequestParam(required = false) String termo,
                                Model model, Authentication auth) {

        List<Livro> resultados = (termo != null && !termo.isBlank())
                ? livroService.buscaInteligente(termo.trim())
                : livroService.listarTodos();

        model.addAttribute("livros", resultados);
        model.addAttribute("termoPesquisa", termo);
        model.addAttribute("categorias", categoriaService.listarTodas());

        configurarAuth(model, auth);
        return "public/livros/livros";
    }


    /* ============================================================
       FILTROS COMPLETOS (formulário)
    ============================================================ */
    @GetMapping("/livros/filtros")
    public String filtrosCompletos(@RequestParam(required = false) String titulo,
                                   @RequestParam(required = false) String autor,
                                   @RequestParam(required = false) String editora,
                                   @RequestParam(required = false) Integer anoMin,
                                   @RequestParam(required = false) Integer anoMax,
                                   @RequestParam(required = false) Boolean disponivel,
                                   Model model, Authentication auth) {

        List<Livro> livros;

        if (disponivel != null) {
            livros = livroService.buscarPorStatus(disponivel);
        } else {
            livros = livroService.listarTodos();
        }


        livros = livros.stream()
                .filter(l -> titulo == null || l.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                .filter(l -> autor == null || l.getAutor().toLowerCase().contains(autor.toLowerCase()))
                .filter(l -> editora == null || l.getEditora().toLowerCase().contains(editora.toLowerCase()))
                .filter(l -> anoMin == null || l.getAnoPublicacao() >= anoMin)
                .filter(l -> anoMax == null || l.getAnoPublicacao() <= anoMax)
                .collect(Collectors.toList());

        model.addAttribute("livros", livros);
        model.addAttribute("categorias", categoriaService.listarTodas());

        configurarAuth(model, auth);
        return "public/livros/livros";
    }


    /* ============================================================
       DETALHES DO LIVRO
    ============================================================ */
    @GetMapping("/livros/detalhes/{id}")
    public String detalhesLivro(@PathVariable Long id, Model model, Authentication auth) {

        Livro livro = livroService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        model.addAttribute("livro", livro);
        model.addAttribute("avaliacoes", avaliacaoService.buscarAvaliacoesPorLivro(id));
        model.addAttribute("mediaAvaliacoes", avaliacaoService.calcularMediaAvaliacoes(id));

        if (livro.getCategoria() != null) {
            model.addAttribute("livrosRelacionados",
                    livroService.buscarLivrosRelacionados(livro.getCategoria().getId(), id));
        } else {
            model.addAttribute("livrosRelacionados", List.of());
        }

        configurarAuth(model, auth);
        return "public/livros/detalhes-livro";
    }


    /* ============================================================
       FAVORITAR LIVRO
    ============================================================ */
    @PostMapping("/livros/desejar/{id}")
    public String desejarLivro(@PathVariable Long id,
                               Authentication auth,
                               RedirectAttributes ra) {

        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("erro", "Faça login para favoritar");
            return "redirect:/login";
        }

        try {
            Usuario usuario = usuarioService.buscarPorEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            boolean jaExiste = usuarioService.buscarDesejos(usuario)
                    .stream().anyMatch(l -> l.getId().equals(id));

            if (jaExiste) {
                ra.addFlashAttribute("erro", "Este livro já está nos favoritos");
            } else {
                usuarioService.adicionarDesejo(usuario, id);
                ra.addFlashAttribute("sucesso", "Livro adicionado aos favoritos");
            }

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/livros/detalhes/" + id;
    }


    /* ============================================================
       ATUALIZAR PERFIL
    ============================================================ */
    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(@ModelAttribute Usuario usuarioAtualizado,
                                  @AuthenticationPrincipal Usuario usuarioLogado,
                                  RedirectAttributes ra) {

        try {
            usuarioService.atualizarDados(usuarioLogado.getId(), usuarioAtualizado);
            ra.addFlashAttribute("sucesso", "Perfil atualizado!");

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/perfil";
    }

    @PostMapping("/perfil/excluir")
    public String excluirConta(@AuthenticationPrincipal Usuario usuarioLogado,
                               RedirectAttributes ra,
                               HttpServletRequest request) {

        try {
            usuarioService.excluirUsuario(usuarioLogado.getId());
            request.logout();
            ra.addFlashAttribute("sucesso", "Conta excluída!");
            return "redirect:/";

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/perfil";
        }
    }


    /* ============================================================
       MÉTODO AUXILIAR
    ============================================================ */
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
    /* ============================================================
   PÁGINA SOBRE / CONTATO
============================================================ */
    @GetMapping("/sobre")
    public String sobre() {
        return "public/sobre";
    }

    @GetMapping("/contato")
    public String contato() {
        return "public/contato";
    }

}
