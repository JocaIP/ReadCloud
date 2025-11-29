package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Livro;
import com.faculdade.biblioteca.repository.CategoriaRepository;
import com.faculdade.biblioteca.repository.LivroRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CsvImportService {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private static final Pattern SPLIT_CSV = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    private static final Pattern SPLIT_SEMICOLON = Pattern.compile(";(?![^\\\"]*\\\")");

    private static final String UPLOAD_DIR = "uploads/";

    @Transactional
    public Map<String, Object> importarLivrosCSV(InputStream csvStream) {
        Map<String, Object> resultado = new HashMap<>();
        List<String> erros = new ArrayList<>();
        List<Long> idsImportados = new ArrayList<>();

        int totalLinhas = 0;
        int importados = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            String linha;
            boolean primeiraLinha = true;

            while ((linha = br.readLine()) != null) {
                totalLinhas++;

                if (primeiraLinha) {
                    primeiraLinha = false;
                    if (linha.toLowerCase().contains("titulo")) continue;
                }

                if (linha.trim().isEmpty()) continue;

                try {
                    String[] campos = splitCsvSmart(linha);

                    if (campos.length < 3) {
                        erros.add("Linha " + totalLinhas + " inválida: mínimo 3 colunas.");
                        continue;
                    }

                    String titulo = normalize(campos[0]);
                    String autor = normalize(campos[1]);
                    String quantidadeStr = normalize(campos[2]);

                    if (titulo.isBlank() || autor.isBlank()) {
                        erros.add("Linha " + totalLinhas + ": título/autor vazio.");
                        continue;
                    }

                    int quantidade;
                    try {
                        quantidade = Integer.parseInt(quantidadeStr);
                    } catch (Exception e) {
                        erros.add("Linha " + totalLinhas + ": quantidade inválida.");
                        continue;
                    }

                    String editora = campos.length > 3 ? normalize(campos[3]) : null;

                    Integer ano = null;
                    if (campos.length > 4 && !normalize(campos[4]).isBlank()) {
                        try {
                            ano = Integer.parseInt(normalize(campos[4]));
                        } catch (Exception ignored) {}
                    }

                    String isbn = campos.length > 5 ? normalize(campos[5]) : null;
                    String descricao = campos.length > 6 ? normalize(campos[6]) : null;

                    Categoria categoria = null;
                    if (campos.length > 7 && !normalize(campos[7]).isBlank()) {
                        String cat = normalize(campos[7]);

                        try {
                            categoria = categoriaRepository.findById(Long.parseLong(cat)).orElse(null);
                        } catch (Exception e) {
                            categoria = categoriaRepository.findByNomeIgnoreCase(cat).orElse(null);
                        }
                    }

                    // --------------- IMPORTAR IMAGEM VIA URL ---------------
                    String imagemURL = campos.length > 8 ? normalize(campos[8]) : null;
                    String nomeImagemSalva = null;

                    if (imagemURL != null && !imagemURL.isBlank()) {
                        try {
                            nomeImagemSalva = baixarImagemDaURL(imagemURL);
                        } catch (Exception e) {
                            erros.add("Linha " + totalLinhas + ": falha ao baixar imagem -> " + imagemURL);
                        }
                    }

                    Livro livro = new Livro();
                    livro.setTitulo(titulo);
                    livro.setAutor(autor);
                    livro.setQuantidade(quantidade);
                    livro.setEditora(editora);
                    livro.setAnoPublicacao(ano);
                    livro.setIsbn(isbn);
                    livro.setDescricao(descricao);
                    livro.setCategoria(categoria);
                    livro.setImagemCapa(nomeImagemSalva);

                    Livro salvo = livroRepository.save(livro);
                    importados++;
                    idsImportados.add(salvo.getId());

                } catch (Exception ex) {
                    erros.add("Erro na linha " + totalLinhas + ": " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            resultado.put("erro", "Falha ao ler arquivo: " + e.getMessage());
        }

        resultado.put("importados", importados);
        resultado.put("erros", erros);
        resultado.put("linhasProcessadas", totalLinhas);
        resultado.put("detalhesImportados", idsImportados);

        return resultado;
    }

    private static long countChar(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }

    private String[] splitCsvSmart(String linha) {
        long countComma = countChar(linha, ',');
        long countSemi = countChar(linha, ';');

        String[] raw;
        if (countSemi > countComma) {
            raw = SPLIT_SEMICOLON.split(linha, -1);
        } else {
            raw = SPLIT_CSV.split(linha, -1);
        }

        for (int i = 0; i < raw.length; i++) {
            raw[i] = normalize(raw[i]);
        }
        return raw;
    }

    private static String normalize(String campo) {
        if (campo == null) return "";
        String s = campo.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s.trim();
    }

    // ---------------------- DOWNLOAD DE IMAGENS VIA URL ----------------------
    private String baixarImagemDaURL(String urlStr) throws IOException {

        // Detecta extensão da imagem
        String extensao = ".jpg";
        if (urlStr.toLowerCase().endsWith(".png")) extensao = ".png";

        String nomeArquivo = UUID.randomUUID() + extensao;

        // Garante que a pasta existe
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Faz o download corretamente
        try (InputStream in = new URL(urlStr).openStream()) {
            Path destino = Paths.get(UPLOAD_DIR, nomeArquivo); // <-- CORRETO
            Files.copy(in, destino);
        }

        return nomeArquivo;
    }

    public Map<String, Object> importarLivrosCSV(MultipartFile arquivo) {
        try {
            return importarLivrosCSV(arquivo.getInputStream());
        } catch (IOException e) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("importados", 0);
            erro.put("erros", List.of("Falha ao ler arquivo: " + e.getMessage()));
            erro.put("linhasProcessadas", 0);
            erro.put("detalhesImportados", List.of());
            return erro;
        }
    }
}
