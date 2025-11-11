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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authProvider())
                .authorizeHttpRequests(auth -> auth
                        // 🔹 PERMITIR CADASTRO COMPLETO
                        .requestMatchers(HttpMethod.GET, "/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/Usuario/cadastrar").permitAll()

                        // Recursos públicos
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/", "/login", "/cadastro/**",
                                "/livros", "/livros/**"
                        ).permitAll()

                        // Área administrativa
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Resto precisa de autenticação
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                // 🔹 CORREÇÃO: REMOVER A EXCEÇÃO DO CSRF PARA /cadastro
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**") // Apenas H2
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}