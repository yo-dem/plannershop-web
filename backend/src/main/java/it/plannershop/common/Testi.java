package it.plannershop.common;

/** Utilità per i testi ricevuti dal frontend. */
public final class Testi {

    private Testi() {
    }

    /** Spazi iniziali e finali tolti; testo vuoto = campo assente (null). */
    public static String pulisci(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}
