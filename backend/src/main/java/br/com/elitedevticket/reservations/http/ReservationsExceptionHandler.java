package br.com.elitedevticket.reservations.http;

import br.com.elitedevticket.auth.http.ApiErrorResponse;
import br.com.elitedevticket.auth.http.AuthErrorCode;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import br.com.elitedevticket.reservations.domain.EventNotPublishedException;
import br.com.elitedevticket.reservations.domain.InsufficientAvailabilityException;
import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;
import br.com.elitedevticket.reservations.domain.ReservationNotFoundException;
import br.com.elitedevticket.reservations.domain.SalesClosedException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.elitedevticket.reservations.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReservationsExceptionHandler {

    private final Clock clock;

    public ReservationsExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(InsufficientAvailabilityException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleInsufficientAvailability(InsufficientAvailabilityException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.INSUFFICIENT_AVAILABILITY,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(SalesClosedException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleSalesClosed(SalesClosedException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.SALES_CLOSED,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(EventNotPublishedException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleEventNotPublished(EventNotPublishedException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.EVENT_NOT_PUBLISHED,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleEventNotFound(EventNotFoundException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.EVENT_NOT_FOUND,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(TicketSectorNotFoundException.class)
    public ResponseEntity<ReservationApiErrorResponse> handleTicketSectorNotFound(TicketSectorNotFoundException ex) {
        ReservationApiErrorResponse body = new ReservationApiErrorResponse(
                ReservationErrorCode.SECTOR_NOT_FOUND,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
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

    @ExceptionHandler(InvalidReservationQuantityException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidQuantity(InvalidReservationQuantityException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_INVALID_REQUEST,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_INVALID_REQUEST,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
