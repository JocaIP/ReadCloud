package com.faculdade.biblioteca.Config;

import com.faculdade.biblioteca.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 🔥 HANDLER PARA REDIRECIONAR ADMIN → /admin/dashboard
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {

            var roles = authentication.getAuthorities();
            String redirect = "/"; // usuário comum

            if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"))) {
                redirect = "/admin/painel"; // ROTA CORRETA
            }

            response.sendRedirect(redirect);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authProvider())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.GET, "/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/Usuario/cadastrar").permitAll()

                        .requestMatchers("/emprestimo/confirmado").permitAll()

                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/", "/login", "/cadastro/**",
                                "/livros", "/livros/**"
                        ).permitAll()

                        .requestMatchers("/carrinho/**").authenticated()

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler()) // **AQUI É O PULO DO GATO**
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
