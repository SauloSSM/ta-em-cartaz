package br.com.elitedevticket.payments.http;

import br.com.elitedevticket.auth.http.ApiErrorResponse;
import br.com.elitedevticket.auth.http.AuthErrorCode;
import br.com.elitedevticket.auth.http.FieldErrorResponse;
import br.com.elitedevticket.payments.domain.PaymentDomainException;
import br.com.elitedevticket.reservations.domain.IdempotencyConflictException;
import br.com.elitedevticket.reservations.domain.ReservationAlreadyConfirmedException;
import br.com.elitedevticket.reservations.domain.ReservationExpiredException;
import br.com.elitedevticket.reservations.domain.ReservationNotFoundException;
import br.com.elitedevticket.reservations.domain.ReservationOwnershipException;
import br.com.elitedevticket.reservations.http.ReservationApiErrorResponse;
import br.com.elitedevticket.reservations.http.ReservationErrorCode;
import java.time.Clock;
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

@RestControllerAdvice(basePackages = "br.com.elitedevticket.payments.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentsExceptionHandler {

    private final Clock clock;

    public PaymentsExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleReservationNotFound(ReservationNotFoundException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.RESERVATION_NOT_FOUND,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleReservationExpired(ReservationExpiredException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.RESERVATION_EXPIRED,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.IDEMPOTENCY_CONFLICT,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ReservationAlreadyConfirmedException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleAlreadyConfirmed(ReservationAlreadyConfirmedException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.IDEMPOTENCY_CONFLICT,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ReservationOwnershipException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationOwnership(ReservationOwnershipException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_FORBIDDEN,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorResponse(err.getField(), err.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_INVALID_REQUEST,
                "Requisição de pagamento contém campos inválidos.",
                fieldErrors,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class, PaymentDomainException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_INVALID_REQUEST,
                "Requisição inválida para processamento de pagamento.",
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
