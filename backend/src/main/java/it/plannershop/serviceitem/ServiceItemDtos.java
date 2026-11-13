package it.plannershop.serviceitem;

import static it.plannershop.common.TextUtils.clean;

import java.math.BigDecimal;
import java.time.Instant;

import it.plannershop.common.PriceBase;
import it.plannershop.common.PriceRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ServiceItemDtos {

    private ServiceItemDtos() {
    }

    public record ServiceItemDto(
            Long id,
            String name,
            String description,
            BigDecimal vatRate,
            BigDecimal netPrice,
            BigDecimal grossPrice,
            BigDecimal salePrice,
            String notes,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static ServiceItemDto of(ServiceItem s) {
            var pr = s.getPrices();
            return new ServiceItemDto(s.getId(), s.getName(), s.getDescription(), pr.getVatRate(), pr.getNetPrice(),
                    pr.getGrossPrice(), pr.getSalePrice(), s.getNotes(), s.getVersion(), s.getCreatedAt(),
                    s.getUpdatedAt());
        }
    }

    /**
     * Data of a service to create or update.
     *
     * @param priceBase which price the user entered (the other one is calculated)
     * @param version   required on update: the version that was read
     */
    public record ServiceItemRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 100, message = "{validation.maxLength}") String name,
            @NotBlank(message = "{validation.required}") @Size(max = 200, message = "{validation.maxLength}") String description,
            @NotNull(message = "{validation.required}") @DecimalMin(value = "0", message = "{validation.vatRate}")
            @DecimalMax(value = "100", message = "{validation.vatRate}")
            @Digits(integer = 3, fraction = 2, message = "{validation.twoDecimals}") BigDecimal vatRate,
            @NotNull(message = "{validation.priceBase}") PriceBase priceBase,
            @DecimalMin(value = "0", message = "{validation.notNegative}")
            @Digits(integer = 7, fraction = 2, message = "{validation.amount}") BigDecimal netPrice,
            @DecimalMin(value = "0", message = "{validation.notNegative}")
            @Digits(integer = 7, fraction = 2, message = "{validation.amount}") BigDecimal grossPrice,
            @NotNull(message = "{validation.required}") @DecimalMin(value = "0", message = "{validation.notNegative}")
            @Digits(integer = 7, fraction = 2, message = "{validation.amount}") BigDecimal salePrice,
            @Size(max = 4000, message = "{validation.maxLength}") String notes,
            Long version) implements PriceRequest {

        public ServiceItemRequest {
            name = clean(name);
            description = clean(description);
            notes = clean(notes);
        }
    }
}
