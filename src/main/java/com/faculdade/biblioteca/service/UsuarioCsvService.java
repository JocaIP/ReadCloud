package com.faculdade.biblioteca.service;

import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class UsuarioCsvService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public int importarCsv(MultipartFile arquivo) throws Exception {

        if (arquivo.isEmpty()) {
            throw new Exception("O arquivo está vazio");
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8)
        );

        String linha;
        int count = 0;

        // Pular o cabeçalho
        reader.readLine();

        while ((linha = reader.readLine()) != null) {

            // Divide considerando CSV simples (sem vírgulas dentro das colunas)
            String[] campos = linha.split(",");

            if (campos.length < 5) continue;

            String nome = campos[0].trim();
            String email = campos[1].trim();
            String senha = campos[2].trim();
            String ativoStr = campos[3].trim();
            String roleStr = campos[4].trim().toUpperCase();

            Usuario u = new Usuario();
            u.setNome(nome);
            u.setEmail(email);
            u.setAtivo(Boolean.parseBoolean(ativoStr));

            // Senha aleatória caso não enviada
            if (senha.isEmpty()) {
                senha = UUID.randomUUID().toString().substring(0, 8);
            }

            u.setSenha(passwordEncoder.encode(senha));

            // Role padrão USER
            if (roleStr.isEmpty()) {
                roleStr = "USER";
            }

            u.setRole("ROLE_" + roleStr);

            usuarioRepository.save(u);
            count++;
        }

        return count;
    }
}
