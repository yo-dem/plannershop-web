package it.plannershop.common;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;

/**
 * Prices sent by the frontend for a product or a service. The user enters the
 * net or the gross price ({@link #priceBase()}): the other one is recalculated.
 * Constraints on the single fields are on the records implementing this interface.
 */
public interface PriceRequest {

    BigDecimal vatRate();

    PriceBase priceBase();

    BigDecimal netPrice();

    BigDecimal grossPrice();

    BigDecimal salePrice();

    /** The price the other one is calculated from must be present (error on the "price" field). */
    @AssertTrue(message = "{validation.price}")
    default boolean isPrice() {
        if (priceBase() == null) {
            return true; // reported by @NotNull on priceBase
        }
        return switch (priceBase()) {
            case NET -> netPrice() != null;
            case GROSS -> grossPrice() != null;
        };
    }
}
