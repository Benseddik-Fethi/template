package com.benseddik.template.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration de sécurité pour les tests d'intégration
 * <p>
 * Cette configuration désactive la sécurité OAuth2 pour les tests et fournit
 * des mocks pour les composants de sécurité.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Bean de test pour le JwtDecoder
     * <p>
     * Ce mock permet de simuler la validation JWT sans connexion réelle à Keycloak
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> createMockJwt();
    }

    /**
     * Configuration de sécurité désactivée pour les tests
     * <p>
     * Permet de tester les endpoints sans authentification OAuth2 réelle
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Crée un JWT mock pour les tests
     */
    private Jwt createMockJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user-id");
        claims.put("preferred_username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("name", "Test User");
        claims.put("realm_access", Map.of("roles", List.of("USER")));

        return new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );
    }

    /**
     * Crée un JWT mock avec un rôle spécifique pour les tests
     */
    public static Jwt createMockJwtWithRole(String role) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user-id");
        claims.put("preferred_username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("name", "Test User");
        claims.put("realm_access", Map.of("roles", List.of(role)));

        return new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );
    }

    /**
     * Crée un JWT mock avec plusieurs rôles pour les tests
     */
    public static Jwt createMockJwtWithRoles(List<String> roles) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user-id");
        claims.put("preferred_username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("name", "Test User");
        claims.put("realm_access", Map.of("roles", roles));

        return new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );
    }
}
