package com.bd.xchoice.security;

import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Class to validate the audience of the JWT.
 */
@AllArgsConstructor
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audience;

    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        final OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);

        if (jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(error);
    }
}
