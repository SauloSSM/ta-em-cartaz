package br.com.elitedevticket.tickets.adapters.persistence;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "sector_id", nullable = false)
    private UUID sectorId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TicketStatus status;

    @Column(name = "validation_token", nullable = false, unique = true, length = 128)
    private String validationToken;

    @Column(name = "manual_code", nullable = false, unique = true, length = 32)
    private String manualCode;

    @Column(name = "share_token", nullable = false, unique = true, length = 128)
    private String shareToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by_gate_user_id")
    private UUID usedByGateUserId;

    protected TicketEntity() {
    }

    public TicketEntity(Ticket domain) {
        this.id = domain.id();
        this.reservationId = domain.reservationId();
        this.eventId = domain.eventId();
        this.sectorId = domain.sectorId();
        this.customerId = domain.customerId();
        this.ordinal = domain.ordinal();
        this.status = domain.status();
        this.validationToken = domain.validationToken();
        this.manualCode = domain.manualCode();
        this.shareToken = domain.shareToken();
        this.createdAt = domain.createdAt();
        this.usedAt = domain.usedAt();
        this.usedByGateUserId = domain.usedByGateUserId();
    }

    public Ticket toDomain() {
        return new Ticket(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                ordinal,
                status,
                validationToken,
                manualCode,
                shareToken,
                createdAt,
                usedAt,
                usedByGateUserId
        );
    }
}
