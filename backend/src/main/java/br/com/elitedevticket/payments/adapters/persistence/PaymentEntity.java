package br.com.elitedevticket.payments.adapters.persistence;

import br.com.elitedevticket.payments.domain.Payment;
import br.com.elitedevticket.payments.domain.PaymentStatus;
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
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(Payment domain) {
        this.id = domain.id();
        this.reservationId = domain.reservationId();
        this.customerId = domain.customerId();
        this.amount = domain.amount();
        this.currency = domain.currency();
        this.status = domain.status();
        this.provider = domain.provider();
        this.declineReason = domain.declineReason();
        this.fingerprint = domain.fingerprint();
        this.createdAt = domain.createdAt();
        this.processedAt = domain.processedAt();
    }

    public Payment toDomain() {
        return new Payment(
                id,
                reservationId,
                customerId,
                amount,
                currency,
                status,
                provider,
                declineReason,
                fingerprint,
                createdAt,
                processedAt
        );
    }
}
