package it.plannershop.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token issued at sign-in. Only the hash is in the database: whoever read
 * the table could not use the tokens. At every use the token is revoked and
 * replaced by a new one (rotation).
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Tenant the token was issued for; null for the SUPERADMIN without a tenant. */
    private Long tenantId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    protected RefreshToken() {
        // for JPA
    }

    public RefreshToken(Long userId, Long tenantId, String tokenHash, String userAgent, Instant expiresAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tokenHash = tokenHash;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
