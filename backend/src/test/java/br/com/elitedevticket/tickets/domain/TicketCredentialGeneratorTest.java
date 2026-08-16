package br.com.elitedevticket.tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class TicketCredentialGeneratorTest {

    private final TicketCredentialGenerator generator = new TicketCredentialGenerator();

    @Test
    @DisplayName("Gera validationToken e shareToken distintos, com alta entropia (64 hex) e 100% de separação em 1000 iterações")
    void shouldGenerateHighEntropyTokensDistinctly() {
        Set<String> validationTokens = new HashSet<>();
        Set<String> shareTokens = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            String validationToken = generator.generateValidationToken();
            String shareToken = generator.generateShareToken();

            assertThat(validationToken).isNotNull().hasSize(64).matches("^[a-f0-9]{64}$");
            assertThat(shareToken).isNotNull().hasSize(64).matches("^[a-f0-9]{64}$");

            // validationToken e shareToken são estritamente diferentes
            assertThat(validationToken).isNotEqualTo(shareToken);

            validationTokens.add(validationToken);
            shareTokens.add(shareToken);
        }

        assertThat(validationTokens).hasSize(1000);
        assertThat(shareTokens).hasSize(1000);
        // Nenhum token de validação coincide com qualquer token de compartilhamento
        assertThat(validationTokens).doesNotContainAnyElementsOf(shareTokens);
    }

    @Test
    @DisplayName("Gera manualCode com 10 caracteres pertencentes exclusivamente ao alfabeto Crockford Base32 sem I, L, O, U")
    void shouldGenerateValidCrockfordManualCode() {
        Set<String> generatedCodes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generateManualCode();
            assertThat(code).hasSize(10);
            for (char c : code.toCharArray()) {
                assertThat(TicketCredentialGenerator.CROCKFORD_BASE32_ALPHABET).contains(String.valueOf(c));
            }
            // Letras I, L, O, U são proibidas no alfabeto Crockford do sistema
            assertThat(code).doesNotContain("I", "L", "O", "U");
            generatedCodes.add(code);
        }
        assertThat(generatedCodes).hasSize(1000);
    }

    @ParameterizedTest
    @CsvSource({
        "ab-7k-92-ox-lm, AB7K920X1M",
        "AB-7K-92-OX-LM, AB7K920X1M",
        "ab7k92oxlm, AB7K920X1M",
        "  ab- 7k. 92/ox_lm  , AB7K920X1M",
        "ii-ll-oo-11-00, 1111001100",
        "II-LL-OO-11-00, 1111001100",
        "AB7K92QX4M, AB7K92QX4M"
    })
    @DisplayName("Normaliza código manual convertendo minúsculas, removendo separadores e substituindo caracteres ambíguos I, L, O por 1, 1, 0")
    void shouldNormalizeManualCodeCorrectly(String raw, String expectedNormalized) {
        String normalized = TicketCredentialGenerator.normalizeManualCode(raw);
        assertThat(normalized).isEqualTo(expectedNormalized);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Normalização lida com entradas nulas ou vazias de forma segura")
    void shouldHandleNullOrEmptyInNormalization(String input) {
        String normalized = TicketCredentialGenerator.normalizeManualCode(input);
        if (input == null) {
            assertThat(normalized).isNull();
        } else {
            assertThat(normalized).isEmpty();
        }
    }

    @Test
    @DisplayName("Formata código manual agrupado no padrão 4-4-2 (XXXX-XXXX-XX) para exibição ao usuário")
    void shouldFormatGroupedManualCode() {
        String raw = "AB7K92QX4M";
        String formatted = TicketCredentialGenerator.formatGrouped(raw);
        assertThat(formatted).isEqualTo("AB7K-92QX-4M");

        // Formatando a partir de entrada não normalizada
        String unnormalized = "ab-7k-92-ox-lm";
        String formattedFromRaw = TicketCredentialGenerator.formatGrouped(unnormalized);
        assertThat(formattedFromRaw).isEqualTo("AB7K-920X-1M");
    }

    @Test
    @DisplayName("formatGrouped lida com entradas inválidas, incompletas ou nulas")
    void shouldHandleInvalidLengthInFormatGrouped() {
        assertThat(TicketCredentialGenerator.formatGrouped(null)).isNull();
        assertThat(TicketCredentialGenerator.formatGrouped("SHORT")).isEqualTo("SHORT");
        assertThat(TicketCredentialGenerator.formatGrouped("AB-CD")).isEqualTo("AB-CD");
    }

    @Test
    @DisplayName("Invariante matemática: dois códigos Crockford válidos gerados nunca resolvem para o mesmo valor normalizado")
    void distinctGeneratedCrockfordCodesNeverNormalizeToSameValue() {
        Set<String> normalizedSet = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generateManualCode();
            String normalized = TicketCredentialGenerator.normalizeManualCode(code);
            // Códigos gerados já são a forma canônica normalizada
            assertThat(normalized).isEqualTo(code);
            normalizedSet.add(normalized);
        }
        assertThat(normalizedSet).hasSize(1000);
    }
}
