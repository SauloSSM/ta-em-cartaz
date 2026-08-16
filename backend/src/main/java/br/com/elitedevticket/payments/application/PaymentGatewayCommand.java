package br.com.elitedevticket.payments.application;

import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentGatewayCommand(
        UUID paymentAttemptId,
        UUID reservationId,
        BigDecimal amount,
        PaymentSimulatedOutcome simulatedOutcome
) {
    public PaymentGatewayCommand {
        Objects.requireNonNull(paymentAttemptId, "paymentAttemptId must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(simulatedOutcome, "simulatedOutcome must not be null");
    }
}
