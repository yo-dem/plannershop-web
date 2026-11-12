package it.plannershop.servizio;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.PrezziArticolo;
import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Servizio offerto dall'attività (es. un trattamento). Vedi SPECIFICA §6.4. */
@Entity
@Table(name = "servizio")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE servizio SET deleted_at = now() WHERE id = ? AND version = ?")
public class Servizio extends TenantScopedEntity {

    private String nome;
    private String descrizione;
    @Embedded
    private PrezziArticolo prezzi;
    private String note;

    protected Servizio() {
    }

    public Servizio(String nome, String descrizione) {
        this.nome = nome;
        this.descrizione = descrizione;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public PrezziArticolo getPrezzi() {
        return prezzi;
    }

    public void setPrezzi(PrezziArticolo prezzi) {
        this.prezzi = prezzi;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
