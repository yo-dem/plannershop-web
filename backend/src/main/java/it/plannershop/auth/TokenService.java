package it.plannershop.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import it.plannershop.common.AppProperties;
import it.plannershop.security.JwtClaims;
import it.plannershop.security.SecurityProperties;
import it.plannershop.user.AppUser;
import it.plannershop.user.Membership;

/** Creates access tokens (JWT) and refresh tokens (random strings). */
@Service
class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final SecurityProperties props;
    private final AppProperties app;

    TokenService(JwtEncoder encoder, SecurityProperties props, AppProperties app) {
        this.encoder = encoder;
        this.props = props;
        this.app = app;
    }

    /**
     * @param membership current tenant and role; null for the SUPERADMIN
     *                   working at platform level
     */
    String createAccessToken(AppUser user, Membership membership) {
        Instant now = Instant.now();
        List<String> roles = new ArrayList<>();
        if (membership != null) {
            roles.add(membership.getRole().name());
        }
        // In LOCAL mode the platform does not exist: the role is never granted.
        if (user.isSuperadmin() && !app.isLocal()) {
            roles.add("SUPERADMIN");
        }
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("plannershop")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .claim(JwtClaims.ROLES, roles)
                .claim(JwtClaims.NAME, user.getName())
                .claim(JwtClaims.EMAIL, user.getEmail());
        if (membership != null) {
            claims.claim(JwtClaims.TENANT, membership.getTenant().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }

    /** 32 random bytes in URL-safe Base64: impossible to guess. */
    String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
