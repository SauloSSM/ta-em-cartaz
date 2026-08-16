package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record TicketApiErrorResponse(
        TicketErrorCode code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldErrorResponse> fieldErrors,
        String traceId,
        Instant timestamp
) {
}
