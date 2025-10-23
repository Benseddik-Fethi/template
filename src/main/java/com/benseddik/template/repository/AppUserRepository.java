package com.benseddik.template.repository;

import com.benseddik.template.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByExternalId(String externalId);
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
