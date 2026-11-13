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
 * Signs and verifies JWT access tokens with HMAC-SHA256 (symmetric key): the
 * backend itself issues and verifies the tokens.
 */
@Configuration
class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    SecretKey jwtSecretKey(SecurityProperties props) throws NoSuchAlgorithmException {
        byte[] material;
        if (props.jwtSecret() == null || props.jwtSecret().isBlank()) {
            material = new byte[32];
            new SecureRandom().nextBytes(material);
            log.warn("JWT_SECRET not set: generated a random secret. "
                    + "Sessions will expire at every restart. Set it in production.");
        } else {
            material = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        }
        // SHA-256 → always 32 bytes, the length required by HS256.
        byte[] key = MessageDigest.getInstance("SHA-256").digest(material);
        return new SecretKeySpec(key, "HmacSHA256");
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
