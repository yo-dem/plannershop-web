package it.plannershop.common;

/** Which price the user entered: the other one is calculated (SPECIFICA §7.1). */
public enum PriceBase {
    /** Net price entered, gross price calculated. */
    NET,
    /** Gross price (VAT included) entered, net price calculated. */
    GROSS
}
