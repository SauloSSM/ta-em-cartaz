package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketQueryService {

    private final TicketRepository ticketRepository;

    public TicketQueryService(TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<Ticket> listOwnTickets(UUID customerId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        return ticketRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Ticket getOwnTicket(UUID customerId, UUID ticketId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(ticketId, "ticketId must not be null");

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ingresso não encontrado."));

        // Ownership enforced by the backend — never trust frontend-supplied customerId
        if (!ticket.customerId().equals(customerId)) {
            // Return 404 to avoid revealing existence of tickets owned by others (AC security)
            throw new TicketNotFoundException("Ingresso não encontrado.");
        }

        return ticket;
    }

    @Transactional(readOnly = true)
    public Ticket getTicketByShareToken(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            throw new TicketNotFoundException("Ingresso não encontrado.");
        }
        return ticketRepository.findByShareToken(shareToken.trim())
                .orElseThrow(() -> new TicketNotFoundException("Ingresso não encontrado."));
    }
}
