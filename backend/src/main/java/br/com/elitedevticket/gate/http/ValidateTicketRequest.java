package br.com.elitedevticket.gate.http;

import br.com.elitedevticket.gate.domain.ValidationMethod;
import java.util.UUID;

public record ValidateTicketRequest(
        UUID validationAttemptId,
        UUID selectedEventId,
        String manualCode,
        ValidationMethod method
) {
    public void validate() {
        if (validationAttemptId == null) {
            throw new IllegalArgumentException("validationAttemptId e obrigatorio.");
        }
        if (selectedEventId == null) {
            throw new IllegalArgumentException("selectedEventId e obrigatorio.");
        }
        if (method == null) {
            throw new IllegalArgumentException("method e obrigatorio.");
        }
    }
}
