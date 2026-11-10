package it.plannershop.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token emesso al login. Nel database c'è solo l'hash: chi leggesse la
 * tabella non potrebbe usare i token. A ogni utilizzo il token viene revocato
 * e sostituito da uno nuovo (rotazione).
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Attività per cui è stato emesso il token; null per il SUPERADMIN senza attività. */
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
        // per JPA
    }

    public RefreshToken(Long userId, Long tenantId, String tokenHash, String userAgent, Instant expiresAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tokenHash = tokenHash;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
    }

    public boolean isRevocato() {
        return revokedAt != null;
    }

    public boolean isScaduto(Instant adesso) {
        return !expiresAt.isAfter(adesso);
    }

    public void revoca() {
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
