package com.benseddik.template.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record JwtIssuerValidator(List<String> validIssuers) implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getIssuer().toString();

        if (validIssuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                String.format("The iss claim is not valid. Expected one of %s but got %s",
                        validIssuers, issuer),
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}