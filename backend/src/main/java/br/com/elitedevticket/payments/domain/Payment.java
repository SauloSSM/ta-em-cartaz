package br.com.elitedevticket.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID reservationId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String declineReason,
        String fingerprint,
        Instant createdAt,
        Instant processedAt
) {
    public static final String DEFAULT_CURRENCY = "BRL";
    public static final String DEFAULT_PROVIDER = "FAKE";

    public Payment {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be greater than or equal to zero");
        }
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
    }
}
