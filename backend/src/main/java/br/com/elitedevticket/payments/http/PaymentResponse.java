package br.com.elitedevticket.payments.http;

import br.com.elitedevticket.payments.domain.Payment;
import br.com.elitedevticket.payments.domain.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        UUID id,
        UUID reservationId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String declineReason,
        Instant createdAt,
        Instant processedAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.reservationId(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.provider(),
                payment.declineReason(),
                payment.createdAt(),
                payment.processedAt()
        );
    }
}
