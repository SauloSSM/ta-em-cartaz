package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;

    @Column(length = 255)
    private String venue;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EventEntity() {
    }

    public EventEntity(Event event) {
        this.id = event.id();
        this.organizerId = event.organizerId();
        this.externalId = event.externalId();
        this.title = event.title();
        this.description = event.description();
        this.imageUrl = event.imageUrl();
        this.category = event.category();
        this.status = event.status();
        this.venue = event.venue();
        this.startsAt = event.startsAt();
        this.createdAt = event.createdAt();
        this.updatedAt = event.updatedAt();
    }

    public Event toDomain() {
        return new Event(
                id,
                organizerId,
                externalId,
                title,
                description,
                imageUrl,
                category,
                status,
                venue,
                startsAt,
                createdAt,
                updatedAt
        );
    }

    public UUID id() {
        return id;
    }

    public UUID organizerId() {
        return organizerId;
    }

    public String externalId() {
        return externalId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public String category() {
        return category;
    }

    public EventStatus status() {
        return status;
    }

    public String venue() {
        return venue;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
