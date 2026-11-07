package com.shoploc.shoplocidentityservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Règles d'accès HTTP du service.
 * API REST sans session : pas de cookie, donc pas de protection CSRF nécessaire.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // US 1.1 : l'inscription est ouverte aux visiteurs
                        .requestMatchers(HttpMethod.POST, "/api/identity/auth/register").permitAll()
                        // US 1.2 : la connexion aussi (on n'a pas encore de token)
                        .requestMatchers(HttpMethod.POST, "/api/identity/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
