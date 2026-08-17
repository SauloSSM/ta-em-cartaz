package br.com.elitedevticket.gate.adapters.persistence;

import br.com.elitedevticket.gate.domain.GateValidationResult;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import br.com.elitedevticket.gate.domain.ValidationMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_attempts")
class ValidationAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "gate_user_id", nullable = false)
    private UUID gateUserId;

    @Column(name = "selected_event_id", nullable = false)
    private UUID selectedEventId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_method", nullable = false, length = 16)
    private ValidationMethod validationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 32)
    private GateValidationResult result;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ValidationAttemptEntity() {
    }

    ValidationAttemptEntity(ValidationAttempt domain) {
        this.id = domain.id();
        this.gateUserId = domain.gateUserId();
        this.selectedEventId = domain.selectedEventId();
        this.ticketId = domain.ticketId();
        this.validationMethod = domain.validationMethod();
        this.result = domain.result();
        this.fingerprint = domain.fingerprint();
        this.processedAt = domain.processedAt();
        this.createdAt = domain.createdAt();
    }

    ValidationAttempt toDomain() {
        return new ValidationAttempt(
                id,
                gateUserId,
                selectedEventId,
                ticketId,
                validationMethod,
                result,
                fingerprint,
                processedAt,
                createdAt
        );
    }
}
