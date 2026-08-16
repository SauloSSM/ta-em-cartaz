package br.com.elitedevticket.tickets.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.tickets.application.TicketQueryService;
import br.com.elitedevticket.tickets.domain.Ticket;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/my-tickets")
public class MyTicketsController {

    private final TicketQueryService ticketQueryService;

    public MyTicketsController(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<MyTicketListResponse> listMyTickets(
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        List<Ticket> tickets = ticketQueryService.listOwnTickets(sessionUser.id());
        List<MyTicketResponse> responses = tickets.stream()
                .map(MyTicketsController::toResponse)
                .toList();
        return ResponseEntity.ok(new MyTicketListResponse(responses));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<MyTicketResponse> getMyTicket(
            @AuthenticationPrincipal SessionUser sessionUser,
            @PathVariable UUID ticketId
    ) {
        Ticket ticket = ticketQueryService.getOwnTicket(sessionUser.id(), ticketId);
        return ResponseEntity.ok(toResponse(ticket));
    }

    private static MyTicketResponse toResponse(Ticket ticket) {
        return new MyTicketResponse(
                ticket.id(),
                ticket.reservationId(),
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
