package br.com.elitedevticket.events.http;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PublicEventResponse(
        UUID id,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String category,
        EventStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) String venueName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String venueAddress,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant startsAt,
        BigDecimal startingPrice,
        boolean salesClosed,
        Instant createdAt,
        Instant updatedAt
) {
    public static PublicEventResponse of(Event event, BigDecimal startingPrice, boolean salesClosed) {
        return new PublicEventResponse(
                event.id(),
                event.title(),
                event.description(),
                event.imageUrl(),
                event.category(),
                event.status(),
                event.venueName(),
                event.venueAddress(),
                event.startsAt(),
                startingPrice,
                salesClosed,
                event.createdAt(),
                event.updatedAt()
        );
    }
}
