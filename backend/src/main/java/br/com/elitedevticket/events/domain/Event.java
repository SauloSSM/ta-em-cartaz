package br.com.elitedevticket.events.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Event(
        UUID id,
        UUID organizerId,
        String externalId,
        String title,
        String description,
        String imageUrl,
        String category,
        EventStatus status,
        String venue,
        Instant startsAt,
        Instant createdAt,
        Instant updatedAt
) {
    public Event {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public boolean isOwnedBy(UUID userId) {
        return this.organizerId.equals(userId);
    }
}
