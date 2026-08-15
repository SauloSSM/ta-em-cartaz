package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.domain.TicketSector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_sectors")
public class TicketSectorEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketSectorEntity() {
    }

    public TicketSectorEntity(TicketSector domain) {
        this.id = domain.id();
        this.eventId = domain.eventId();
        this.name = domain.name();
        this.description = domain.description();
        this.capacity = domain.capacity();
        this.availableQuantity = domain.availableQuantity();
        this.price = domain.price();
        this.createdAt = domain.createdAt();
        this.updatedAt = domain.updatedAt();
    }

    public TicketSector toDomain() {
        return new TicketSector(
                id,
                eventId,
                name,
                description,
                capacity,
                availableQuantity,
                price,
                createdAt,
                updatedAt
        );
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public int capacity() {
        return capacity;
    }

    public int availableQuantity() {
        return availableQuantity;
    }

    public BigDecimal price() {
        return price;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
