package it.plannershop.tenant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A business (shop, beauty center, ...) served by the platform. */
@Entity
@Table(name = "tenant")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE tenant SET deleted_at = now() WHERE id = ? AND version = ?")
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String timeZone = "Europe/Rome";

    protected Tenant() {
        // for JPA
    }

    public Tenant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
