package it.plannershop.common;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for all entities owned by a tenant (customers, products, ...).
 *
 * <p>{@code @TenantId}: Hibernate sets {@code tenant_id} on insert and adds
 * {@code tenant_id = ?} to every query, reading the current tenant from
 * {@link it.plannershop.tenant.TenantIdentifierResolver}. Application code must
 * not (and cannot) set it by hand.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @TenantId
    @Column(nullable = false, updatable = false)
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }
}
