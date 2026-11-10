package it.plannershop.tenant;

import java.util.Map;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import it.plannershop.security.JwtClaims;

/**
 * Dice a Hibernate qual è il tenant corrente, usato da {@code @TenantId}.
 *
 * <p>Ordine: {@link TenantContext} (uso interno) → claim {@code tenant} del token.
 * Se non c'è un tenant restituisce {@link #NESSUN_TENANT}: le query sulle entità
 * di un'attività non trovano nulla e gli inserimenti falliscono (vincolo FK).
 * Sicuro per default: mai "tutti i tenant".
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    public static final Long NESSUN_TENANT = -1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long forzato = TenantContext.corrente();
        if (forzato != null) {
            return forzato;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt
                && jwt.getToken().getClaims().get(JwtClaims.TENANT) instanceof Number tenant) {
            return tenant.longValue();
        }
        return NESSUN_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
