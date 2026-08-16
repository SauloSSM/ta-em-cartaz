package br.com.elitedevticket.tickets.domain;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TicketCredentialGenerator {

    public static final String CROCKFORD_BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    public static final int MANUAL_CODE_LENGTH = 10;
    public static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom;

    public TicketCredentialGenerator() {
        this.secureRandom = new SecureRandom();
    }

    public TicketCredentialGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    public String generateValidationToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String generateShareToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String generateManualCode() {
        char[] chars = new char[MANUAL_CODE_LENGTH];
        for (int i = 0; i < MANUAL_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CROCKFORD_BASE32_ALPHABET.length());
            chars[i] = CROCKFORD_BASE32_ALPHABET.charAt(index);
        }
        return new String(chars);
    }

    public static String normalizeManualCode(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = input.toUpperCase().replaceAll("[^0-9A-Z]", "");
        cleaned = cleaned.replace('I', '1').replace('L', '1').replace('O', '0');
        return cleaned;
    }

    public static String formatGrouped(String manualCode) {
        if (manualCode == null) {
            return null;
        }
        String normalized = normalizeManualCode(manualCode);
        if (normalized.length() != MANUAL_CODE_LENGTH) {
            return manualCode;
        }
        return normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8);
    }
}
