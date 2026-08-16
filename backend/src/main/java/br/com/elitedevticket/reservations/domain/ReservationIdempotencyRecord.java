package br.com.elitedevticket.reservations.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReservationIdempotencyRecord(
        UUID id,
        UUID customerId,
        String idempotencyKey,
        String payloadHash,
        UUID reservationId,
        Instant createdAt
) {
    public ReservationIdempotencyRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(payloadHash, "payloadHash must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
