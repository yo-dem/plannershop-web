package it.plannershop.prodotto;

import java.time.LocalDate;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.PrezziArticolo;
import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Prodotto in magazzino. Vedi SPECIFICA §6.3. */
@Entity
@Table(name = "prodotto")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE prodotto SET deleted_at = now() WHERE id = ? AND version = ?")
public class Prodotto extends TenantScopedEntity {

    /** Data di carico della merce, usata dalle statistiche come data di acquisto. */
    private LocalDate dataCarico;
    private String marca;
    private String descrizione;
    /** Giacenza in magazzino, mai negativa. */
    private int quantita;
    @Embedded
    private PrezziArticolo prezzi;
    private String note;

    protected Prodotto() {
    }

    public Prodotto(String marca, String descrizione) {
        this.marca = marca;
        this.descrizione = descrizione;
    }

    public LocalDate getDataCarico() {
        return dataCarico;
    }

    public void setDataCarico(LocalDate dataCarico) {
        this.dataCarico = dataCarico;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
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
