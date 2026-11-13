package it.plannershop.common;

/** Helpers for text received from the frontend. */
public final class TextUtils {

    private TextUtils() {
    }

    /** Leading and trailing spaces removed; empty text = missing field (null). */
    public static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}
