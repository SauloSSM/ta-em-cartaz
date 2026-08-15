package br.com.elitedevticket.events.http;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizerId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalSource,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalId,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String category,
        EventStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) String venueName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String venueAddress,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant startsAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse fromDomain(Event event) {
        return new EventResponse(
                event.id(),
                event.organizerId(),
                event.externalSource(),
                event.externalId(),
                event.title(),
                event.description(),
                event.imageUrl(),
                event.category(),
                event.status(),
                event.venueName(),
                event.venueAddress(),
                event.startsAt(),
                event.createdAt(),
                event.updatedAt()
        );
    }
}
