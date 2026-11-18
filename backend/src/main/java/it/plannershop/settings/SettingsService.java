package it.plannershop.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.ApiException;
import it.plannershop.settings.SettingsDtos.SettingsDto;
import it.plannershop.settings.SettingsDtos.SettingsRequest;

/** Settings of the current tenant. Every business has its own. */
@Service
public class SettingsService {

    private final TenantSettingsRepository settings;

    SettingsService(TenantSettingsRepository settings) {
        this.settings = settings;
    }

    /** The saved settings, or the defaults if the tenant has never saved them. */
    @Transactional(readOnly = true)
    public SettingsDto get() {
        return settings.findFirstBy().map(SettingsDto::of).orElseGet(SettingsDto::defaults);
    }

    /** The first save creates the row; later ones need the version read before (409 if changed meanwhile). */
    @Transactional
    public SettingsDto update(SettingsRequest req) {
        TenantSettings s = settings.findFirstBy().orElse(null);
        if (s == null) {
            s = new TenantSettings();
        } else if (req.version() == null) {
            throw ApiException.badRequest("error.versionMissing");
        } else if (req.version() != s.getVersion()) {
            throw ApiException.conflict("error.conflict");
        }
        s.setStockThresholds(req.outOfStockThreshold(), req.lowStockThreshold());
        return SettingsDto.of(settings.saveAndFlush(s));
    }
}
