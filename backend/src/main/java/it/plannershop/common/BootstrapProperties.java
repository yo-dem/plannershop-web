package it.plannershop.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Utenti creati al primo avvio ({@code app.bootstrap.*}), solo se il database
 * non ha ancora utenti. Una password vuota viene generata e scritta nel log.
 *
 * @param superadminEmail    modalità CLOUD: email del SUPERADMIN
 * @param superadminPassword modalità CLOUD: password del SUPERADMIN
 * @param attivitaNome       modalità LOCALE: nome dell'attività
 * @param adminEmail         modalità LOCALE: email dell'ADMIN dell'attività
 * @param adminNome          modalità LOCALE: nome visualizzato dell'ADMIN
 * @param adminPassword      modalità LOCALE: password provvisoria dell'ADMIN
 */
@ConfigurationProperties("app.bootstrap")
public record BootstrapProperties(
        @DefaultValue("admin@plannershop.local") String superadminEmail,
        String superadminPassword,
        @DefaultValue("La mia attività") String attivitaNome,
        @DefaultValue("admin@plannershop.local") String adminEmail,
        @DefaultValue("Amministratore") String adminNome,
        String adminPassword) {
}
