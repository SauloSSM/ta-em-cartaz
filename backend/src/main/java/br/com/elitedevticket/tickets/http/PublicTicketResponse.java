package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record PublicTicketResponse(
        UUID id,
        UUID eventId,
        UUID sectorId,
        int ordinal,
        TicketStatus status,
        String manualCode,
        String shareToken,
        String validationToken,
        Instant createdAt
) {}
