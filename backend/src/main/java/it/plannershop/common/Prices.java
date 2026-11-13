package it.plannershop.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversion between net price and gross price (VAT included) for products and
 * services. See SPECIFICA §7.1: rounded up to the cent, as in the old desktop
 * app, but calculated exactly (BigDecimal, no double).
 */
public final class Prices {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Prices() {
    }

    /** Net and gross price, consistent with the VAT rate. */
    public record NetGross(BigDecimal net, BigDecimal gross) {
    }

    /** {@code gross = ceil(net × (1 + rate/100))} to the cent. */
    public static BigDecimal gross(BigDecimal net, BigDecimal vatRate) {
        return net.multiply(factor(vatRate)).setScale(2, RoundingMode.CEILING);
    }

    /** {@code net = ceil(gross / (1 + rate/100))} to the cent. */
    public static BigDecimal net(BigDecimal gross, BigDecimal vatRate) {
        return gross.divide(factor(vatRate), 2, RoundingMode.CEILING);
    }

    /** Starts from the entered price ({@code base}) and calculates the other one. */
    public static NetGross compute(PriceBase base, BigDecimal net, BigDecimal gross, BigDecimal vatRate) {
        return switch (base) {
            case NET -> new NetGross(net.setScale(2, RoundingMode.CEILING), gross(net, vatRate));
            case GROSS -> new NetGross(net(gross, vatRate), gross.setScale(2, RoundingMode.CEILING));
        };
    }

    private static BigDecimal factor(BigDecimal vatRate) {
        return BigDecimal.ONE.add(vatRate.divide(HUNDRED));
    }
}
