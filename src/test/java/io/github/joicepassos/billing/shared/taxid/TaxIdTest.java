package io.github.joicepassos.billing.shared.taxid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaxIdTest {

    @ParameterizedTest
    @CsvSource({
            "529.982.247-25, CPF, 52998224725",
            "52998224725, CPF, 52998224725",
            "11.222.333/0001-81, CNPJ, 11222333000181",
            "12.ABC.345/01DE-35, CNPJ, 12ABC34501DE35",
            "12.abc.345/01de-35, CNPJ, 12ABC34501DE35"
    })
    void parsesValidDocumentsWithOrWithoutPunctuation(String raw, TaxId.Type type, String normalized) {
        assertThat(TaxId.parse(raw)).hasValue(new TaxId(type, normalized));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "529.982.247-24", "111.111.111-11", "11.222.333/0001-80", "00000000000000",
            "12.ABC.345/01DE-36", "1234567890", "12ABC34501DEAB"})
    void rejectsWrongFormatOrCheckDigits(String raw) {
        assertThat(TaxId.parse(raw)).isEmpty();
    }

    @Test
    void neverPrintsTheFullDocument() {
        TaxId cpf = TaxId.parse("52998224725").orElseThrow();
        TaxId cnpj = TaxId.parse("12ABC34501DE35").orElseThrow();

        assertThat(cpf.masked()).isEqualTo("***.982.247-**");
        assertThat(cnpj.masked()).isEqualTo("**.ABC.345/****-**");
        assertThat(cpf.toString()).doesNotContain("52998224725");
        assertThat(cnpj.toString()).doesNotContain("12ABC34501DE35");
    }
}
