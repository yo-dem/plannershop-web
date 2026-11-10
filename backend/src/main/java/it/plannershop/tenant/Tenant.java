package it.plannershop.tenant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Un'attività (negozio, centro estetico, ...) servita dalla piattaforma. */
@Entity
@Table(name = "tenant")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE tenant SET deleted_at = now() WHERE id = ? AND version = ?")
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean attivo = true;

    @Column(nullable = false)
    private String fusoOrario = "Europe/Rome";

    protected Tenant() {
        // per JPA
    }

    public Tenant(String nome) {
        this.nome = nome;
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

    public String getFusoOrario() {
        return fusoOrario;
    }
}
