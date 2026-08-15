package br.com.elitedevticket.events.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TicketSector(
        UUID id,
        UUID eventId,
        String name,
        String description,
        int capacity,
        int availableQuantity,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
    public TicketSector {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        Objects.requireNonNull(price, "price must not be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to zero");
        }
        if (availableQuantity < 0 || availableQuantity > capacity) {
            throw new IllegalArgumentException("availableQuantity must be between 0 and capacity");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public TicketSector withUpdatedDraftDetails(
            String name,
            String description,
            int capacity,
            BigDecimal price,
            Instant updatedAt
    ) {
        return new TicketSector(
                this.id,
                this.eventId,
                name,
                description,
                capacity,
                capacity,
                price,
                this.createdAt,
                updatedAt
        );
    }
}
