package br.com.elitedevticket.gate.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ValidationAttempt(
        UUID id,
        UUID gateUserId,
        UUID selectedEventId,
        UUID ticketId,
        ValidationMethod validationMethod,
        GateValidationResult result,
        String fingerprint,
        Instant processedAt,
        Instant createdAt
) {
    public ValidationAttempt {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(gateUserId, "gateUserId must not be null");
        Objects.requireNonNull(selectedEventId, "selectedEventId must not be null");
        Objects.requireNonNull(validationMethod, "validationMethod must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
