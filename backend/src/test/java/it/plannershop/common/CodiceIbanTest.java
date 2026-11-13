package it.plannershop.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodiceIbanTest {

    @Test
    void normalizzaSpaziEMinuscole() {
        assertThat(CodiceIban.normalizza(" it60 x054 2811 1010 0000 0123 456 ")).isEqualTo("IT60X0542811101000000123456");
        assertThat(CodiceIban.normalizza("   ")).isNull();
    }

    @Test
    void verificaFormatoECifreDiControllo() {
        assertThat(CodiceIban.valido("IT60X0542811101000000123456")).isTrue();
        assertThat(CodiceIban.valido("DE89370400440532013000")).isTrue();
        assertThat(CodiceIban.valido("GB82WEST12345698765432")).isTrue();
        assertThat(CodiceIban.valido("IT61X0542811101000000123456")).isFalse(); // cifre di controllo sbagliate
        assertThat(CodiceIban.valido("IT60X05428")).isFalse(); // troppo corto
        assertThat(CodiceIban.valido("1T60X0542811101000000123456")).isFalse();
    }
}
