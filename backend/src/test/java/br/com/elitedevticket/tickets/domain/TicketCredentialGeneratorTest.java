package br.com.elitedevticket.tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketCredentialGeneratorTest {

    private final TicketCredentialGenerator generator = new TicketCredentialGenerator();

    @Test
    @DisplayName("Gera validationToken e shareToken distintos, com alta entropia e 64 caracteres hexadecimais")
    void shouldGenerateHighEntropyTokensDistinctly() {
        String validationToken = generator.generateValidationToken();
        String shareToken = generator.generateShareToken();

        assertThat(validationToken).isNotNull();
        assertThat(validationToken).hasSize(64);
        assertThat(validationToken).matches("^[a-f0-9]{64}$");

        assertThat(shareToken).isNotNull();
        assertThat(shareToken).hasSize(64);
        assertThat(shareToken).matches("^[a-f0-9]{64}$");

        assertThat(validationToken).isNotEqualTo(shareToken);
    }

    @Test
    @DisplayName("Gera manualCode com 10 caracteres pertencentes exclusivamente ao alfabeto Crockford Base32")
    void shouldGenerateValidCrockfordManualCode() {
        Set<String> generatedCodes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String code = generator.generateManualCode();
            assertThat(code).hasSize(10);
            for (char c : code.toCharArray()) {
                assertThat(TicketCredentialGenerator.CROCKFORD_BASE32_ALPHABET).contains(String.valueOf(c));
            }
            // Alphabets I, L, O, U are strictly prohibited
            assertThat(code).doesNotContain("I", "L", "O", "U");
            generatedCodes.add(code);
        }
        assertThat(generatedCodes).hasSize(100);
    }

    @Test
    @DisplayName("Normaliza código manual convertendo minúsculas, removendo separadores e substituindo I, L, O")
    void shouldNormalizeManualCodeCorrectly() {
        String raw = "ab-7k-92-ox-lm";
        String normalized = TicketCredentialGenerator.normalizeManualCode(raw);

        // 'a'->'A', 'b'->'B', '7'->'7', 'k'->'K', '9'->'9', '2'->'2', 'o'->'0', 'x'->'X', 'l'->'1', 'm'->'M'
        assertThat(normalized).isEqualTo("AB7K920X1M");
    }

    @Test
    @DisplayName("Formata código manual agrupado com hífens para exibição")
    void shouldFormatGroupedManualCode() {
        String raw = "AB7K92QX4M";
        String formatted = TicketCredentialGenerator.formatGrouped(raw);

        assertThat(formatted).isEqualTo("AB7K-92QX-4M");
    }
}

