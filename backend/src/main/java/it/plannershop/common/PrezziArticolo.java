package it.plannershop.common;

import java.math.BigDecimal;

import it.plannershop.common.Prezzi.NettoIvato;
import jakarta.persistence.Embeddable;

/**
 * Aliquota e prezzi di un prodotto o di un servizio (colonne aliquota_iva,
 * prezzo_netto, prezzo_ivato, prezzo_vendita). Vedi SPECIFICA §7.1.
 */
@Embeddable
public class PrezziArticolo {

    private BigDecimal aliquotaIva;
    private BigDecimal prezzoNetto;
    private BigDecimal prezzoIvato;
    private BigDecimal prezzoVendita;

    protected PrezziArticolo() {
    }

    private PrezziArticolo(BigDecimal aliquotaIva, BigDecimal prezzoNetto, BigDecimal prezzoIvato,
            BigDecimal prezzoVendita) {
        this.aliquotaIva = aliquotaIva;
        this.prezzoNetto = prezzoNetto;
        this.prezzoIvato = prezzoIvato;
        this.prezzoVendita = prezzoVendita;
    }

    /** Dai dati ricevuti: il prezzo non inserito dall'utente viene ricalcolato qui (fa fede il backend). */
    public static PrezziArticolo di(RichiestaPrezzi req) {
        NettoIvato p = Prezzi.calcola(req.calcolaDa(), req.prezzoNetto(), req.prezzoIvato(), req.aliquotaIva());
        return new PrezziArticolo(req.aliquotaIva(), p.netto(), p.ivato(), req.prezzoVendita());
    }

    public BigDecimal getAliquotaIva() {
        return aliquotaIva;
    }

    public BigDecimal getPrezzoNetto() {
        return prezzoNetto;
    }

    public BigDecimal getPrezzoIvato() {
        return prezzoIvato;
    }

    public BigDecimal getPrezzoVendita() {
        return prezzoVendita;
    }
}
