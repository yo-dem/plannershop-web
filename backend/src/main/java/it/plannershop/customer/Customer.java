package it.plannershop.customer;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Customer of a tenant. See SPECIFICA §6.2. */
@Entity
@Table(name = "customer")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE customer SET deleted_at = now() WHERE id = ? AND version = ?")
public class Customer extends TenantScopedEntity {

    private String firstName;
    private String lastName;
    private Short birthDay;
    private Short birthMonth;
    private String phone;
    private String email;
    private String address;
    private String notes;

    protected Customer() {
    }

    public Customer(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Short getBirthDay() {
        return birthDay;
    }

    public Short getBirthMonth() {
        return birthMonth;
    }

    /** Birthday without a year; both null to remove it. */
    public void setBirthday(Short day, Short month) {
        this.birthDay = day;
        this.birthMonth = month;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
