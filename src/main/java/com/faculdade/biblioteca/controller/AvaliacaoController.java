package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Avaliacao;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.AvaliacaoService;
import com.faculdade.biblioteca.service.LivroService;
import com.faculdade.biblioteca.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/livros")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;
    @Autowired
    private LivroService livroService;
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/avaliar/{livroId}")
    public String avaliarLivro(@PathVariable Long livroId,
                               @RequestParam Integer rating,
                               @RequestParam String comentario,
                               Authentication authentication,
                               RedirectAttributes ra) {
        try {

            if (authentication == null) {
                ra.addFlashAttribute("erro", "Você precisa estar logado para avaliar.");
                return "redirect:/login";
            }

            String email = authentication.getName();
            Usuario usuario = usuarioService.buscarPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            Livro livro = livroService.buscarPorId(livroId)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

            if (avaliacaoService.usuarioJaAvaliouLivro(usuario.getId(), livroId)) {
                ra.addFlashAttribute("erro", "Você já avaliou este livro.");
                return "redirect:/livros/detalhes/" + livroId;
            }

            if (rating < 1 || rating > 5) {
                ra.addFlashAttribute("erro", "Selecione uma avaliação de 1 a 5 estrelas.");
                return "redirect:/livros/detalhes/" + livroId;
            }

            if (comentario.trim().isEmpty()) {
                ra.addFlashAttribute("erro", "Escreva um comentário antes de enviar.");
                return "redirect:/livros/detalhes/" + livroId;
            }

            Avaliacao avaliacao = new Avaliacao(usuario, livro, rating, comentario.trim());
            avaliacaoService.salvar(avaliacao);

            ra.addFlashAttribute("sucesso", "Avaliação enviada com sucesso!");

        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao enviar avaliação: " + e.getMessage());
        }

        return "redirect:/livros/detalhes/" + livroId;
    }
}
