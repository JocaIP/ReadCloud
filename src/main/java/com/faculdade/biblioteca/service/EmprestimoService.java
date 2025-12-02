package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.*;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;

@Service
@Transactional
public class EmprestimoService {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Configuração padrão: prazo 14 dias, multa 2.00/dia, limite 3 livros
    private final ConfigEmprestimo config = new ConfigEmprestimo(14, new BigDecimal("2.00"), 3);

    // ---------------------------
    // Métodos públicos usados pelos controllers (assinaturas exigidas)
    // ---------------------------

    public List<Emprestimo> buscarTodos() {
        return emprestimoRepository.findAll();
    }

    public List<Emprestimo> buscarPorTermo(String termo) {
        if (termo == null || termo.trim().isEmpty()) return buscarTodos();
        return emprestimoRepository.buscarPorTermo(termo.trim());
    }

    public Emprestimo buscarPorId(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado"));
    }

    public Long contarTotalEmprestimos() {
        return emprestimoRepository.count();
    }

    public Long contarEmprestimosAtivos() {
        return emprestimoRepository.countEmprestimosAtivos();
    }

    public Long contarEmprestimosAtrasados() {
        // atualiza status antes de contar
        atualizarStatusEmprestimos();
        return emprestimoRepository.countEmprestimosAtrasados();
    }

    public Long contarEmprestimosFinalizadosEsteMes() {
        return emprestimoRepository.countEmprestimosFinalizadosEsteMes();
    }

    /**
     * Retorna o valor padrão da multa diária (BigDecimal).
     */
    public BigDecimal getConfigMultaPadrao() {
        return config.getMultaDiaria();
    }

    /**
     * Cria um empréstimo via painel admin (usa IDs)
     */
    @Transactional
    public Emprestimo criarEmprestimoAdmin(Long usuarioId, Long livroId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Livro indisponível.");
        }

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        if (ativos >= config.getLimiteLivrosPorUsuario()) {
            throw new RuntimeException("Usuário atingiu o limite de empréstimos.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime devolucao = agora.plusDays(config.getPrazoDevolucaoDias());

        Emprestimo e = new Emprestimo(livro, usuario, agora, devolucao);
        e.setStatus(StatusEmprestimo.ATIVO);
        e.setDevolvido(false);
        e.setRenovacoes(0);
        e.setMultaAcumulada(BigDecimal.ZERO);

        emprestimoRepository.save(e);

        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);

        return e;
    }

    /**
     * Renova empréstimo (assinatura esperada pelos controllers).
     */
    @Transactional
    public Emprestimo renovarEmprestimo(Long emprestimoId) {
        Emprestimo e = buscarPorId(emprestimoId);

        if (!e.isPodeRenovar()) {
            throw new RuntimeException("Este empréstimo não pode ser renovado.");
        }

        if (e.getDataDevolucao() == null) {
            throw new RuntimeException("Empréstimo não possui data de devolução.");
        }

        e.setDataDevolucao(e.getDataDevolucao().plusDays(config.getPrazoDevolucaoDias()));
        e.setRenovacoes(e.getRenovacoes() + 1);
        e.setStatus(StatusEmprestimo.RENOVADO);

        return emprestimoRepository.save(e);
    }

    /**
     * Aplica multa (assinatura esperada pelos controllers).
     */
    @Transactional
    public Emprestimo aplicarMulta(Long emprestimoId, BigDecimal valor) {
        Emprestimo e = buscarPorId(emprestimoId);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor de multa inválido.");
        }

        // aceita aplicar em ATIVO ou ATRASADO
        if (e.getStatus() != StatusEmprestimo.ATIVO && e.getStatus() != StatusEmprestimo.ATRASADO) {
            throw new RuntimeException("Multa só pode ser aplicada em empréstimos ativos ou atrasados.");
        }

        if (e.getMultaAcumulada() == null) {
            e.setMultaAcumulada(BigDecimal.ZERO);
        }

        e.setMultaAcumulada(e.getMultaAcumulada().add(valor));

        // se estava ATIVO e ultrapassou prazo, marca ATRASADO
        if (e.getStatus() == StatusEmprestimo.ATIVO && e.getDataDevolucao() != null && e.getDataDevolucao().isBefore(LocalDateTime.now())) {
            e.setStatus(StatusEmprestimo.ATRASADO);
        }

        return emprestimoRepository.save(e);
    }

    // ---------------------------
    // Métodos usados pelo perfil / carrinho
    // ---------------------------

    public List<Emprestimo> listarAtivosPorUsuario(Long usuarioId) {
        // se repository tiver método específico, usa; caso contrário usa fallback
        try {
            return emprestimoRepository.findByUsuarioIdAndStatus(usuarioId, StatusEmprestimo.ATIVO);
        } catch (Exception ex) {
            return emprestimoRepository.findByUsuarioIdAndDevolvidoFalse(usuarioId);
        }
    }

    public List<Emprestimo> listarHistoricoPorUsuario(Long usuarioId) {
        try {
            return emprestimoRepository.findByUsuarioId(usuarioId);
        } catch (Exception ex) {
            return emprestimoRepository.findByUsuarioIdAndDevolvidoTrue(usuarioId);
        }
    }

    public List<Emprestimo> listarAtrasosPorUsuario(Long usuarioId) {
        // atualiza status antes de retornar
        atualizarStatusEmprestimos();
        try {
            return emprestimoRepository.findByUsuarioIdAndStatus(usuarioId, StatusEmprestimo.ATRASADO);
        } catch (Exception ex) {
            return emprestimoRepository.findAtrasadosPorUsuario(usuarioId, LocalDateTime.now());
        }
    }

    /**
     * Registra empréstimo a partir de objeto Usuario + Livro (usado pelo CarrinhoController).
     */
    @Transactional
    public void registrarEmprestimo(Usuario usuario, Livro livro) {

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RuntimeException("Usuário inativo não pode alugar livros.");
        }

        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Livro indisponível.");
        }

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuario.getId());
        if (ativos >= config.getLimiteLivrosPorUsuario()) {
            throw new RuntimeException("Você atingiu o limite de empréstimos.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime devolucao = agora.plusDays(config.getPrazoDevolucaoDias());

        Emprestimo e = new Emprestimo();
        e.setUsuario(usuario);
        e.setLivro(livro);
        e.setDataEmprestimo(agora);
        e.setDataDevolucao(devolucao);
        e.setStatus(StatusEmprestimo.ATIVO);
        e.setDevolvido(false);
        e.setRenovacoes(0);
        e.setMultaAcumulada(BigDecimal.ZERO);

        emprestimoRepository.save(e);

        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);
    }

    /**
     * Verifica se usuário pode adicionar mais itens no carrinho (assinatura usada no CarrinhoController).
     */
    public boolean podeAdicionarAoCarrinho(Long usuarioId, List<Livro> carrinhoAtual) {
        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        int futuro = ativos.intValue() + (carrinhoAtual != null ? carrinhoAtual.size() : 0);
        return futuro < config.getLimiteLivrosPorUsuario();
    }

    // ---------------------------
    // Devolução via perfil (assinatura que o PerfilController usa)
    // ---------------------------
    @Transactional
    public String devolverLivro(Long emprestimoId, Usuario usuario) {
        Optional<Emprestimo> opt = emprestimoRepository.findByIdAndUsuarioId(emprestimoId, usuario.getId());

        if (opt.isEmpty()) {
            throw new RuntimeException("Empréstimo não encontrado.");
        }

        Emprestimo e = opt.get();

        if (e.isDevolvido()) {
            throw new RuntimeException("Este empréstimo já foi devolvido.");
        }

        e.setDataDevolucaoReal(LocalDateTime.now());
        e.setDevolvido(true);
        e.setStatus(StatusEmprestimo.FINALIZADO);

        Livro livro = e.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        emprestimoRepository.save(e);

        return "Livro devolvido com sucesso!";
    }

    // ---------------------------
    // Devolução admin (assinatura que EmprestimoController usa)
    // ---------------------------
    @Transactional
    public Emprestimo devolverEmprestimo(Long emprestimoId) {
        Emprestimo e = buscarPorId(emprestimoId);

        if (e.isDevolvido()) {
            throw new RuntimeException("Este empréstimo já foi devolvido.");
        }

        e.setDataDevolucaoReal(LocalDateTime.now());
        e.setDevolvido(true);
        e.setStatus(StatusEmprestimo.FINALIZADO);

        Livro livro = e.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(e);
    }

    // ---------------------------
    // Forçar atraso (admin)
    // ---------------------------
    @Transactional
    public Emprestimo forcarAtraso(Long id) {
        Emprestimo e = buscarPorId(id);

        if (e.getStatus() == StatusEmprestimo.FINALIZADO) {
            throw new RuntimeException("Não é possível atrasar um empréstimo finalizado.");
        }

        if (e.getDataDevolucao() != null) {
            e.setDataDevolucao(e.getDataDevolucao().minusDays(7));
        } else {
            // se não há dataDevolucao, cria uma para forçar atraso
            e.setDataDevolucao(LocalDateTime.now().minusDays(7));
        }

        e.setStatus(StatusEmprestimo.ATRASADO);
        return emprestimoRepository.save(e);
    }

    // ---------------------------
    // Quitar multa (admin)
    // ---------------------------
    @Transactional
    public Emprestimo quitarMulta(Long id) {
        Emprestimo e = buscarPorId(id);

        if (e.getStatus() != StatusEmprestimo.ATRASADO) {
            throw new RuntimeException("Somente empréstimos atrasados podem ser quitados.");
        }

        e.setStatus(StatusEmprestimo.FINALIZADO);
        e.setDevolvido(true);
        e.setMultaAcumulada(BigDecimal.ZERO);
        e.setDataDevolucaoReal(LocalDateTime.now());

        Livro livro = e.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(e);
    }

    // ---------------------------
    // Atualiza status (ATIVO -> ATRASADO) de forma segura
    // ---------------------------
    @Transactional
    public void atualizarStatusEmprestimos() {
        List<Emprestimo> ativos = emprestimoRepository.findByStatus(StatusEmprestimo.ATIVO);
        LocalDateTime agora = LocalDateTime.now();

        for (Emprestimo e : ativos) {
            LocalDateTime prevista = e.getDataDevolucao();
            if (prevista == null) continue;
            if (prevista.isBefore(agora)) {
                e.setStatus(StatusEmprestimo.ATRASADO);
                emprestimoRepository.save(e);
            }
        }
    }

    // ---------------------------
    // Helpers para views / dashboard
    // ---------------------------

    public List<Emprestimo> listarRecentes(int limite) {
        List<Emprestimo> todos = emprestimoRepository.findAll();
        todos.sort(Comparator.comparing(Emprestimo::getId).reversed());
        return todos.stream().limit(limite).toList();
    }

    public List<Emprestimo> listarTodosOrdenadosPorDataDesc() {
        List<Emprestimo> todos = emprestimoRepository.findAll();
        todos.sort(Comparator.comparing(Emprestimo::getDataEmprestimo, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return todos;
    }
}
