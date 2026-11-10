package it.plannershop.common;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base per tutte le entità che appartengono a un'attività (clienti, prodotti, ...).
 *
 * <p>{@code @TenantId}: Hibernate imposta da solo {@code tenant_id} in inserimento
 * e aggiunge {@code tenant_id = ?} a ogni query, leggendo il tenant corrente da
 * {@link it.plannershop.tenant.TenantIdentifierResolver}. Il codice applicativo
 * non deve (e non può) impostarlo a mano.
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
