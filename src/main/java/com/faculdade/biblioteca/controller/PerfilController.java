package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.Notificacao;
import com.faculdade.biblioteca.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final LivroService livroService;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    private NotificacaoService notificacaoService;

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


    // =====================================================================
    // DASHBOARD FINAL — COM NOTIFICAÇÕES
    // =====================================================================
    @GetMapping
    public String dashboard(Model model, Authentication authentication) {

        Usuario usuario = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Long id = usuario.getId();

        // Dados do perfil
        model.addAttribute("usuario", usuario);
        model.addAttribute("emprestimosAtivos", emprestimoService.listarAtivosPorUsuario(id));
        model.addAttribute("desejos", usuarioService.listarDesejosDoUsuario(id));
        model.addAttribute("historico", emprestimoService.listarHistoricoPorUsuario(id));
        model.addAttribute("atrasos", emprestimoService.listarAtrasosPorUsuario(id));
        model.addAttribute("avaliacoes", avaliacaoService.buscarAvaliacoesPorUsuario(id));

        // Totais
        model.addAttribute("totalEmprestimosAtivos", emprestimoService.listarAtivosPorUsuario(id).size());
        model.addAttribute("totalDesejos", usuarioService.listarDesejosDoUsuario(id).size());
        model.addAttribute("totalAtrasos", emprestimoService.listarAtrasosPorUsuario(id).size());
        model.addAttribute("totalAvaliacoes", avaliacaoService.buscarAvaliacoesPorUsuario(id).size());

        // 🔔 Notificações
        List<Notificacao> notificacoes = notificacaoService.buscarNaoLidas(id);
        model.addAttribute("notificacoes", notificacoes);

        // marcar como lidas APÓS exibir
        if (!notificacoes.isEmpty()) {
            notificacaoService.marcarTodasComoLidas(id);
        }

        return "Perfil/dashboard";
    }


    // =====================================================================
    // LISTA DE DESEJOS
    // =====================================================================
    @GetMapping("/desejos")
    public String verDesejos(Model model, Authentication authentication) {

        Usuario usuario = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("desejos", usuarioService.listarDesejosDoUsuario(usuario.getId()));

        return "Perfil/lista-desejos";
    }

    @PostMapping("/desejos/adicionar/{livroId}")
    public String adicionarDesejo(@PathVariable Long livroId,
                                  Authentication authentication,
                                  RedirectAttributes ra) {

        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            usuarioService.adicionarDesejo(usuario, livroId);
            ra.addFlashAttribute("sucesso", "Livro adicionado à lista de desejos!");

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/perfil/desejos";
    }


    @PostMapping("/desejos/remover/{livroId}")
    public String removerDesejo(@PathVariable Long livroId,
                                Authentication authentication,
                                RedirectAttributes ra) {

        try {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            usuarioService.removerDesejo(usuario, livroId);
            ra.addFlashAttribute("sucesso", "Livro removido!");

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/perfil/desejos";
    }


    // =====================================================================
    // HISTÓRICO
    // =====================================================================
    @GetMapping("/historico")
    public String historico(Model model, Authentication authentication) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("historico",
                emprestimoService.listarHistoricoPorUsuario(usuario.getId()));

        return "Perfil/historico-emprestimos";
    }


    // =====================================================================
    // AVALIAÇÕES
    // =====================================================================
    @GetMapping("/avaliacoes")
    public String minhasAvaliacoes(Model model, Authentication authentication) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("avaliacoes",
                avaliacaoService.buscarAvaliacoesPorUsuario(usuario.getId()));

        return "Perfil/minhas-avaliacoes";
    }


    // =====================================================================
    // DEVOLVER LIVRO
    // =====================================================================
    @PostMapping("/devolver/{emprestimoId}")
    public String devolverLivro(@PathVariable Long emprestimoId,
                                Authentication authentication,
                                RedirectAttributes ra) {

        try {

            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            String msg = emprestimoService.devolverLivro(emprestimoId, usuario);
            ra.addFlashAttribute("sucesso", msg);

        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/perfil";
    }

}
