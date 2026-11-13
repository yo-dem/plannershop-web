package it.plannershop.tenant;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import it.plannershop.security.CurrentUser;

/** Date and time zone of the current tenant (not the server's). */
@Component
public class TenantClock {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Rome");

    private final TenantRepository tenants;

    TenantClock(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public ZoneId zone() {
        return CurrentUser.tenantId()
                .flatMap(tenants::findById)
                .map(t -> ZoneId.of(t.getTimeZone()))
                .orElse(DEFAULT_ZONE);
    }

    public LocalDate today() {
        return LocalDate.now(zone());
    }
}
