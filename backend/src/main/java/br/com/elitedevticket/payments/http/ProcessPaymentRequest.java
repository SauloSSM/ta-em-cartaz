package br.com.elitedevticket.payments.http;

import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import java.util.UUID;

public record ProcessPaymentRequest(
        UUID paymentAttemptId,
        PaymentSimulatedOutcome simulatedOutcome
) {
    public void validate() {
        if (paymentAttemptId == null) {
            throw new IllegalArgumentException("paymentAttemptId é obrigatório.");
        }
        if (simulatedOutcome == null) {
            throw new IllegalArgumentException("simulatedOutcome é obrigatório.");
        }
    }
}
