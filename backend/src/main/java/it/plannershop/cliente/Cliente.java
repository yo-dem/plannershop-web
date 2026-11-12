package it.plannershop.cliente;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Cliente di un'attività. Vedi SPECIFICA §6.2. */
@Entity
@Table(name = "cliente")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE cliente SET deleted_at = now() WHERE id = ? AND version = ?")
public class Cliente extends TenantScopedEntity {

    private String nome;
    private String cognome;
    private Short giornoNascita;
    private Short meseNascita;
    private String telefono;
    private String email;
    private String indirizzo;
    private String note;

    protected Cliente() {
    }

    public Cliente(String nome, String cognome) {
        this.nome = nome;
        this.cognome = cognome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public Short getGiornoNascita() {
        return giornoNascita;
    }

    public Short getMeseNascita() {
        return meseNascita;
    }

    /** Compleanno senza anno; entrambi null per toglierlo. */
    public void setCompleanno(Short giorno, Short mese) {
        this.giornoNascita = giorno;
        this.meseNascita = mese;
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

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
