package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record MyTicketResponse(
        UUID id,
        UUID reservationId,
        UUID eventId,
        UUID sectorId,
        int ordinal,
        TicketStatus status,
        String manualCode,
        String shareToken,
        String validationToken,
        Instant createdAt
) {
}
