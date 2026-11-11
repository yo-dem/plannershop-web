package it.plannershop.platform;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.platform.PlatformDtos.ModificaTenantRequest;
import it.plannershop.platform.PlatformDtos.NuovoTenantRequest;
import it.plannershop.platform.PlatformDtos.TenantDto;
import jakarta.validation.Valid;

/**
 * Attività della piattaforma. {@code @PreAuthorize} a livello di classe vale
 * per tutti i metodi (in aggiunta alla regola su /api/platform/** in SecurityConfig).
 */
@RestController
@ConditionalOnProperty(name = "app.modalita", havingValue = "cloud", matchIfMissing = true)
@RequestMapping("/api/platform/tenants")
@PreAuthorize("hasRole('SUPERADMIN')")
public class PlatformTenantController {

    private final PlatformTenantService service;

    PlatformTenantController(PlatformTenantService service) {
        this.service = service;
    }

    @GetMapping
    List<TenantDto> elenco() {
        return service.elenco();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TenantDto crea(@Valid @RequestBody NuovoTenantRequest body) {
        return service.crea(body);
    }

    @PutMapping("/{id}")
    TenantDto modifica(@PathVariable Long id, @Valid @RequestBody ModificaTenantRequest body) {
        return service.modifica(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable Long id) {
        service.elimina(id);
    }
}
