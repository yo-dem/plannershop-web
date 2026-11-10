package it.plannershop.user;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.BaseEntity;
import it.plannershop.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Appartenenza di un utente a un'attività, con il suo ruolo.
 *
 * <p>Non usa {@code @TenantId}: al login bisogna leggere le appartenenze
 * dell'utente a tutte le attività, prima che un tenant sia stato scelto.
 */
@Entity
@Table(name = "membership")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE membership SET deleted_at = now() WHERE id = ? AND version = ?")
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ruolo ruolo;

    protected Membership() {
        // per JPA
    }

    public Membership(AppUser user, Tenant tenant, Ruolo ruolo) {
        this.user = user;
        this.tenant = tenant;
        this.ruolo = ruolo;
    }

    public AppUser getUser() {
        return user;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }
}
