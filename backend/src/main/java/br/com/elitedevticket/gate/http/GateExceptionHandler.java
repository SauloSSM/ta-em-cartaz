package br.com.elitedevticket.gate.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.gate.domain.GateAttemptConflictException;
import br.com.elitedevticket.gate.domain.GateDomainException;
import br.com.elitedevticket.gate.domain.GateOwnershipException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.elitedevticket.gate.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GateExceptionHandler {

    private final Clock clock;

    public GateExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @ExceptionHandler(GateAttemptConflictException.class)
    public ResponseEntity<GateApiErrorResponse> handleConflict(GateAttemptConflictException ex) {
        GateApiErrorResponse body = new GateApiErrorResponse(
                GateErrorCode.GATE_ATTEMPT_CONFLICT,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant().truncatedTo(ChronoUnit.MICROS)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(GateOwnershipException.class)
    public ResponseEntity<GateApiErrorResponse> handleOwnership(GateOwnershipException ex) {
        GateApiErrorResponse body = new GateApiErrorResponse(
                GateErrorCode.AUTH_FORBIDDEN,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant().truncatedTo(ChronoUnit.MICROS)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GateApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorResponse(err.getField(), err.getDefaultMessage()))
                .toList();

        GateApiErrorResponse body = new GateApiErrorResponse(
                GateErrorCode.GATE_INVALID_REQUEST,
                "Requisicao de validacao contem campos invalidos.",
                fieldErrors,
                UUID.randomUUID().toString(),
                clock.instant().truncatedTo(ChronoUnit.MICROS)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class, GateDomainException.class})
    public ResponseEntity<GateApiErrorResponse> handleBadRequest(Exception ex) {
        GateApiErrorResponse body = new GateApiErrorResponse(
                GateErrorCode.GATE_INVALID_REQUEST,
                "Requisicao invalida para validacao de ingresso.",
                null,
                UUID.randomUUID().toString(),
                clock.instant().truncatedTo(ChronoUnit.MICROS)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
