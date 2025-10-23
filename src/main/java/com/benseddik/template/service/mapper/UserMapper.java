package com.benseddik.template.service.mapper;

import com.benseddik.template.domain.AppUser;
import com.benseddik.template.service.dto.MeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Mapper pour convertir les entités AppUser vers les DTOs
 *
 * Exemple d'utilisation de MapStruct dans le template.
 * MapStruct génère automatiquement l'implémentation lors de la compilation.
 *
 * @see <a href="https://mapstruct.org/">MapStruct Documentation</a>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Convertir AppUser + JWT vers MeResponse
     *
     * Note: Cette méthode est un exemple. Dans le code actuel, nous construisons
     * MeResponse manuellement dans UserService car nous avons besoin de données
     * du JWT qui ne sont pas dans AppUser.
     *
     * @param user L'utilisateur de la base de données
     * @return La réponse contenant les informations utilisateur
     */
    @Mapping(source = "displayName", target = "name")
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "issuedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    MeResponse toMeResponse(AppUser user);
}
