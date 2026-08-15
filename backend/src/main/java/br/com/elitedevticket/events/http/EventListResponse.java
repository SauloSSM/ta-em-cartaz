package br.com.elitedevticket.events.http;

import java.util.List;
import java.util.Objects;

public record EventListResponse(
        List<EventResponse> events
) {
    public EventListResponse {
        Objects.requireNonNull(events, "events must not be null");
        events = List.copyOf(events);
    }
}
