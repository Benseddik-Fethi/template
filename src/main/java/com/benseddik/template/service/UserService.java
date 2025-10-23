package com.benseddik.template.service;

import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import com.benseddik.template.security.CurrentUserService;
import com.benseddik.template.service.dto.MeResponse;
import com.benseddik.template.service.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public MeResponse getCurrentUserProfile(Authentication auth) {
        if (!(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        AppUser user = currentUserService.ensureCurrentUser(auth);

        MeResponse response = MeResponse.builder()
                .subject(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .name(jwt.getClaimAsString("name"))
                .email(jwt.getClaimAsString("email"))
                .roles(auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .photoUrl(user.getPhotoUrl())
                .build();

        log.debug("Profile retrieved for user: {}", user.getEmail());
        return response;
    }

    public void updateProfile(UpdateProfileRequest request, Authentication auth) {
        AppUser user = currentUserService.ensureCurrentUser(auth);

        boolean updated = false;

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            String newDisplayName = request.getDisplayName().trim();
            if (!newDisplayName.equals(user.getDisplayName())) {
                user.setDisplayName(newDisplayName);
                updated = true;
                log.debug("Display name updated for user {}: {} -> {}",
                        user.getEmail(), user.getDisplayName(), newDisplayName);
            }
        }

        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            String newPhotoUrl = request.getPhotoUrl().trim();
            if (!newPhotoUrl.equals(user.getPhotoUrl())) {
                user.setPhotoUrl(newPhotoUrl);
                updated = true;
                log.debug("Photo URL updated for user {}", user.getEmail());
            }
        }

        if (updated) {
            userRepository.save(user);
            log.info("✅ Profile updated for user: {}", user.getEmail());
        } else {
            log.debug("No changes detected for user profile: {}", user.getEmail());
        }
    }

    public void deleteAccount(Authentication auth) {
        AppUser user = currentUserService.ensureCurrentUser(auth);

        log.warn("⚠️ Account deletion requested by user: {}", user.getEmail());

        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Account deletion not yet implemented. Please contact support."
        );
    }
}