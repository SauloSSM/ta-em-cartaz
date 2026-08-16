package br.com.elitedevticket.tickets.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IssueTicketsCommand(
        UUID reservationId,
        UUID eventId,
        UUID sectorId,
        UUID customerId,
        int quantity,
        Instant serverNow
) {
    public IssueTicketsCommand {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(sectorId, "sectorId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        if (quantity < 1 || quantity > 6) {
            throw new IllegalArgumentException("quantity must be between 1 and 6");
        }
        Objects.requireNonNull(serverNow, "serverNow must not be null");
    }
}
