package it.plannershop.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.plannershop.common.Prezzi.CalcolaDa;
import it.plannershop.common.Prezzi.NettoIvato;

class PrezziTest {

    @ParameterizedTest(name = "netto {0} + IVA {1}% = {2}")
    @CsvSource({
            "10,    22, 12.20", // esatto: il vecchio programma (double) dava 12,21
            "10.01, 22, 12.22", // 12,2122 → per eccesso
            "100,   10, 110.00",
            "9.99,  4,  10.39", // 10,3896
            "50,    0,  50.00",
            "0,     22, 0.00",
            "10,    22.5, 12.25",
    })
    void ivatoDalNetto(String netto, String aliquota, String atteso) {
        assertThat(Prezzi.ivato(new BigDecimal(netto), new BigDecimal(aliquota))).isEqualTo(new BigDecimal(atteso));
    }

    @ParameterizedTest(name = "ivato {0} con IVA {1}% → netto {2}")
    @CsvSource({
            "18,    22, 14.76", // 14,7540...
            "12.20, 22, 10.00",
            "110,   10, 100.00",
            "50,    0,  50.00",
    })
    void nettoDallIvato(String ivato, String aliquota, String atteso) {
        assertThat(Prezzi.netto(new BigDecimal(ivato), new BigDecimal(aliquota))).isEqualTo(new BigDecimal(atteso));
    }

    @Test
    void calcolaParteDalPrezzoInseritoEIgnoraLAltro() {
        BigDecimal aliquota = new BigDecimal("22");
        assertThat(Prezzi.calcola(CalcolaDa.NETTO, new BigDecimal("10"), new BigDecimal("999"), aliquota))
                .isEqualTo(new NettoIvato(new BigDecimal("10.00"), new BigDecimal("12.20")));
        assertThat(Prezzi.calcola(CalcolaDa.IVATO, null, new BigDecimal("18"), aliquota))
                .isEqualTo(new NettoIvato(new BigDecimal("14.76"), new BigDecimal("18.00")));
    }
}
