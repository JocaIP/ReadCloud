package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CsvImportUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Expressão que separa CSV respeitando valores entre aspas
    private static final Pattern SPLIT_CSV =
            Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    public Map<String, Object> importarUsuariosCSV(InputStream input) {

        Map<String, Object> resultado = new HashMap<>();
        List<String> erros = new ArrayList<>();
        List<Long> idsImportados = new ArrayList<>();

        int total = 0;
        int importados = 0;

        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String linha;
            boolean primeira = true;

            while ((linha = br.readLine()) != null) {
                total++;

                if (primeira) {
                    primeira = false; // pula cabeçalho
                    continue;
                }

                if (linha.trim().isEmpty()) continue;

                try {
                    String[] c = SPLIT_CSV.split(linha, -1);

                    if (c.length < 13) {
                        erros.add("Linha " + total + ": número incorreto de colunas.");
                        continue;
                    }

                    Usuario u = new Usuario();

                    u.setAtivo(Boolean.parseBoolean(clean(c[0])));
                    u.setCep(clean(c[1]));
                    u.setCidade(clean(c[2]));
                    u.setComplemento(clean(c[3]));
                    u.setDataCadastro(LocalDateTime.parse(clean(c[4])));
                    u.setDataNascimento(LocalDate.parse(clean(c[5])));
                    u.setEmail(clean(c[6]));
                    u.setEndereco(clean(c[7]));
                    u.setEstado(clean(c[8]));
                    u.setNome(clean(c[9]));
                    u.setPapel(clean(c[10]));
                    u.setSenha(clean(c[11])); // já bcrypt
                    u.setTelefone(clean(c[12]));

                    Usuario salvo = usuarioRepository.save(u);
                    importados++;
                    idsImportados.add(salvo.getId());

                } catch (Exception e) {
                    erros.add("Erro na linha " + total + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            resultado.put("erro", "Falha ao processar CSV: " + e.getMessage());
            return resultado;
        }

        resultado.put("importados", importados);
        resultado.put("totalProcessado", total);
        resultado.put("erros", erros);
        resultado.put("ids", idsImportados);

        return resultado;
    }

    private String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.trim();
    }
}
