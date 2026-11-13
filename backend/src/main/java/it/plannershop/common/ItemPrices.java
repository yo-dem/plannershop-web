package it.plannershop.common;

import java.math.BigDecimal;

import it.plannershop.common.Prices.NetGross;
import jakarta.persistence.Embeddable;

/**
 * VAT rate and prices of a product or service (columns vat_rate, net_price,
 * gross_price, sale_price). See SPECIFICA §7.1.
 */
@Embeddable
public class ItemPrices {

    private BigDecimal vatRate;
    private BigDecimal netPrice;
    private BigDecimal grossPrice;
    private BigDecimal salePrice;

    protected ItemPrices() {
    }

    private ItemPrices(BigDecimal vatRate, BigDecimal netPrice, BigDecimal grossPrice, BigDecimal salePrice) {
        this.vatRate = vatRate;
        this.netPrice = netPrice;
        this.grossPrice = grossPrice;
        this.salePrice = salePrice;
    }

    /** From the request: the price the user did not enter is recalculated here (the backend is authoritative). */
    public static ItemPrices from(PriceRequest req) {
        NetGross p = Prices.compute(req.priceBase(), req.netPrice(), req.grossPrice(), req.vatRate());
        return new ItemPrices(req.vatRate(), p.net(), p.gross(), req.salePrice());
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getNetPrice() {
        return netPrice;
    }

    public BigDecimal getGrossPrice() {
        return grossPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }
}
