package it.plannershop.fornitore;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Fornitore dell'attività. Vedi SPECIFICA §6.5. */
@Entity
@Table(name = "fornitore")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE fornitore SET deleted_at = now() WHERE id = ? AND version = ?")
public class Fornitore extends TenantScopedEntity {

    private String nome;
    private String indirizzo;
    private String telefono;
    private String email;
    /** Senza spazi, in maiuscolo, già verificato. */
    private String iban;
    private String note;

    protected Fornitore() {
    }

    public Fornitore(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
