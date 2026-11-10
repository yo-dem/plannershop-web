package it.plannershop.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Impostazioni di sicurezza ({@code app.security.*} in application.yml).
 *
 * @param jwtSecret         segreto per firmare i token; se vuoto se ne genera uno
 *                          casuale a ogni avvio (va bene solo in sviluppo)
 * @param accessTokenTtl    durata dell'access token
 * @param refreshTokenTtl   durata del refresh token (sessione "ricordata")
 * @param loginMaxTentativi tentativi falliti consentiti nella finestra
 * @param loginFinestra     finestra temporale per il conteggio dei tentativi
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("10") int loginMaxTentativi,
        @DefaultValue("15m") Duration loginFinestra) {
}
