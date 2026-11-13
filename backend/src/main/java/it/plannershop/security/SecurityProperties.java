package it.plannershop.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security settings ({@code app.security.*} in application.yml).
 *
 * @param jwtSecret        secret used to sign the tokens; if empty a random one is
 *                         generated at every start (fine only in development)
 * @param accessTokenTtl   access token lifetime
 * @param refreshTokenTtl  refresh token lifetime ("remembered" session)
 * @param loginMaxAttempts failed attempts allowed within the window
 * @param loginWindow      time window for counting the attempts
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("10") int loginMaxAttempts,
        @DefaultValue("15m") Duration loginWindow) {
}
