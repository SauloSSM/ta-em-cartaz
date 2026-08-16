package br.com.elitedevticket.payments.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.payments.application.ProcessPaymentAttemptCommand;
import br.com.elitedevticket.payments.application.ProcessPaymentAttemptUseCase;
import br.com.elitedevticket.payments.domain.Payment;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class PaymentsController {

    private final ProcessPaymentAttemptUseCase processPaymentAttemptUseCase;

    public PaymentsController(ProcessPaymentAttemptUseCase processPaymentAttemptUseCase) {
        this.processPaymentAttemptUseCase = Objects.requireNonNull(processPaymentAttemptUseCase, "processPaymentAttemptUseCase must not be null");
    }

    @PostMapping("/{reservationId}/payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody(required = false) ProcessPaymentRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Corpo da requisição de pagamento é obrigatório.");
        }
        request.validate();

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                sessionUser.id(),
                reservationId,
                request.paymentAttemptId(),
                request.simulatedOutcome()
        );

        Payment payment = processPaymentAttemptUseCase.execute(command);
        return ResponseEntity.ok(PaymentResponse.fromDomain(payment));
    }
}
