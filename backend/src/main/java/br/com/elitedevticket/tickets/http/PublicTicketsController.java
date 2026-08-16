package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.tickets.application.TicketQueryService;
import br.com.elitedevticket.tickets.domain.Ticket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/tickets")
public class PublicTicketsController {

    private final TicketQueryService ticketQueryService;

    public PublicTicketsController(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    @GetMapping("/{shareToken}")
    public ResponseEntity<PublicTicketResponse> getPublicTicket(@PathVariable String shareToken) {
        Ticket ticket = ticketQueryService.getTicketByShareToken(shareToken);
        return ResponseEntity.ok(toPublicResponse(ticket));
    }

    private static PublicTicketResponse toPublicResponse(Ticket ticket) {
        return new PublicTicketResponse(
                ticket.id(),
                ticket.eventId(),
                ticket.sectorId(),
                ticket.ordinal(),
                ticket.status(),
                ticket.manualCode(),
                ticket.shareToken(),
                ticket.validationToken(),
                ticket.createdAt()
        );
    }
}
