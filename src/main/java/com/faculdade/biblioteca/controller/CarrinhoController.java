package com.faculdade.biblioteca.controller;
import com.faculdade.biblioteca.modelo.CustomUserDetails;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.Emprestimo;  // ✅ IMPORT NECESSÁRIO
import com.faculdade.biblioteca.service.EmprestimoService;
import com.faculdade.biblioteca.service.LivroService;
import com.faculdade.biblioteca.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrinho")
public class CarrinhoController {

    @Autowired private LivroService livroService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private EmprestimoService emprestimoService;

    @GetMapping
    public String carrinho(Model model, HttpSession session) {

        List<Livro> carrinho = (List<Livro>) session.getAttribute("carrinho");

        if (carrinho == null) {
            carrinho = new ArrayList<>();
            session.setAttribute("carrinho", carrinho);
        }

        System.out.println("✅ [CARRINHO] Itens no carrinho: " + carrinho.size());

        model.addAttribute("livrosNoCarrinho", carrinho);
        model.addAttribute("totalItens", carrinho.size());
        model.addAttribute("prazoPadrao", 14);

        return "carrinho/carrinho";
    }

    @PostMapping("/adicionar/{id}")
    public String adicionar(@PathVariable Long id,
                            HttpSession session,
                            @AuthenticationPrincipal CustomUserDetails usuarioLogado,
                            RedirectAttributes ra) {

        try {
            System.out.println("✅ REQUISIÇÃO PARA ADICIONAR LIVRO ID: " + id);

            if (usuarioLogado == null) {
                System.out.println("❌ Usuário não logado");
                ra.addFlashAttribute("erro", "Faça login.");
                return "redirect:/login";
            }

            System.out.println("✅ USUÁRIO LOGADO ID: " + usuarioLogado.getId());

            Usuario usuario = usuarioService.buscarPorId(usuarioLogado.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            System.out.println("✅ Usuário existe: " + usuario.getNome());

            List<Livro> carrinho = (List<Livro>) session.getAttribute("carrinho");

            if (carrinho == null) {
                carrinho = new ArrayList<>();
                session.setAttribute("carrinho", carrinho);
                System.out.println("✅ Sessão criada");
            }

            System.out.println("✅ Carrinho tem: " + carrinho.size() + " itens");

            boolean pode = emprestimoService.podeAdicionarAoCarrinho(usuario.getId(), carrinho);
            System.out.println("✅ Pode adicionar? " + pode);

            if (!pode) {
                ra.addFlashAttribute("erro", "Limite atingido");
                return "redirect:/carrinho";
            }

            Livro livro = livroService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

            System.out.println("✅ Livro encontrado: " + livro.getTitulo());

            if (livro.getQuantidade() <= 0) {
                ra.addFlashAttribute("erro", "Livro indisponível");
                return "redirect:/livros";
            }

            boolean existe = carrinho.stream().anyMatch(l -> l.getId().equals(id));
            System.out.println("✅ Já está no carrinho? " + existe);

            if (existe) {
                ra.addFlashAttribute("erro", "Já está no carrinho");
                return "redirect:/carrinho";
            }

            carrinho.add(livro);
            session.setAttribute("carrinho", carrinho);

            System.out.println("✅ LIVRO ADICIONADO COM SUCESSO: " + livro.getTitulo());

            ra.addFlashAttribute("sucesso", "Livro adicionado!");
            return "redirect:/carrinho";

        } catch (Exception e) {
            System.out.println("🔥 ERRO AO ADICIONAR LIVRO:");
            e.printStackTrace();

            ra.addFlashAttribute("erro", "Erro ao adicionar livro: " + e.getMessage());
            return "redirect:/carrinho";
        }
    }

    @PostMapping("/remover/{id}")
    public String remover(@PathVariable Long id,
                          HttpSession session,
                          RedirectAttributes ra) {

        List<Livro> carrinho = (List<Livro>) session.getAttribute("carrinho");

        if (carrinho != null) {
            carrinho.removeIf(l -> l.getId().equals(id));
            session.setAttribute("carrinho", carrinho);
        }

        ra.addFlashAttribute("sucesso", "Removido.");
        return "redirect:/carrinho";
    }

    @PostMapping("/finalizar")
    public String finalizarAluguel(HttpSession session,
                                   Principal principal,
                                   Model model,
                                   RedirectAttributes ra) {

        Usuario usuario = usuarioService.buscarPorEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Livro> carrinho = (List<Livro>) session.getAttribute("carrinho");

        if (carrinho == null || carrinho.isEmpty()) {
            ra.addFlashAttribute("erro", "Seu carrinho está vazio!");
            return "redirect:/";
        }

        int prazoDias = 14;
        double multaDiaria = 2.00;

        List<Emprestimo> emprestimosCriados = new ArrayList<>();

        for (Livro livro : carrinho) {
            emprestimoService.registrarEmprestimo(usuario, livro);

            List<Emprestimo> ativos = emprestimoService.listarAtivosPorUsuario(usuario.getId());
            Emprestimo ultimo = ativos.get(ativos.size() - 1);

            emprestimosCriados.add(ultimo);
        }

        session.setAttribute("carrinho", new ArrayList<>());

        model.addAttribute("emprestimos", emprestimosCriados);
        model.addAttribute("prazoDias", prazoDias);
        model.addAttribute("multaDiaria", multaDiaria);

        return "emprestimos/aluguel-confirmado"; //  ✅ AJUSTADO AQUI
    }


}
