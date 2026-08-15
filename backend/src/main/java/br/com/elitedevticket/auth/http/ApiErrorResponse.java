package br.com.elitedevticket.auth.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        AuthErrorCode code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldErrorResponse> fieldErrors,
        String traceId,
        Instant timestamp) {
}
