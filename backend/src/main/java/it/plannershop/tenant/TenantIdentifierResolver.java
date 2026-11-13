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
 * Tells Hibernate which tenant is current, used by {@code @TenantId}.
 *
 * <p>Order: {@link TenantContext} (internal use) → {@code tenant} claim of the token.
 * Without a tenant it returns {@link #NO_TENANT}: queries on tenant entities find
 * nothing and inserts fail (FK constraint). Safe by default: never "all tenants".
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    public static final Long NO_TENANT = -1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long forced = TenantContext.current();
        if (forced != null) {
            return forced;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt
                && jwt.getToken().getClaims().get(JwtClaims.TENANT) instanceof Number tenant) {
            return tenant.longValue();
        }
        return NO_TENANT;
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
