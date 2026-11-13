package it.plannershop.user;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A person who signs in to the application. Does not belong to a tenant: the
 * roles in the single tenants are in {@link Membership}.
 */
@Entity
@Table(name = "app_user")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE app_user SET deleted_at = now() WHERE id = ? AND version = ?")
public class AppUser extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    /** Platform administrator: manages the tenants, not their data. */
    @Column(nullable = false)
    private boolean superadmin;

    /** Temporary password: the UI asks to change it at the first sign-in. */
    @Column(nullable = false)
    private boolean mustChangePassword;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String email, String name, String passwordHash) {
        this.email = email.trim().toLowerCase();
        this.name = name;
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String newHash, boolean temporary) {
        this.passwordHash = newHash;
        this.mustChangePassword = temporary;
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

    public boolean isSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
