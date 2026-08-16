package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.tickets.domain.TicketNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.elitedevticket.tickets.http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TicketsExceptionHandler {

    private final Clock clock;

    public TicketsExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<TicketApiErrorResponse> handleTicketNotFound(TicketNotFoundException ex) {
        TicketApiErrorResponse body = new TicketApiErrorResponse(
                TicketErrorCode.TICKET_NOT_FOUND,
                ex.getMessage(),
                null,
                UUID.randomUUID().toString(),
                clock.instant()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
