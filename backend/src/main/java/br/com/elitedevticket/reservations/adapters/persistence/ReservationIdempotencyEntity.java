package br.com.elitedevticket.reservations.adapters.persistence;

import br.com.elitedevticket.reservations.domain.ReservationIdempotencyRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation_idempotency_keys")
public class ReservationIdempotencyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReservationIdempotencyEntity() {}

    public ReservationIdempotencyEntity(
            UUID id,
            UUID customerId,
            String idempotencyKey,
            String payloadHash,
            UUID reservationId,
            Instant createdAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.reservationId = reservationId;
        this.createdAt = createdAt;
    }

    public static ReservationIdempotencyEntity fromDomain(ReservationIdempotencyRecord record) {
        return new ReservationIdempotencyEntity(
                record.id(),
                record.customerId(),
                record.idempotencyKey(),
                record.payloadHash(),
                record.reservationId(),
                record.createdAt()
        );
    }

    public ReservationIdempotencyRecord toDomain() {
        return new ReservationIdempotencyRecord(
                this.id,
                this.customerId,
                this.idempotencyKey,
                this.payloadHash,
                this.reservationId,
                this.createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
