package it.plannershop.user;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Persona che accede all'applicazione. Non appartiene a un tenant: i ruoli
 * nelle singole attività sono in {@link Membership}.
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
    private String nome;

    @Column(nullable = false)
    private boolean attivo = true;

    /** Amministratore della piattaforma: gestisce le attività, non i loro dati. */
    @Column(nullable = false)
    private boolean superadmin;

    /** Password provvisoria: l'interfaccia chiede di cambiarla al primo accesso. */
    @Column(nullable = false)
    private boolean deveCambiarePassword;

    protected AppUser() {
        // per JPA
    }

    public AppUser(String email, String nome, String passwordHash) {
        this.email = email.trim().toLowerCase();
        this.nome = nome;
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void cambiaPassword(String nuovoHash, boolean provvisoria) {
        this.passwordHash = nuovoHash;
        this.deveCambiarePassword = provvisoria;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public boolean isSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    public boolean isDeveCambiarePassword() {
        return deveCambiarePassword;
    }
}
