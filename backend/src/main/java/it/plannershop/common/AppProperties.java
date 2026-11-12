package it.plannershop.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Impostazioni generali ({@code app.*} in application.yml).
 *
 * @param modalita come viene installata l'applicazione (variabile APP_MODE)
 */
@ConfigurationProperties("app")
public record AppProperties(@DefaultValue("CLOUD") Modalita modalita) {

    /**
     * <ul>
     * <li>{@code CLOUD}: piattaforma con più attività, gestite dal SUPERADMIN.</li>
     * <li>{@code LOCALE}: installazione sul PC di un'unica attività. Il SUPERADMIN
     * e le API di piattaforma non esistono; al primo avvio si creano
     * direttamente l'attività e il suo ADMIN.</li>
     * </ul>
     */
    public enum Modalita {
        CLOUD, LOCALE
    }

    public boolean isLocale() {
        return modalita == Modalita.LOCALE;
    }
}
