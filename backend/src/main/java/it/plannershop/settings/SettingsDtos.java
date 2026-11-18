package it.plannershop.settings;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    /**
     * @param version null while the tenant has never saved its settings (defaults in use)
     */
    public record SettingsDto(int outOfStockThreshold, int lowStockThreshold, Long version) {

        static SettingsDto of(TenantSettings s) {
            return new SettingsDto(s.getOutOfStockThreshold(), s.getLowStockThreshold(), s.getVersion());
        }

        static SettingsDto defaults() {
            return new SettingsDto(TenantSettings.DEFAULT_OUT_OF_STOCK_THRESHOLD,
                    TenantSettings.DEFAULT_LOW_STOCK_THRESHOLD, null);
        }
    }

    /**
     * @param version the one read with GET; ignored on the first save
     */
    public record SettingsRequest(
            @NotNull(message = "{validation.required}") @Min(value = 0, message = "{validation.notNegative}")
            @Max(value = 1_000_000, message = "{validation.quantityTooLarge}") Integer outOfStockThreshold,
            @NotNull(message = "{validation.required}") @Min(value = 0, message = "{validation.notNegative}")
            @Max(value = 1_000_000, message = "{validation.quantityTooLarge}") Integer lowStockThreshold,
            Long version) {

        /** The orange threshold cannot be below the red one (error on the field "lowStockThreshold"). */
        @AssertTrue(message = "{validation.lowStockThreshold}")
        boolean isLowStockThreshold() {
            return outOfStockThreshold == null || lowStockThreshold == null || lowStockThreshold >= outOfStockThreshold;
        }
    }
}
