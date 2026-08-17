package br.com.elitedevticket.gate.http;

import br.com.elitedevticket.gate.domain.GateValidationResult;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import br.com.elitedevticket.gate.domain.ValidationMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

public record ValidateTicketResponse(
        GateValidationResult result,
        UUID validationAttemptId,
        UUID selectedEventId,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID ticketId,
        ValidationMethod method,
        Instant processedAt
) {
    public static ValidateTicketResponse fromDomain(ValidationAttempt attempt) {
        return new ValidateTicketResponse(
                attempt.result(),
                attempt.id(),
                attempt.selectedEventId(),
                attempt.ticketId(),
                attempt.validationMethod(),
                attempt.processedAt()
        );
    }
}
