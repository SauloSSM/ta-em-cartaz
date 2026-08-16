package br.com.elitedevticket.payments.application;

import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import java.util.Objects;
import java.util.UUID;

public record ProcessPaymentAttemptCommand(
        UUID customerId,
        UUID reservationId,
        UUID paymentAttemptId,
        PaymentSimulatedOutcome simulatedOutcome
) {
    public ProcessPaymentAttemptCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(paymentAttemptId, "paymentAttemptId must not be null");
        Objects.requireNonNull(simulatedOutcome, "simulatedOutcome must not be null");
    }
}
