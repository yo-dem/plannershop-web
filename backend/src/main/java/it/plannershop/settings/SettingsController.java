package it.plannershop.settings;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.plannershop.settings.SettingsDtos.SettingsDto;
import it.plannershop.settings.SettingsDtos.SettingsRequest;
import jakarta.validation.Valid;

/**
 * Settings of the business: the operator reads them (e.g. stock thresholds for
 * the products list), only the ADMIN changes them.
 */
@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class SettingsController {

    private final SettingsService service;

    SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    SettingsDto get() {
        return service.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    SettingsDto update(@Valid @RequestBody SettingsRequest body) {
        return service.update(body);
    }
}
