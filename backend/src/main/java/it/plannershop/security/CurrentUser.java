package it.plannershop.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import it.plannershop.common.ApiException;

/** Accesso comodo ai dati dell'utente autenticato nella richiesta corrente. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        return Long.valueOf(jwt().getSubject());
    }

    public static Optional<Long> tenantId() {
        Object t = jwt().getClaims().get(JwtClaims.TENANT);
        return t instanceof Number n ? Optional.of(n.longValue()) : Optional.empty();
    }

    private static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw ApiException.nonAutenticato("Accesso richiesto");
    }
}
