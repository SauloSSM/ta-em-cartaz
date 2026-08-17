package br.com.elitedevticket.gate.application;

import br.com.elitedevticket.gate.domain.ValidationMethod;
import java.util.Objects;
import java.util.UUID;

public record ValidateTicketCommand(
        UUID validationAttemptId,
        UUID gateUserId,
        UUID selectedEventId,
        ValidationMethod method,
        String manualCode
) {
    public ValidateTicketCommand {
        Objects.requireNonNull(validationAttemptId, "validationAttemptId must not be null");
        Objects.requireNonNull(gateUserId, "gateUserId must not be null");
        Objects.requireNonNull(selectedEventId, "selectedEventId must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}
