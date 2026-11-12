package it.plannershop.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcolo tra prezzo netto e prezzo IVA inclusa, per prodotti e servizi.
 * Vedi SPECIFICA §7.1: arrotondamento per eccesso al centesimo, come nel
 * vecchio programma, ma con calcolo esatto (BigDecimal, niente double).
 */
public final class Prezzi {

    private static final BigDecimal CENTO = BigDecimal.valueOf(100);

    private Prezzi() {
    }

    /** Quale dei due prezzi ha inserito l'utente: l'altro viene calcolato. */
    public enum CalcolaDa {
        NETTO, IVATO
    }

    /** Prezzo netto e prezzo IVA inclusa, coerenti con l'aliquota. */
    public record NettoIvato(BigDecimal netto, BigDecimal ivato) {
    }

    /** {@code ivato = ceil(netto × (1 + aliquota/100))} al centesimo. */
    public static BigDecimal ivato(BigDecimal netto, BigDecimal aliquota) {
        return netto.multiply(fattore(aliquota)).setScale(2, RoundingMode.CEILING);
    }

    /** {@code netto = ceil(ivato / (1 + aliquota/100))} al centesimo. */
    public static BigDecimal netto(BigDecimal ivato, BigDecimal aliquota) {
        return ivato.divide(fattore(aliquota), 2, RoundingMode.CEILING);
    }

    /** Parte dal prezzo inserito ({@code da}) e calcola l'altro. */
    public static NettoIvato calcola(CalcolaDa da, BigDecimal netto, BigDecimal ivato, BigDecimal aliquota) {
        return switch (da) {
            case NETTO -> new NettoIvato(netto.setScale(2, RoundingMode.CEILING), ivato(netto, aliquota));
            case IVATO -> new NettoIvato(netto(ivato, aliquota), ivato.setScale(2, RoundingMode.CEILING));
        };
    }

    private static BigDecimal fattore(BigDecimal aliquota) {
        return BigDecimal.ONE.add(aliquota.divide(CENTO));
    }
}
