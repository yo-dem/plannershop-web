package it.plannershop.product;

import java.time.LocalDate;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.ItemPrices;
import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Product in stock. See SPECIFICA §6.3. */
@Entity
@Table(name = "product")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE product SET deleted_at = now() WHERE id = ? AND version = ?")
public class Product extends TenantScopedEntity {

    /** Date the goods were stocked, used by the statistics as purchase date. */
    private LocalDate stockDate;
    private String brand;
    private String description;
    /** Quantity in stock, never negative. */
    private int quantity;
    @Embedded
    private ItemPrices prices;
    private String notes;

    protected Product() {
    }

    public Product(String brand, String description) {
        this.brand = brand;
        this.description = description;
    }

    public LocalDate getStockDate() {
        return stockDate;
    }

    public void setStockDate(LocalDate stockDate) {
        this.stockDate = stockDate;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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
