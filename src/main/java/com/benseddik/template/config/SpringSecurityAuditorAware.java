package com.benseddik.template.config;
import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of AuditorAware based on Spring Security.
 *
 * @author Fethi Benseddik
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    private final AppUserRepository userRepository;

    /**
     * Method called by Spring Security to automatically get the current auditor (username).
     * Used by Spring Data JPA to automatically set the current auditor (username) when saving a new object.
     *
     * @return the current auditor (username)
     * @author Fethi Benseddik
     * @see AuditorAware
     * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Cas 1 : Pas d'authentification
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            log.debug("No authentication found, using 'system' as auditor");
            return Optional.of("system");
        }

        // Cas 2 : Authentification JWT (OAuth2)
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String externalId = jwt.getSubject();
            log.debug("JWT authentication found, subject: {}", externalId);

            // Chercher l'utilisateur en base par son externalId
            return userRepository.findByExternalId(externalId)
                    .map(user -> {
                        log.debug("User found for audit: {}", user.getId());
                        return user.getId().toString();
                    })
                    .or(() -> {
                        log.warn("User with externalId {} not found in database, using 'system'", externalId);
                        return Optional.of("system");
                    });
        }

        // Cas 3 : Autre type d'authentification (fallback)
        log.warn("Unknown authentication type: {}, using 'system'",
                authentication.getPrincipal().getClass().getName());
        return Optional.of("system");
    }
}

