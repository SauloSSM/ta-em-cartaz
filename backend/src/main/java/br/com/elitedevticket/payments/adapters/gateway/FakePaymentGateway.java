package br.com.elitedevticket.payments.adapters.gateway;

import br.com.elitedevticket.payments.application.PaymentGateway;
import br.com.elitedevticket.payments.application.PaymentGatewayCommand;
import br.com.elitedevticket.payments.application.PaymentGatewayResult;
import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import br.com.elitedevticket.payments.domain.PaymentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private final Clock clock;

    public FakePaymentGateway(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PaymentGatewayResult process(PaymentGatewayCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Instant processedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);

        if (command.simulatedOutcome() == PaymentSimulatedOutcome.DECLINED) {
            return new PaymentGatewayResult(
                    PaymentStatus.DECLINED,
                    "SIMULATED_DECLINE",
                    processedAt
            );
        }

        if (command.simulatedOutcome() == PaymentSimulatedOutcome.APPROVED) {
            return new PaymentGatewayResult(
                    PaymentStatus.APPROVED,
                    null,
                    processedAt
            );
        }

        throw new IllegalArgumentException("Unsupported simulated outcome: " + command.simulatedOutcome());
    }
}
