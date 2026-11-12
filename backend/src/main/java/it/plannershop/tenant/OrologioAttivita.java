package it.plannershop.tenant;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import it.plannershop.security.CurrentUser;

/** Data e fuso orario dell'attività corrente (non quelli del server). */
@Component
public class OrologioAttivita {

    private static final ZoneId PREDEFINITO = ZoneId.of("Europe/Rome");

    private final TenantRepository tenants;

    OrologioAttivita(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public ZoneId fusoOrario() {
        return CurrentUser.tenantId()
                .flatMap(tenants::findById)
                .map(t -> ZoneId.of(t.getFusoOrario()))
                .orElse(PREDEFINITO);
    }

    public LocalDate oggi() {
        return LocalDate.now(fusoOrario());
    }
}
