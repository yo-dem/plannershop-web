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
 * A user's membership in a tenant, with their role.
 *
 * <p>Does not use {@code @TenantId}: at sign-in the memberships of the user in
 * all tenants must be read, before a tenant has been chosen.
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
    private Role role;

    protected Membership() {
        // for JPA
    }

    public Membership(AppUser user, Tenant tenant, Role role) {
        this.user = user;
        this.tenant = tenant;
        this.role = role;
    }

    public AppUser getUser() {
        return user;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
