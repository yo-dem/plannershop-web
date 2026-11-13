package it.plannershop.common;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** IBAN normalization and validation (SPECIFICA §7.4). See also {@link Iban}. */
public final class Ibans {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");

    private Ibans() {
    }

    /** Spaces removed and letters upper-cased ("it60 x054..." → "IT60X054..."); empty → null. */
    public static String normalize(String iban) {
        if (iban == null) {
            return null;
        }
        String s = iban.replaceAll("\\s", "").toUpperCase();
        return s.isEmpty() ? null : s;
    }

    /** Format and check digits (ISO 13616: Mod-97 must be 1). */
    public static boolean isValid(String iban) {
        if (iban == null || !FORMAT.matcher(iban).matches()) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder digits = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
