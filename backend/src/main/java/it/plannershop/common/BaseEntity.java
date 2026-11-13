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
 * Fields shared by all entities: id, auditing, optimistic locking and soft delete.
 *
 * <p>Soft delete: no row is ever physically deleted. Call {@link #delete(Long)}
 * and save; every concrete entity has {@code @SQLRestriction("deleted_at IS NULL")},
 * so deleted rows disappear from all queries, and {@code @SQLDelete}, so even an
 * accidental {@code repository.delete()} becomes an UPDATE.
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

    /** Marks the entity as deleted by the given user. Idempotent. */
    public void delete(Long deletedByUserId) {
        if (deletedAt == null) {
            deletedAt = Instant.now();
            deletedBy = deletedByUserId;
        }
    }

    public boolean isDeleted() {
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
