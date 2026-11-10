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

import it.plannershop.security.JwtClaims;
import it.plannershop.security.SecurityProperties;
import it.plannershop.user.AppUser;
import it.plannershop.user.Membership;

/** Creazione degli access token (JWT) e dei refresh token (stringhe casuali). */
@Service
class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final SecurityProperties props;

    TokenService(JwtEncoder encoder, SecurityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    /**
     * @param membership attività e ruolo correnti; null per il SUPERADMIN
     *                   che lavora a livello di piattaforma
     */
    String creaAccessToken(AppUser user, Membership membership) {
        Instant adesso = Instant.now();
        List<String> ruoli = new ArrayList<>();
        if (membership != null) {
            ruoli.add(membership.getRuolo().name());
        }
        if (user.isSuperadmin()) {
            ruoli.add("SUPERADMIN");
        }
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("plannershop")
                .subject(user.getId().toString())
                .issuedAt(adesso)
                .expiresAt(adesso.plus(props.accessTokenTtl()))
                .claim(JwtClaims.ROLES, ruoli)
                .claim(JwtClaims.NOME, user.getNome())
                .claim(JwtClaims.EMAIL, user.getEmail());
        if (membership != null) {
            claims.claim(JwtClaims.TENANT, membership.getTenant().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    long durataAccessTokenSecondi() {
        return props.accessTokenTtl().toSeconds();
    }

    /** 32 byte casuali in Base64 URL-safe: impossibili da indovinare. */
    String nuovoRefreshToken() {
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
