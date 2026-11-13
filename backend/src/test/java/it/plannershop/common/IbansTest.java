package it.plannershop.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IbansTest {

    @Test
    void normalizesSpacesAndCase() {
        assertThat(Ibans.normalize(" it60 x054 2811 1010 0000 0123 456 ")).isEqualTo("IT60X0542811101000000123456");
        assertThat(Ibans.normalize("   ")).isNull();
    }

    @Test
    void checksFormatAndCheckDigits() {
        assertThat(Ibans.isValid("IT60X0542811101000000123456")).isTrue();
        assertThat(Ibans.isValid("DE89370400440532013000")).isTrue();
        assertThat(Ibans.isValid("GB82WEST12345698765432")).isTrue();
        assertThat(Ibans.isValid("IT61X0542811101000000123456")).isFalse(); // wrong check digits
        assertThat(Ibans.isValid("IT60X05428")).isFalse(); // too short
        assertThat(Ibans.isValid("1T60X0542811101000000123456")).isFalse();
    }
}
