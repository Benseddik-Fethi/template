package com.benseddik.template.security;


import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository userRepository;

    /**
     * Récupère ou crée l'utilisateur courant à partir du JWT
     */
    @Transactional
    public AppUser ensureCurrentUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No JWT authentication found");
        }

        String externalId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        return userRepository.findByExternalId(externalId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existingUser -> {
                            if (existingUser.getExternalId() == null ||
                                    !existingUser.getExternalId().equals(externalId)) {
                                existingUser.setExternalId(externalId);
                                return userRepository.save(existingUser);
                            }
                            return existingUser;
                        })
                        .orElseGet(() -> {
                            AppUser newUser = AppUser.builder()
                                    .externalId(externalId)
                                    .email(email)
                                    .displayName(name != null ? name : email)
                                    .build();
                            return userRepository.save(newUser);
                        }));
    }
}