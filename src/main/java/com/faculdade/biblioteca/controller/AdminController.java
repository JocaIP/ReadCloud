package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private LivroService livroService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private EmprestimoService emprestimoService;
    @Autowired(required = false) private AvaliacaoService avaliacaoService;

    // ========================== PAINEL PRINCIPAL ==========================
    @GetMapping("/painel")
    public String painelAdmin(Model model) {
        try {
            model.addAttribute("totalLivros", livroService.contarTotalLivros());
            model.addAttribute("totalUsuarios", usuarioService.contarTotalUsuarios());
            model.addAttribute("totalEmprestimos", livroService.contarLivrosIndisponiveis());
            model.addAttribute("totalAtrasos", emprestimoService.contarEmprestimosAtrasados());
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar dados do painel");
        }
        return "Admin/Painel";
    }

    // ========================== LISTAR LIVROS ==========================
    @GetMapping("/livros")
    public String listarLivrosAdmin(Model model) {
        try {
            model.addAttribute("livros", livroService.listarTodos());
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar livros: " + e.getMessage());
        }
        return "Admin/Livro/livros";
    }

    // ========================== FORM CADASTRAR LIVRO ==========================
    @GetMapping("/livros/cadastrar")
    public String formCadastrarLivro(Model model) {
        model.addAttribute("livro", new Livro());
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "Admin/Livro/livro-form";
    }

    // ========================== CADASTRAR LIVRO ==========================
    @PostMapping("/livros/cadastrar")
    public String cadastrarLivro(@ModelAttribute Livro livro,
                                 @RequestParam Long categoriaId,
                                 @RequestParam(value = "capa", required = false) MultipartFile capa,
                                 RedirectAttributes ra) {
        try {
            Categoria categoria = categoriaService.buscarPorId(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            livro.setCategoria(categoria);

            livroService.salvarComCapa(livro, capa);
            ra.addFlashAttribute("sucesso", "Livro cadastrado com sucesso!");
            return "redirect:/admin/painel"; // 🔹 CORREÇÃO: Redireciona para o painel admin
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao cadastrar livro: " + e.getMessage());
            return "redirect:/admin/livros/cadastrar";
        }
    }

    // ========================== EXCLUIR LIVRO ==========================
    @PostMapping("/livros/excluir/{id}")
    public String excluirLivro(@PathVariable Long id, RedirectAttributes ra) {
        try {
            livroService.excluirPorId(id);
            ra.addFlashAttribute("sucesso", "Livro excluído com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao excluir livro: " + e.getMessage());
        }
        return "redirect:/admin/livros";
    }

    // ========================== GESTÃO DE USUÁRIOS ==========================
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        try {
            model.addAttribute("usuarios", usuarioService.listarTodos());
            model.addAttribute("totalUsuarios", usuarioService.contarTotalUsuarios());
            model.addAttribute("usuariosAtivos", usuarioService.contarUsuariosAtivos());
            model.addAttribute("emprestimosAtivos", emprestimoService.contarEmprestimosAtivos());
            model.addAttribute("emprestimosAtrasados", emprestimoService.contarEmprestimosAtrasados());
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao carregar dados dos usuários: " + e.getMessage());
            model.addAttribute("usuarios", List.of());
        }
        return "Admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/status")
    public String alterarStatusUsuario(@PathVariable Long id,
                                       @RequestParam boolean novoStatus,
                                       RedirectAttributes ra) {
        try {
            usuarioService.alterarStatus(id, novoStatus);
            ra.addFlashAttribute("sucesso", "Status do usuário alterado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao alterar status do usuário: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/detalhes/{id}")
    public String detalhesUsuario(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("usuario", usuario);
        model.addAttribute("emprestimos", emprestimoService.buscarEmprestimosAtivos(id));
        model.addAttribute("historico", emprestimoService.buscarHistorico(id));
        model.addAttribute("atrasos", emprestimoService.buscarAtrasosPorUsuario(id));

        return "Admin/usuario-detalhes"; // 🔹 Nome do template HTML
    }
}