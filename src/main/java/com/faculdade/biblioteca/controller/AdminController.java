package com.faculdade.biblioteca.controller;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("imagemCapa");
    }

    @Autowired private LivroService livroService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private EmprestimoService emprestimoService;
    @Autowired private CsvImportService csvImportService;
    @Autowired private UsuarioCsvService usuarioCsvService;
    @Autowired private NotificacaoService notificacaoService;

    // ------------------------------------------------------------
    // PAINEL PRINCIPAL
    // ------------------------------------------------------------
    @GetMapping("/painel")
    public String painelAdmin(Model model) {

        model.addAttribute("totalLivros", livroService.contarTotalLivros());
        model.addAttribute("totalUsuarios", usuarioService.contarTotalUsuarios());
        model.addAttribute("totalEmprestados", livroService.contarLivrosIndisponiveis());
        model.addAttribute("totalAtrasos", emprestimoService.contarEmprestimosAtrasados());

        model.addAttribute("livros", livroService.listarRecentes(5));
        model.addAttribute("usuarios", usuarioService.listarRecentes(5));
        model.addAttribute("emprestimos", emprestimoService.listarRecentes(5));

        return "Admin/Painel";
    }

    // ------------------------------------------------------------
    // LIVROS
    // ------------------------------------------------------------
    @GetMapping("/livros")
    public String listarLivros(@RequestParam(required = false) String busca,
                               @RequestParam(required = false) Long categoria,
                               @RequestParam(required = false) String disponibilidade,
                               Model model) {

        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("livros", livroService.filtrarLivros(busca, categoria, disponibilidade));
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaSelecionada", categoria);
        model.addAttribute("disponibilidadeSelecionada", disponibilidade);

        return "Admin/Livro/livros";
    }

    @GetMapping("/livros/cadastrar")
    public String formCadastrarLivro(Model model) {
        model.addAttribute("livro", new Livro());
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "Admin/Livro/livro-form";
    }

    @PostMapping("/livros/cadastrar")
    public String cadastrarLivro(@ModelAttribute Livro livro,
                                 @RequestParam Long categoriaId,
                                 @RequestParam(value = "imagemCapa", required = false) MultipartFile imagemCapa,
                                 RedirectAttributes ra) {

        try {
            Categoria categoria = categoriaService.buscarPorId(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

            livro.setCategoria(categoria);
            livroService.salvarComCapa(livro, imagemCapa);

            ra.addFlashAttribute("sucesso", "Livro cadastrado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao cadastrar livro: " + e.getMessage());
            return "redirect:/admin/livros/cadastrar";
        }

        return "redirect:/admin/livros";
    }

    @GetMapping("/livros/editar/{id}")
    public String formEditarLivro(@PathVariable Long id, Model model) {

        Livro livro = livroService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        model.addAttribute("livro", livro);
        model.addAttribute("categorias", categoriaService.listarTodas());

        return "Admin/Livro/livro-editar";
    }

    @PostMapping("/livros/editar/{id}")
    public String editarLivro(@PathVariable Long id,
                              @ModelAttribute Livro livroForm,
                              @RequestParam Long categoriaId,
                              @RequestParam(value = "imagemCapa", required = false) MultipartFile imagemCapa,
                              RedirectAttributes ra) {

        try {
            Livro livroExistente = livroService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

            Categoria categoria = categoriaService.buscarPorId(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

            livroExistente.setTitulo(livroForm.getTitulo());
            livroExistente.setAutor(livroForm.getAutor());
            livroExistente.setDescricao(livroForm.getDescricao());
            livroExistente.setQuantidade(livroForm.getQuantidade());
            livroExistente.setEditora(livroForm.getEditora());
            livroExistente.setAnoPublicacao(livroForm.getAnoPublicacao());
            livroExistente.setCategoria(categoria);

            livroService.atualizarComCapa(id, livroExistente, imagemCapa);

            ra.addFlashAttribute("sucesso", "Livro atualizado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar livro: " + e.getMessage());
        }

        return "redirect:/admin/livros";
    }

    @PostMapping("/livros/excluir/{id}")
    public String excluirLivro(@PathVariable Long id, RedirectAttributes ra) {
        try {
            livroService.excluir(id);
            ra.addFlashAttribute("sucesso", "Livro excluído com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao excluir livro: " + e.getMessage());
        }
        return "redirect:/admin/livros";
    }

    // ------------------------------------------------------------
    // IMPORTAÇÃO CSV — LIVROS
    // ------------------------------------------------------------
    @GetMapping("/livros/importar")
    public String formImportarLivrosCsv() {
        return "Admin/Livro/importar-csv";
    }

    @PostMapping("/livros/importar")
    public String importarLivrosCsv(@RequestParam("arquivo") MultipartFile arquivo,
                                    RedirectAttributes ra) {

        try {
            Map<String, Object> resultado = csvImportService.importarLivrosCSV(arquivo);

            ra.addFlashAttribute("sucesso",
                    resultado.get("importados") + " livros importados com sucesso!");

            if (!((List<?>) resultado.get("erros")).isEmpty()) {
                ra.addFlashAttribute("erros", resultado.get("erros"));
            }

        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Falha ao importar arquivo: " + e.getMessage());
        }

        return "redirect:/admin/livros";
    }

    // ------------------------------------------------------------
    // CATEGORIAS
    // ------------------------------------------------------------
    @GetMapping("/categorias")
    public String listarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "Admin/Categoria/categorias";
    }

    @GetMapping("/categorias/nova")
    public String novaCategoriaForm(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "Admin/Categoria/categoria-form";
    }

    @PostMapping("/categorias/nova")
    public String salvarCategoria(@ModelAttribute Categoria categoria, RedirectAttributes ra) {
        try {
            categoriaService.salvar(categoria);
            ra.addFlashAttribute("sucesso", "Categoria cadastrada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    // ------------------------------------------------------------
    // USUÁRIOS
    // ------------------------------------------------------------
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "Admin/Usuario/usuarios";
    }

    @GetMapping("/usuarios/detalhes/{id}")
    public String detalhesUsuario(@PathVariable Long id, Model model) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("usuario", usuario);
        model.addAttribute("emprestimosAtivos", emprestimoService.listarAtivosPorUsuario(id));
        model.addAttribute("historico", emprestimoService.listarHistoricoPorUsuario(id));
        model.addAttribute("atrasos", emprestimoService.listarAtrasosPorUsuario(id));
        model.addAttribute("desejos", usuarioService.listarDesejosDoUsuario(id));

        return "Admin/Usuario/usuario-detalhes";
    }

    // ------------------------------------------------------------
    // IMPORTAÇÃO CSV — USUÁRIOS
    // ------------------------------------------------------------
    @GetMapping("/usuarios/importar")
    public String formImportarUsuariosCsv() {
        return "Admin/Usuario/importar-csv";
    }

    @PostMapping("/usuarios/importar")
    public String importarUsuariosCsv(@RequestParam("arquivo") MultipartFile arquivo,
                                      RedirectAttributes ra) {

        try {
            int total = usuarioCsvService.importarCsv(arquivo);
            ra.addFlashAttribute("sucesso", total + " usuários importados com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao importar CSV: " + e.getMessage());
        }

        return "redirect:/admin/usuarios";
    }

    // ------------------------------------------------------------
    // NOTIFICAÇÕES
    // ------------------------------------------------------------
    @PostMapping("/notificacoes/enviar/{id}")
    public String enviarLembrete(@PathVariable Long id, RedirectAttributes ra) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        notificacaoService.enviar(usuario, "Você possui livros em atraso ou pendências!");

        ra.addFlashAttribute("sucesso", "Lembrete enviado para o usuário!");
        return "redirect:/admin/usuarios/detalhes/" + id;
    }
    @GetMapping("/usuarios/ativar/{id}")
    public String ativarUsuario(@PathVariable Long id, RedirectAttributes ra) {
        try {
            usuarioService.alterarStatus(id, true);
            ra.addFlashAttribute("sucesso", "Usuário ativado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao ativar usuário: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
    @GetMapping("/usuarios/desativar/{id}")
    public String desativarUsuario(@PathVariable Long id, RedirectAttributes ra) {
        try {
            usuarioService.alterarStatus(id, false);
            ra.addFlashAttribute("sucesso", "Usuário desativado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao desativar usuário: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
    @GetMapping("/usuarios/excluir/{id}")
    public String excluirUsuario(@PathVariable Long id, RedirectAttributes ra) {
        try {
            usuarioService.excluirUsuario(id);
            ra.addFlashAttribute("sucesso", "Usuário excluído com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao excluir usuário: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/emprestimos/atrasar/{id}")
    public String atrasarEmprestimo(@PathVariable Long id, RedirectAttributes ra) {

        try {
            emprestimoService.forcarAtraso(id);  // Você cria este método no service
            ra.addFlashAttribute("sucesso", "Empréstimo marcado como atrasado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao marcar atraso: " + e.getMessage());
        }

        return "redirect:/admin/emprestimos";
    }

    @PostMapping("/emprestimos/quitar/{id}")
    public String quitarEmprestimo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            emprestimoService.quitarMulta(id); // você implementa no service
            ra.addFlashAttribute("sucesso", "Multa quitada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao quitar multa: " + e.getMessage());
        }
        return "redirect:/admin/emprestimos";
    }
}
