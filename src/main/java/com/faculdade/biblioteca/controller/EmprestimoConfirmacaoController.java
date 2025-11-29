package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.CustomUserDetails;
import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.repository.EmprestimoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class EmprestimoConfirmacaoController {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @GetMapping("/emprestimo/confirmado")
    public String confirmado(@AuthenticationPrincipal CustomUserDetails usuarioLogado,
                             Model model) {

        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        Emprestimo emprestimo = emprestimoRepository
                .findTopByUsuarioIdOrderByDataEmprestimoDesc(usuarioLogado.getId())
                .orElseThrow(() -> new RuntimeException("Nenhum empréstimo encontrado"));

        int prazoDias = 14;
        LocalDate dataDevolucao = LocalDate.from(emprestimo.getDataEmprestimo().plusDays(prazoDias));

        model.addAttribute("emprestimo", emprestimo);
        model.addAttribute("livro", emprestimo.getLivro());
        model.addAttribute("prazoDias", prazoDias);
        model.addAttribute("multaDiaria", 2.0);
        model.addAttribute("dataDevolucao", dataDevolucao);

        return "emprestimo/confirmado";
    }
}
