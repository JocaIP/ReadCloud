package com.faculdade.biblioteca.Config;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.PapelUsuario;
import com.faculdade.biblioteca.repository.CategoriaRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer {

    private final UsuarioRepository usuarioRepo;
    private final CategoriaRepository categoriaRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepo,
                           CategoriaRepository categoriaRepo,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.categoriaRepo = categoriaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Transactional
    public ApplicationRunner initializer() {
        return args -> {
            String adminEmail = "admin@admin.com";

            if (usuarioRepo.findByEmail(adminEmail).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setEmail(adminEmail);
                admin.setSenha(passwordEncoder.encode("admin123"));
                admin.setPapel(PapelUsuario.ROLE_ADMIN.name());
                admin.setAtivo(true);
                usuarioRepo.save(admin);
            }

            String[] categorias = {"Ficção", "Programação", "Ciências", "História"};
            for (String nome : categorias) {
                if (categoriaRepo.findByNomeIgnoreCase(nome).isEmpty()) {
                    categoriaRepo.save(new Categoria(nome));
                }
            }
        };
    }
}
