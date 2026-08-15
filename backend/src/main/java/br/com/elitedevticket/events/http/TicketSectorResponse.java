package br.com.elitedevticket.events.http;

import br.com.elitedevticket.events.domain.TicketSector;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketSectorResponse(
        UUID id,
        UUID eventId,
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        int capacity,
        int availableQuantity,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketSectorResponse fromDomain(TicketSector sector) {
        return new TicketSectorResponse(
                sector.id(),
                sector.eventId(),
                sector.name(),
                sector.description(),
                sector.capacity(),
                sector.availableQuantity(),
                sector.price(),
                sector.createdAt(),
                sector.updatedAt()
        );
    }
}
