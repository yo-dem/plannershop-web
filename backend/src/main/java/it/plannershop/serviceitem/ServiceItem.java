package it.plannershop.serviceitem;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.ItemPrices;
import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Service offered by the business (e.g. a treatment). See SPECIFICA §6.4.
 * Called ServiceItem to avoid confusion with Spring's {@code @Service}.
 */
@Entity
@Table(name = "service_item")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE service_item SET deleted_at = now() WHERE id = ? AND version = ?")
public class ServiceItem extends TenantScopedEntity {

    private String name;
    private String description;
    @Embedded
    private ItemPrices prices;
    private String notes;

    protected ServiceItem() {
    }

    public ServiceItem(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemPrices getPrices() {
        return prices;
    }

    public void setPrices(ItemPrices prices) {
        this.prices = prices;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
