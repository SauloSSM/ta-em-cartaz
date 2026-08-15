package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.http.ApiErrorResponse;
import br.com.elitedevticket.auth.http.AuthErrorCode;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.elitedevticket.events.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventsExceptionHandler {

    private final Clock clock;

    public EventsExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(InvalidEventRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidEventRequestException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                AuthErrorCode.AUTH_INVALID_REQUEST,
                ex.getMessage(),
                ex.fieldErrors(),
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<EventApiErrorResponse> handleNotFound(EventNotFoundException ex) {
        EventApiErrorResponse body = new EventApiErrorResponse(
                EventErrorCode.EVENT_NOT_FOUND,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(EventForbiddenException.class)
    public ResponseEntity<EventApiErrorResponse> handleForbidden(EventForbiddenException ex) {
        EventApiErrorResponse body = new EventApiErrorResponse(
                EventErrorCode.EVENT_FORBIDDEN,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
