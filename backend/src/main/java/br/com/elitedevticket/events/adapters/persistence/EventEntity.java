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

    @Column(name = "external_source", length = 64)
    private String externalSource;

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

    @Column(name = "venue_name", length = 255)
    private String venueName;

    @Column(name = "venue_address", length = 500)
    private String venueAddress;

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
        this.externalSource = event.externalSource();
        this.externalId = event.externalId();
        this.title = event.title();
        this.description = event.description();
        this.imageUrl = event.imageUrl();
        this.category = event.category();
        this.status = event.status();
        this.venueName = event.venueName();
        this.venueAddress = event.venueAddress();
        this.startsAt = event.startsAt();
        this.createdAt = event.createdAt();
        this.updatedAt = event.updatedAt();
    }

    public Event toDomain() {
        return new Event(
                id,
                organizerId,
                externalSource,
                externalId,
                title,
                description,
                imageUrl,
                category,
                status,
                venueName,
                venueAddress,
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

    public String externalSource() {
        return externalSource;
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

    public String venueName() {
        return venueName;
    }

    public String venueAddress() {
        return venueAddress;
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
