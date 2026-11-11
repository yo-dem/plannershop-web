package it.plannershop.common;

import java.security.SecureRandom;

/** Password provvisorie generate dal sistema (primo avvio, ripristino). */
public final class PasswordCasuale {

    /** Senza caratteri ambigui (0/O, 1/l/I) per poterla dettare o ricopiare. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordCasuale() {
    }

    public static String genera() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
