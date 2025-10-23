package com.benseddik.template.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Slf4j
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${app.security.jwt.clock-skew-seconds:60}")
    private long clockSkewSeconds;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'none'; " +
                                                "frame-ancestors 'none'; " +
                                                "base-uri 'none'; " +
                                                "form-action 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data:; " +
                                                "connect-src 'self'; " +
                                                "font-src 'self'"))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .contentTypeOptions(Customizer.withDefaults())
                        .crossOriginOpenerPolicy(Customizer.withDefaults())
                        .crossOriginEmbedderPolicy(Customizer.withDefaults())
                        .crossOriginResourcePolicy(corp -> corp
                                .policy(CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/me").authenticated()
                        .requestMatchers("/images/**").authenticated()
                        .requestMatchers("/admin/**").hasAnyRole("MODERATOR", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractKeycloakRoles);
        log.debug("JwtAuthenticationConverter initialized");
        return converter;
    }

    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> c) {
                c.forEach(r -> {
                    String role = r.toString().toUpperCase().trim();
                    if (!role.isEmpty()) {
                        roles.add(role);
                    }
                });
                log.debug("Extracted realm roles: {}", roles);
            }

            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof Map<?, ?> resMap) {
                Object clientRoles = resMap.get(keycloakClientId);
                if (clientRoles instanceof Map<?, ?> clientMap &&
                        clientMap.get("roles") instanceof Collection<?> rolesColl) {
                    rolesColl.forEach(r -> {
                        String role = r.toString().toUpperCase().trim();
                        if (!role.isEmpty()) {
                            roles.add(role);
                        }
                    });
                    log.debug("Extracted client roles for '{}': {}", keycloakClientId, rolesColl);
                }
            }

            roles.removeIf(r ->
                    r.startsWith("DEFAULT-ROLES-") ||
                            r.equals("OFFLINE_ACCESS") ||
                            r.equals("UMA_AUTHORIZATION") ||
                            r.equals("MANAGE-ACCOUNT") ||
                            r.equals("MANAGE-ACCOUNT-LINKS")
            );

            log.debug("Final authorities for user '{}': {}", jwt.getSubject(), roles);

        } catch (Exception e) {
            log.error("Error extracting roles from JWT", e);
        }

        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        String[] originsArray = allowedOrigins.split(",");
        List<String> origins = Arrays.stream(originsArray)
                .map(String::trim)
                .toList();

        config.setAllowedOrigins(origins);
        log.info("CORS allowed origins: {}", origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Request-Id",
                "X-Correlation-Id"
        ));
        config.setExposedHeaders(List.of(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset",
                "X-Correlation-Id"
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public NimbusJwtDecoder jwtDecoder() {
        List<String> validIssuers = Arrays.asList(
                issuerUri,
                "http://10.0.2.2:8081/realms/" + keycloakRealm,
                "http://127.0.0.1:8081/realms/" + keycloakRealm
        );

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
                issuerUri + "/protocol/openid-connect/certs"
        ).build();

        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(validIssuers);
        OAuth2TokenValidator<Jwt> timestampValidator =
                new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds));

        OAuth2TokenValidator<Jwt> withValidators = new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                timestampValidator
        );

        jwtDecoder.setJwtValidator(withValidators);

        log.info("JWT Decoder configured - Clock skew: {}s, Valid issuers: {}",
                clockSkewSeconds, validIssuers.size());

        return jwtDecoder;
    }
}