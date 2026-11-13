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

import it.plannershop.platform.PlatformDtos.NewTenantRequest;
import it.plannershop.platform.PlatformDtos.TenantDto;
import it.plannershop.platform.PlatformDtos.UpdateTenantRequest;
import jakarta.validation.Valid;

/**
 * Tenants of the platform. The class-level {@code @PreAuthorize} applies to all
 * methods (in addition to the /api/platform/** rule in SecurityConfig).
 */
@RestController
@ConditionalOnProperty(name = "app.mode", havingValue = "cloud", matchIfMissing = true)
@RequestMapping("/api/platform/tenants")
@PreAuthorize("hasRole('SUPERADMIN')")
public class PlatformTenantController {

    private final PlatformTenantService service;

    PlatformTenantController(PlatformTenantService service) {
        this.service = service;
    }

    @GetMapping
    List<TenantDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TenantDto create(@Valid @RequestBody NewTenantRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    TenantDto update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
