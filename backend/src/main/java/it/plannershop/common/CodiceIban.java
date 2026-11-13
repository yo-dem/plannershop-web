package it.plannershop.common;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** Normalizzazione e verifica degli IBAN (SPECIFICA §7.4). Vedi anche {@link Iban}. */
public final class CodiceIban {

    private CodiceIban() {
    }

    private static final Pattern FORMATO = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");

    /** Spazi tolti e lettere in maiuscolo ("it60 x054..." → "IT60X054..."); vuoto → null. */
    public static String normalizza(String iban) {
        if (iban == null) {
            return null;
        }
        String s = iban.replaceAll("\\s", "").toUpperCase();
        return s.isEmpty() ? null : s;
    }

    /** Formato e cifre di controllo (ISO 13616). */
    public static boolean valido(String iban) {
        if (iban == null || !FORMATO.matcher(iban).matches()) {
            return false;
        }
        String riordinato = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeri = new StringBuilder();
        for (char c : riordinato.toCharArray()) {
            numeri.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(numeri.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
