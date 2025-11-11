package com.faculdade.biblioteca.Config;

import com.faculdade.biblioteca.modelo.Categoria;
import com.faculdade.biblioteca.modelo.Usuario;
import com.faculdade.biblioteca.modelo.PapelUsuario;
import com.faculdade.biblioteca.repository.CategoriaRepository;
import com.faculdade.biblioteca.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    public ApplicationRunner initializer() {
        return args -> {
            // cria admin se não existir
            String adminEmail = "admin@admin.com";
            if (usuarioRepo.findByEmail(adminEmail).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setEmail(adminEmail);
                admin.setSenha(passwordEncoder.encode("admin123")); // troque senha se quiser
                admin.setPapel(String.valueOf(PapelUsuario.ROLE_ADMIN));
                admin.setAtivo(true);
                usuarioRepo.save(admin);
                System.out.println("Admin criado: " + adminEmail + " / admin123");
            }

            // cria categorias iniciais se não existirem
            String[] categorias = {"Ficção", "Programação", "Ciências", "História"};
            for (String nome : categorias) {
                if (categoriaRepo.findByNomeIgnoreCase(nome).isEmpty()) {
                    Categoria c = new Categoria(nome);
                    categoriaRepo.save(c);
                }
            }
        };
    }
}
