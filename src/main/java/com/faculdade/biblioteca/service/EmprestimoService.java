package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.*;
import com.faculdade.biblioteca.repository.EmprestimoRepository;
import com.faculdade.biblioteca.repository.LivroRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class EmprestimoService {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // CONFIG: PRAZO = 14 dias, multa = 2.00/dia, limite = 3 livros
    private final ConfigEmprestimo config = new ConfigEmprestimo(14, new BigDecimal("2.00"), 3);

    // ====================================================================
    // ADMIN – PAINEL
    // ====================================================================

    public List<Emprestimo> buscarTodos() {
        return emprestimoRepository.findAll();
    }

    public List<Emprestimo> buscarPorStatus(StatusEmprestimo status) {
        return emprestimoRepository.findByStatus(status);
    }

    public Emprestimo buscarPorId(Long id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado"));
    }

    public List<Emprestimo> buscarPorTermo(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return buscarTodos();
        }
        return emprestimoRepository.buscarPorTermo(termo.trim());
    }

    @Transactional
    public Emprestimo registrarDevolucao(Long emprestimoId) {
        Emprestimo emprestimo = buscarPorId(emprestimoId);

        if (emprestimo.isDevolvido()) {
            throw new RuntimeException("Este empréstimo já foi devolvido.");
        }

        emprestimo.setDataDevolucaoReal(LocalDateTime.now());
        emprestimo.setDevolvido(true);
        emprestimo.setStatus(StatusEmprestimo.FINALIZADO);

        Livro livro = emprestimo.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo renovarEmprestimo(Long emprestimoId) {
        Emprestimo emprestimo = buscarPorId(emprestimoId);

        if (!emprestimo.isPodeRenovar()) {
            throw new RuntimeException("Este empréstimo não pode ser renovado.");
        }

        emprestimo.setDataDevolucao(
                emprestimo.getDataDevolucao().plusDays(config.getPrazoDevolucaoDias())
        );
        emprestimo.setRenovacoes(emprestimo.getRenovacoes() + 1);
        emprestimo.setStatus(StatusEmprestimo.RENOVADO);

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public void atualizarStatusEmprestimos() {
        List<Emprestimo> ativos = emprestimoRepository.findByStatus(StatusEmprestimo.ATIVO);
        LocalDateTime agora = LocalDateTime.now();

        for (Emprestimo e : ativos) {
            if (e.getDataDevolucao().isBefore(agora)) {
                e.setStatus(StatusEmprestimo.ATRASADO);
                emprestimoRepository.save(e);
            }
        }
    }

    @Transactional
    public Emprestimo aplicarMulta(Long emprestimoId, BigDecimal valor) {
        Emprestimo emprestimo = buscarPorId(emprestimoId);

        if (emprestimo.getStatus() != StatusEmprestimo.ATRASADO && emprestimo.getStatus() != StatusEmprestimo.ATIVO) {
            throw new RuntimeException("Multa só pode ser aplicada em empréstimos ativos ou atrasados.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor de multa inválido.");
        }

        emprestimo.setMultaAcumulada(
                emprestimo.getMultaAcumulada().add(valor)
        );

        // caso esteja ATIVO e com data vencida, marca ATRASADO
        if (emprestimo.getStatus() == StatusEmprestimo.ATIVO && emprestimo.getDataDevolucao().isBefore(LocalDateTime.now())) {
            emprestimo.setStatus(StatusEmprestimo.ATRASADO);
        }

        return emprestimoRepository.save(emprestimo);
    }

    // ====================================================================
    // ESTATÍSTICAS
    // ====================================================================

    public Long contarTotalEmprestimos() {
        return emprestimoRepository.count();
    }

    public Long contarEmprestimosAtivos() {
        return emprestimoRepository.countEmprestimosAtivos();
    }

    public Long contarEmprestimosAtrasados() {
        atualizarStatusEmprestimos();
        return emprestimoRepository.countEmprestimosAtrasados();
    }

    public Long contarEmprestimosFinalizadosEsteMes() {
        return emprestimoRepository.countEmprestimosFinalizadosEsteMes();
    }

    // ====================================================================
    // EMPRÉSTIMO COM DETALHES (usado em página de detalhes)
    // ====================================================================

    @Transactional
    public Map<String, Object> processarAluguelComDetalhes(Long livroId, Usuario usuario) {

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Livro não disponível para empréstimo");
        }

        if (usuario.getAtivo() == null || !usuario.getAtivo()) {
            throw new RuntimeException("Seu usuário está INATIVO e não pode alugar livros.");
        }

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuario.getId());
        if (ativos >= config.getLimiteLivrosPorUsuario()) {
            throw new RuntimeException("Limite máximo de empréstimos atingido.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime devolucao = agora.plusDays(config.getPrazoDevolucaoDias());

        Emprestimo emprestimo = new Emprestimo(livro, usuario, agora, devolucao);
        emprestimoRepository.save(emprestimo);

        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);

        Map<String, Object> dados = new HashMap<>();
        dados.put("emprestimo", emprestimo);
        dados.put("livro", livro);
        dados.put("prazoDias", config.getPrazoDevolucaoDias());
        dados.put("dataDevolucao", devolucao);
        dados.put("multaDiaria", config.getMultaDiaria());

        return dados;
    }

    // ====================================================================
    // PERFIL – CONSULTAS
    // ====================================================================

    public List<Emprestimo> buscarEmprestimosAtivos(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndDevolvidoFalse(usuarioId);
    }

    public List<Emprestimo> buscarHistorico(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndDevolvidoTrue(usuarioId);
    }

    public List<Emprestimo> buscarAtrasosPorUsuario(Long usuarioId) {
        return emprestimoRepository.findAtrasadosPorUsuario(usuarioId, LocalDateTime.now());
    }

    // ====================================================================
    // CARRINHO – REGISTRO DE EMPRÉSTIMO
    // ====================================================================

    @Transactional
    public void registrarEmprestimo(Usuario usuario, Livro livro) {

        if (usuario.getAtivo() == null || !usuario.getAtivo()) {
            throw new RuntimeException("Seu usuário está INATIVO e não pode alugar livros.");
        }

        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Livro indisponível para empréstimo.");
        }

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuario.getId());
        if (ativos >= config.getLimiteLivrosPorUsuario()) {
            throw new RuntimeException("Você atingiu o limite de empréstimos.");
        }

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setUsuario(usuario);
        emprestimo.setLivro(livro);
        emprestimo.setDataEmprestimo(LocalDateTime.now());
        emprestimo.setDataDevolucao(LocalDateTime.now().plusDays(config.getPrazoDevolucaoDias()));
        emprestimo.setStatus(StatusEmprestimo.ATIVO);
        emprestimo.setDevolvido(false);

        emprestimoRepository.save(emprestimo);

        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);
    }

    // ====================================================================
    // MÉTODOS DE APOIO / UTILITÁRIOS
    // ====================================================================

    public boolean podeAdicionarAoCarrinho(Long usuarioId, List<Livro> carrinhoAtual) {
        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        int futuro = ativos.intValue() + (carrinhoAtual != null ? carrinhoAtual.size() : 0);
        return futuro < config.getLimiteLivrosPorUsuario();
    }

    public int livrosDisponiveisParaAlugar(Long usuarioId) {
        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        return config.getLimiteLivrosPorUsuario() - ativos.intValue();
    }

    public Map<String, Object> validarCarrinho(Long usuarioId, List<Livro> carrinho) {
        Map<String, Object> r = new HashMap<>();

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        int futuro = ativos.intValue() + (carrinho != null ? carrinho.size() : 0);

        r.put("valido", futuro <= config.getLimiteLivrosPorUsuario());
        r.put("emprestimosAtivos", ativos);
        r.put("itensCarrinho", carrinho != null ? carrinho.size() : 0);
        r.put("limite", config.getLimiteLivrosPorUsuario());
        r.put("livrosRestantes", config.getLimiteLivrosPorUsuario() - ativos.intValue());

        return r;
    }

    // ====================================================================
    // DEVOLUÇÃO – PERFIL
    // ====================================================================

    @Transactional
    public String devolverLivro(Long emprestimoId, Usuario usuario) {

        Optional<Emprestimo> emprestimoOpt =
                emprestimoRepository.findByIdAndUsuarioId(emprestimoId, usuario.getId());

        if (emprestimoOpt.isEmpty()) {
            throw new RuntimeException("Empréstimo não encontrado.");
        }

        Emprestimo emprestimo = emprestimoOpt.get();

        if (emprestimo.isDevolvido()) {
            throw new RuntimeException("Este empréstimo já foi devolvido.");
        }

        emprestimo.setDataDevolucaoReal(LocalDateTime.now());
        emprestimo.setDevolvido(true);
        emprestimo.setStatus(StatusEmprestimo.FINALIZADO);

        Livro livro = emprestimo.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        emprestimoRepository.save(emprestimo);

        return "Livro devolvido com sucesso!";
    }

    // ====================================================================
    // ADMIN – CRIAR EMPRÉSTIMO
    // ====================================================================

    @Transactional
    public Emprestimo criarEmprestimoAdmin(Long usuarioId, Long livroId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        if (livro.getQuantidade() <= 0) {
            throw new RuntimeException("Livro indisponível para empréstimo.");
        }

        Long ativos = emprestimoRepository.countEmprestimosAtivosPorUsuario(usuarioId);
        if (ativos >= config.getLimiteLivrosPorUsuario()) {
            throw new RuntimeException("Usuário atingiu o limite de empréstimos.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime devolucao = agora.plusDays(config.getPrazoDevolucaoDias());

        Emprestimo emprestimo = new Emprestimo(
                livro, usuario, agora, devolucao
        );
        emprestimo.setStatus(StatusEmprestimo.ATIVO);
        emprestimo.setDevolvido(false);

        emprestimoRepository.save(emprestimo);

        livro.setQuantidade(livro.getQuantidade() - 1);
        livroRepository.save(livro);

        return emprestimo;
    }

    // ====================================================================
    // ADMIN – DEVOLVER, ATRASAR, QUITAR
    // ====================================================================

    @Transactional
    public Emprestimo devolverEmprestimo(Long emprestimoId) {
        Emprestimo emprestimo = buscarPorId(emprestimoId);

        if (emprestimo.isDevolvido()) {
            throw new RuntimeException("Empréstimo já devolvido.");
        }

        emprestimo.setDataDevolucaoReal(LocalDateTime.now());
        emprestimo.setDevolvido(true);
        emprestimo.setStatus(StatusEmprestimo.FINALIZADO);

        Livro livro = emprestimo.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo marcarComoAtrasado(Long id) {
        Emprestimo emprestimo = buscarPorId(id);

        if (emprestimo.getStatus() == StatusEmprestimo.FINALIZADO) {
            throw new RuntimeException("Não é possível marcar um empréstimo finalizado como atrasado.");
        }

        emprestimo.setStatus(StatusEmprestimo.ATRASADO);
        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo marcarComoQuitado(Long id) {
        Emprestimo emprestimo = buscarPorId(id);

        if (emprestimo.getStatus() != StatusEmprestimo.ATRASADO) {
            throw new RuntimeException("Somente empréstimos atrasados podem ser quitados.");
        }

        emprestimo.setStatus(StatusEmprestimo.FINALIZADO);
        emprestimo.setMultaAcumulada(BigDecimal.ZERO);
        emprestimo.setDevolvido(true);
        emprestimo.setDataDevolucaoReal(LocalDateTime.now());

        Livro livro = emprestimo.getLivro();
        livro.setQuantidade(livro.getQuantidade() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(emprestimo);
    }

    // ====================================================================
    // FUNÇÕES QUE O CONTROLLER USA (helpers)
    // ====================================================================

    public BigDecimal getConfigMultaPadrao() {
        return config.getMultaDiaria();
    }

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
    public List<Emprestimo> listarAtivosPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndStatus(usuarioId, StatusEmprestimo.ATIVO);
    }

    public List<Emprestimo> listarHistoricoPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioId(usuarioId);
    }

    public List<Emprestimo> listarAtrasosPorUsuario(Long usuarioId) {
        return emprestimoRepository.findByUsuarioIdAndStatus(usuarioId, StatusEmprestimo.ATRASADO);
    }

    @Transactional
    public void forcarAtraso(Long id) {

        Emprestimo emp = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado"));

        emp.setDataDevolucao(emp.getDataDevolucao().minusDays(7)); // força atraso 7 dias
        emp.setStatus(true);

        emprestimoRepository.save(emp);
    }

    public void quitarMulta(Long id) {
        Emprestimo e = emprestimoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado"));

        e.setStatus(StatusEmprestimo.FINALIZADO);
        e.setDevolvido(true);

        emprestimoRepository.save(e);
    }
}