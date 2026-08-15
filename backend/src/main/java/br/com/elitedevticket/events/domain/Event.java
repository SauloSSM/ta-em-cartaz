package br.com.elitedevticket.events.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Event(
        UUID id,
        UUID organizerId,
        String externalSource,
        String externalId,
        String title,
        String description,
        String imageUrl,
        String category,
        EventStatus status,
        String venueName,
        String venueAddress,
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

    public Event withUpdatedDraftDetails(
            String title,
            String description,
            String imageUrl,
            String category,
            String venueName,
            String venueAddress,
            Instant startsAt,
            Instant updatedAt) {
        if (this.status != EventStatus.DRAFT) {
            throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Apenas eventos em rascunho podem ser modificados.");
        }
        return new Event(
                this.id,
                this.organizerId,
                this.externalSource,
                this.externalId,
                title,
                description,
                imageUrl,
                category,
                this.status,
                venueName,
                venueAddress,
                startsAt,
                this.createdAt,
                updatedAt
        );
    }

    public Event publish(Instant publishedAt, List<TicketSector> sectors, Clock clock) {
        if (this.status != EventStatus.DRAFT) {
            throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Apenas eventos em rascunho podem ser publicados.");
        }
        if (this.title == null || this.title.isBlank()) {
            throw new IllegalArgumentException("Título do evento é obrigatório para publicação.");
        }
        if (this.externalSource == null || this.externalSource.isBlank()) {
            throw new IllegalArgumentException("Origem da referência externa do evento é obrigatória para publicação.");
        }
        if (this.externalId == null || this.externalId.isBlank()) {
            throw new IllegalArgumentException("Referência externa do evento é obrigatória para publicação.");
        }
        if (this.venueName == null || this.venueName.isBlank()) {
            throw new IllegalArgumentException("Nome do local do evento é obrigatório para publicação.");
        }
        if (this.venueAddress == null || this.venueAddress.isBlank()) {
            throw new IllegalArgumentException("Endereço do local do evento é obrigatório para publicação.");
        }
        if (this.startsAt == null) {
            throw new IllegalArgumentException("Data e hora de início são obrigatórias para publicação.");
        }
        Instant now = Instant.now(clock);
        if (!this.startsAt.isAfter(now)) {
            throw new IllegalArgumentException("A data de início do evento deve ser futura.");
        }
        if (sectors == null || sectors.isEmpty()) {
            throw new IllegalArgumentException("O evento deve possuir pelo menos um setor de ingressos configurado para ser publicado.");
        }
        for (TicketSector sector : sectors) {
            if (sector.name() == null || sector.name().isBlank()) {
                throw new IllegalArgumentException("Nome do setor é obrigatório.");
            }
            if (sector.capacity() <= 0) {
                throw new IllegalArgumentException("Capacidade do setor deve ser maior que zero.");
            }
            if (sector.price() == null || sector.price().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Preço do setor deve ser maior ou igual a zero.");
            }
        }
        return new Event(
                this.id,
                this.organizerId,
                this.externalSource,
                this.externalId,
                this.title,
                this.description,
                this.imageUrl,
                this.category,
                EventStatus.PUBLISHED,
                this.venueName,
                this.venueAddress,
                this.startsAt,
                this.createdAt,
                publishedAt
        );
    }

    public void assertCanBeDeleted() {
        if (this.status != EventStatus.DRAFT) {
            throw new EventConflictException("EVENT_CANNOT_BE_DELETED", "Eventos publicados não podem ser excluídos.");
        }
    }
}
