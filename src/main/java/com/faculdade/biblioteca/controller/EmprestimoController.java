package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.EmprestimoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/emprestimos")
public class EmprestimoController {

    @Autowired
    private EmprestimoService emprestimoService;

    // ========================== TELA DE CONFIRMAÇÃO DO ALUGUEL ==========================
    @PostMapping("/alugar/{livroId}")
    public String processarAluguel(@PathVariable Long livroId,
                                   @AuthenticationPrincipal Usuario usuario,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Processa o aluguel e obtém os detalhes
            Map<String, Object> detalhesEmprestimo = emprestimoService.processarAluguelComDetalhes(livroId, usuario);

            // Adiciona os detalhes ao modelo para a view
            model.addAttribute("emprestimo", detalhesEmprestimo.get("emprestimo"));
            model.addAttribute("livro", detalhesEmprestimo.get("livro"));
            model.addAttribute("prazoDias", detalhesEmprestimo.get("prazoDias"));
            model.addAttribute("dataDevolucao", detalhesEmprestimo.get("dataDevolucao"));
            model.addAttribute("multaDiaria", detalhesEmprestimo.get("multaDiaria"));

            return "emprestimo/confirmacao-aluguel";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao alugar livro: " + e.getMessage());
            return "redirect:/livros";
        }
    }
}