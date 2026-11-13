package it.plannershop.product;

import static it.plannershop.common.TextUtils.clean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import it.plannershop.common.PriceBase;
import it.plannershop.common.PriceRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductDto(
            Long id,
            LocalDate stockDate,
            String brand,
            String description,
            int quantity,
            BigDecimal vatRate,
            BigDecimal netPrice,
            BigDecimal grossPrice,
            BigDecimal salePrice,
            String notes,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static ProductDto of(Product p) {
            var pr = p.getPrices();
            return new ProductDto(p.getId(), p.getStockDate(), p.getBrand(), p.getDescription(), p.getQuantity(),
                    pr.getVatRate(), pr.getNetPrice(), pr.getGrossPrice(), pr.getSalePrice(), p.getNotes(),
                    p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    /**
     * Data of a product to create or update.
     *
     * @param stockDate on create, if missing: today
     * @param priceBase which price the user entered (the other one is calculated)
     * @param version   required on update: the version that was read
     */
    public record ProductRequest(
            LocalDate stockDate,
            @NotBlank(message = "{validation.required}") @Size(max = 100, message = "{validation.maxLength}") String brand,
            @NotBlank(message = "{validation.required}") @Size(max = 200, message = "{validation.maxLength}") String description,
            @NotNull(message = "{validation.required}") @Min(value = 0, message = "{validation.notNegative}")
            @Max(value = 1_000_000, message = "{validation.quantityTooLarge}") Integer quantity,
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

        public ProductRequest {
            brand = clean(brand);
            description = clean(description);
            notes = clean(notes);
        }
    }
}
