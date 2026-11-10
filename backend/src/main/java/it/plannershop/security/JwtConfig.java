package it.plannershop.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Firma e verifica degli access token JWT con HMAC-SHA256 (chiave simmetrica):
 * è il backend stesso a emettere e verificare i token.
 */
@Configuration
class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    SecretKey jwtSecretKey(SecurityProperties props) throws NoSuchAlgorithmException {
        byte[] materiale;
        if (props.jwtSecret() == null || props.jwtSecret().isBlank()) {
            materiale = new byte[32];
            new SecureRandom().nextBytes(materiale);
            log.warn("JWT_SECRET non impostato: generato un segreto casuale. "
                    + "Gli accessi scadranno a ogni riavvio. Impostalo in produzione.");
        } else {
            materiale = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        }
        // SHA-256 → sempre 32 byte, la lunghezza richiesta da HS256.
        byte[] chiave = MessageDigest.getInstance("SHA-256").digest(materiale);
        return new SecretKeySpec(chiave, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
