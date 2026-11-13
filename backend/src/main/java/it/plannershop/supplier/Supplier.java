package it.plannershop.supplier;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Supplier of a tenant. See SPECIFICA §6.5. */
@Entity
@Table(name = "supplier")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE supplier SET deleted_at = now() WHERE id = ? AND version = ?")
public class Supplier extends TenantScopedEntity {

    private String name;
    private String address;
    private String phone;
    private String email;
    /** No spaces, upper case, already validated. */
    private String iban;
    private String notes;

    protected Supplier() {
    }

    public Supplier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
