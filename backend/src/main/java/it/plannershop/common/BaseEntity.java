package it.plannershop.common;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Campi comuni a tutte le entità: id, tracciamento, optimistic locking
 * ed eliminazione logica.
 *
 * <p>Eliminazione logica (soft delete): nessuna riga si cancella davvero.
 * Si chiama {@link #elimina(Long)} e si salva; ogni entità concreta ha
 * {@code @SQLRestriction("deleted_at IS NULL")}, così le righe eliminate
 * spariscono da tutte le query, e {@code @SQLDelete}, così anche un
 * {@code repository.delete()} accidentale diventa un UPDATE.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    private Instant deletedAt;

    private Long deletedBy;

    /** Segna l'entità come eliminata dall'utente indicato. Idempotente. */
    public void elimina(Long eliminataDa) {
        if (deletedAt == null) {
            deletedAt = Instant.now();
            deletedBy = eliminataDa;
        }
    }

    public boolean isEliminata() {
        return deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }
}
