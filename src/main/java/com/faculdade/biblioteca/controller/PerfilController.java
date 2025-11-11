package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.EmprestimoService;
import com.faculdade.biblioteca.service.UsuarioService;
import com.faculdade.biblioteca.service.LivroService;
import com.faculdade.biblioteca.service.AvaliacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final LivroService livroService;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    public PerfilController(EmprestimoService emprestimoService,
                            UsuarioService usuarioService,
                            LivroService livroService,
                            AvaliacaoService avaliacaoService) {
        this.emprestimoService = emprestimoService;
        this.usuarioService = usuarioService;
        this.livroService = livroService;
        this.avaliacaoService = avaliacaoService;
    }

    // ==============================
    // DASHBOARD DO PERFIL (principal)
    // ==============================
    @GetMapping("/meu")
    public String meuPerfil(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuarioLogado = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Long usuarioId = usuarioLogado.getId();

        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("emprestimos", emprestimoService.buscarEmprestimosAtivos(usuarioId));
        model.addAttribute("desejos", usuarioService.buscarDesejos(usuarioLogado));
        model.addAttribute("historico", emprestimoService.buscarHistorico(usuarioId));
        model.addAttribute("atrasos", emprestimoService.buscarAtrasosPorUsuario(usuarioId));
        model.addAttribute("avaliacoes", avaliacaoService.buscarAvaliacoesPorUsuario(usuarioId));

        model.addAttribute("totalEmprestimosAtivos", emprestimoService.buscarEmprestimosAtivos(usuarioId).size());
        model.addAttribute("totalDesejos", usuarioService.buscarDesejos(usuarioLogado).size());
        model.addAttribute("totalAtrasos", emprestimoService.buscarAtrasosPorUsuario(usuarioId).size());
        model.addAttribute("totalAvaliacoes", avaliacaoService.buscarAvaliacoesPorUsuario(usuarioId).size());

        return "Perfil/dashboard";
    }

    // ==============================
    // LISTA DE DESEJOS
    // ==============================
    @GetMapping("/desejos")
    public String verDesejos(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("desejos", usuarioService.buscarDesejos(usuario));
        return "Perfil/lista-desejos";
    }

    @PostMapping("/desejos/adicionar/{id}")
    public String adicionarDesejo(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            String email = authentication.getName();
            Usuario usuario = usuarioService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            usuarioService.adicionarDesejo(usuario, id);
            ra.addFlashAttribute("sucesso", "Livro adicionado à lista de desejos!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao adicionar desejo: " + e.getMessage());
        }
        return "redirect:/perfil/desejos";
    }

    @PostMapping("/desejos/remover/{id}")
    public String removerDesejo(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            String email = authentication.getName();
            Usuario usuario = usuarioService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            usuarioService.removerDesejo(usuario, id);
            ra.addFlashAttribute("sucesso", "Livro removido da lista de desejos!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover desejo: " + e.getMessage());
        }
        return "redirect:/perfil/desejos";
    }

    // ==============================
    // HISTÓRICO DE EMPRÉSTIMOS
    // ==============================
    @GetMapping("/historico")
    public String historico(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("historico", emprestimoService.buscarHistorico(usuario.getId()));
        return "Perfil/historico-emprestimos";
    }

    // ==============================
    // AVALIAÇÕES
    // ==============================
    @GetMapping("/avaliacoes")
    public String minhasAvaliacoes(Model model, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("avaliacoes", avaliacaoService.buscarAvaliacoesPorUsuario(usuario.getId()));
        return "Perfil/minhas-avaliacoes";
    }

    // ==============================
    // DEVOLVER LIVRO
    // ==============================
    @PostMapping("/devolver/{id}")
    public String devolver(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            String email = authentication.getName();
            Usuario usuario = usuarioService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            emprestimoService.devolverLivro(id, usuario);
            ra.addFlashAttribute("sucesso", "Livro devolvido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/perfil/meu";
    }
}
