package it.plannershop.tenant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import it.plannershop.common.TenantScopedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Entità di prova (solo test) che appartiene a un'attività. */
@Entity
@Table(name = "nota_prova")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE nota_prova SET deleted_at = now() WHERE id = ? AND version = ?")
public class NotaProva extends TenantScopedEntity {

    private String testo;

    protected NotaProva() {
    }

    public NotaProva(String testo) {
        this.testo = testo;
    }

    public String getTesto() {
        return testo;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }
}
