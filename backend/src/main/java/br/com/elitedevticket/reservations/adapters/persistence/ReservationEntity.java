package br.com.elitedevticket.reservations.adapters.persistence;

import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "sector_id", nullable = false)
    private UUID sectorId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected ReservationEntity() {
    }

    public ReservationEntity(Reservation domain) {
        this.id = domain.id();
        this.customerId = domain.customerId();
        this.eventId = domain.eventId();
        this.sectorId = domain.sectorId();
        this.quantity = domain.quantity();
        this.unitPrice = domain.unitPrice();
        this.totalAmount = domain.totalAmount();
        this.status = domain.status();
        this.expiresAt = domain.expiresAt();
        this.createdAt = domain.createdAt();
        this.confirmedAt = domain.confirmedAt();
    }

    public Reservation toDomain() {
        return new Reservation(
                id,
                customerId,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                totalAmount,
                status,
                expiresAt,
                createdAt,
                confirmedAt
        );
    }
}
