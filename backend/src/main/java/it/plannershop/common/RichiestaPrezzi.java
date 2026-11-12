package it.plannershop.common;

import java.math.BigDecimal;

import it.plannershop.common.Prezzi.CalcolaDa;
import jakarta.validation.constraints.AssertTrue;

/**
 * Prezzi inviati dal frontend per un prodotto o un servizio. L'utente inserisce
 * il netto oppure l'ivato ({@link #calcolaDa()}): l'altro viene ricalcolato.
 * I vincoli sui singoli campi sono nei record che implementano l'interfaccia.
 */
public interface RichiestaPrezzi {

    BigDecimal aliquotaIva();

    CalcolaDa calcolaDa();

    BigDecimal prezzoNetto();

    BigDecimal prezzoIvato();

    BigDecimal prezzoVendita();

    /** Il prezzo da cui si calcola l'altro deve esserci (errore nel campo "prezzo"). */
    @AssertTrue(message = "Inserisci il prezzo")
    default boolean isPrezzo() {
        if (calcolaDa() == null) {
            return true; // segnalato da @NotNull su calcolaDa
        }
        return switch (calcolaDa()) {
            case NETTO -> prezzoNetto() != null;
            case IVATO -> prezzoIvato() != null;
        };
    }
}
