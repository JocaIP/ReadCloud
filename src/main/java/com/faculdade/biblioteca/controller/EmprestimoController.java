package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Emprestimo;
import com.faculdade.biblioteca.service.EmprestimoService;
import com.faculdade.biblioteca.service.LivroService;
import com.faculdade.biblioteca.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/emprestimos")
public class EmprestimoController {

    @Autowired private EmprestimoService emprestimoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private LivroService livroService;

    // LISTAR
    @GetMapping
    public String listarEmprestimos(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String status,
            Model model) {

        List<Emprestimo> emprestimos = (termo != null && !termo.isBlank())
                ? emprestimoService.buscarPorTermo(termo)
                : emprestimoService.buscarTodos();

        if (status != null && !status.isBlank()) {
            emprestimos.removeIf(e -> !e.getStatus().name().equalsIgnoreCase(status));
        }

        model.addAttribute("emprestimos", emprestimos);
        model.addAttribute("totalEmprestimos", emprestimoService.contarTotalEmprestimos());
        model.addAttribute("emprestimosAtivos", emprestimoService.contarEmprestimosAtivos());
        model.addAttribute("emprestimosAtrasados", emprestimoService.contarEmprestimosAtrasados());
        model.addAttribute("emprestimosFinalizados", emprestimoService.contarEmprestimosFinalizadosEsteMes());

        return "Admin/Emprestimo/emprestimos";
    }

    // FORM NOVO
    @GetMapping("/novo")
    public String formNovoEmprestimo(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("livros", livroService.buscarDisponiveis());
        return "Admin/Emprestimo/emprestimo-form";
    }

    // SALVAR NOVO
    @PostMapping("/novo")
    public String criarEmprestimoAdmin(@RequestParam Long usuarioId,
                                       @RequestParam Long livroId,
                                       RedirectAttributes ra) {
        try {
            emprestimoService.criarEmprestimoAdmin(usuarioId, livroId);
            ra.addFlashAttribute("sucesso", "Empréstimo registrado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao registrar empréstimo: " + e.getMessage());
            return "redirect:/admin/emprestimos/novo";
        }
        return "redirect:/admin/emprestimos";
    }

    // DEVOLVER
    @PostMapping("/devolver/{id}")
    public String devolverEmprestimo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            emprestimoService.devolverEmprestimo(id);
            ra.addFlashAttribute("sucesso", "Empréstimo devolvido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao devolver: " + e.getMessage());
        }
        return "redirect:/admin/emprestimos";
    }

    // RENOVAR
    @PostMapping("/renovar/{id}")
    public String renovarEmprestimo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            emprestimoService.renovarEmprestimo(id);
            ra.addFlashAttribute("sucesso", "Empréstimo renovado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao renovar: " + e.getMessage());
        }
        return "redirect:/admin/emprestimos";
    }

    // MULTA
    @PostMapping("/multa/{id}")
    public String aplicarMulta(@PathVariable Long id,
                               @RequestParam(required = false) String valor,
                               RedirectAttributes ra) {
        try {
            java.math.BigDecimal valorDecimal = (valor == null || valor.isBlank())
                    ? emprestimoService.getConfigMultaPadrao()
                    : new java.math.BigDecimal(valor.replace(",", "."));

            emprestimoService.aplicarMulta(id, valorDecimal);
            ra.addFlashAttribute("sucesso", "Multa aplicada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao aplicar multa: " + e.getMessage());
        }
        return "redirect:/admin/emprestimos";
    }

    // DETALHES
    @GetMapping("/detalhes/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        Emprestimo e = emprestimoService.buscarPorId(id);
        model.addAttribute("emprestimo", e);
        return "Admin/Emprestimo/emprestimo-detalhes";
    }
}
