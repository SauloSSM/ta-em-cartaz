package br.com.elitedevticket.tickets.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Ticket(
        UUID id,
        UUID reservationId,
        UUID eventId,
        UUID sectorId,
        UUID customerId,
        int ordinal,
        TicketStatus status,
        String validationToken,
        String manualCode,
        String shareToken,
        Instant createdAt,
        Instant usedAt,
        UUID usedByGateUserId
) {
    public static final int MIN_ORDINAL = 1;
    public static final int MAX_ORDINAL = 6;

    public Ticket {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(sectorId, "sectorId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        if (ordinal < MIN_ORDINAL || ordinal > MAX_ORDINAL) {
            throw new IllegalArgumentException("ordinal must be between 1 and 6");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(validationToken, "validationToken must not be null");
        if (validationToken.isBlank()) {
            throw new IllegalArgumentException("validationToken must not be blank");
        }
        Objects.requireNonNull(manualCode, "manualCode must not be null");
        if (manualCode.isBlank()) {
            throw new IllegalArgumentException("manualCode must not be blank");
        }
        Objects.requireNonNull(shareToken, "shareToken must not be null");
        if (shareToken.isBlank()) {
            throw new IllegalArgumentException("shareToken must not be blank");
        }
        if (validationToken.equals(shareToken)) {
            throw new IllegalArgumentException("validationToken and shareToken must not be equal");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Ticket createValid(
            UUID id,
            UUID reservationId,
            UUID eventId,
            UUID sectorId,
            UUID customerId,
            int ordinal,
            String validationToken,
            String manualCode,
            String shareToken,
            Instant createdAt
    ) {
        return new Ticket(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                ordinal,
                TicketStatus.VALID,
                validationToken,
                manualCode,
                shareToken,
                createdAt,
                null,
                null
        );
    }
}
