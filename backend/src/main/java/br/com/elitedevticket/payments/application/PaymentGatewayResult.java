package br.com.elitedevticket.payments.application;

import br.com.elitedevticket.payments.domain.PaymentStatus;
import java.time.Instant;
import java.util.Objects;

public record PaymentGatewayResult(
        PaymentStatus status,
        String declineReason,
        Instant processedAt
) {
    public PaymentGatewayResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
    }
}
