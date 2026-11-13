package it.plannershop.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.plannershop.common.Prices.NetGross;

class PricesTest {

    @ParameterizedTest(name = "net {0} + VAT {1}% = {2}")
    @CsvSource({
            "10,    22, 12.20", // exact: the old desktop app (double) gave 12.21
            "10.01, 22, 12.22", // 12.2122 → rounded up
            "100,   10, 110.00",
            "9.99,  4,  10.39", // 10.3896
            "50,    0,  50.00",
            "0,     22, 0.00",
            "10,    22.5, 12.25",
    })
    void grossFromNet(String net, String vatRate, String expected) {
        assertThat(Prices.gross(new BigDecimal(net), new BigDecimal(vatRate))).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest(name = "gross {0} with VAT {1}% → net {2}")
    @CsvSource({
            "18,    22, 14.76", // 14.7540...
            "12.20, 22, 10.00",
            "110,   10, 100.00",
            "50,    0,  50.00",
    })
    void netFromGross(String gross, String vatRate, String expected) {
        assertThat(Prices.net(new BigDecimal(gross), new BigDecimal(vatRate))).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void computeStartsFromTheEnteredPriceAndIgnoresTheOther() {
        BigDecimal vatRate = new BigDecimal("22");
        assertThat(Prices.compute(PriceBase.NET, new BigDecimal("10"), new BigDecimal("999"), vatRate))
                .isEqualTo(new NetGross(new BigDecimal("10.00"), new BigDecimal("12.20")));
        assertThat(Prices.compute(PriceBase.GROSS, null, new BigDecimal("18"), vatRate))
                .isEqualTo(new NetGross(new BigDecimal("14.76"), new BigDecimal("18.00")));
    }
}
