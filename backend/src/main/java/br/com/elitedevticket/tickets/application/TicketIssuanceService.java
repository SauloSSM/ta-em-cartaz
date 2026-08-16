package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketIssuanceService implements TicketIssuancePort {

    private final TicketRepository ticketRepository;
    private final TicketCredentialGenerator credentialGenerator;

    public TicketIssuanceService(
            TicketRepository ticketRepository,
            TicketCredentialGenerator credentialGenerator
    ) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository must not be null");
        this.credentialGenerator = Objects.requireNonNull(credentialGenerator, "credentialGenerator must not be null");
    }

    @Override
    @Transactional
    public List<Ticket> issueTickets(IssueTicketsCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        List<Ticket> existing = ticketRepository.findByReservationId(command.reservationId());
        if (!existing.isEmpty() && existing.size() == command.quantity()) {
            return existing;
        }

        List<Ticket> tickets = new ArrayList<>(command.quantity());
        for (int ordinal = 1; ordinal <= command.quantity(); ordinal++) {
            UUID ticketId = UUID.randomUUID();
            String validationToken = credentialGenerator.generateValidationToken();
            String shareToken = credentialGenerator.generateShareToken();
            String manualCode = credentialGenerator.generateManualCode();

            Ticket ticket = Ticket.createValid(
                    ticketId,
                    command.reservationId(),
                    command.eventId(),
                    command.sectorId(),
                    command.customerId(),
                    ordinal,
                    validationToken,
                    manualCode,
                    shareToken,
                    command.serverNow()
            );
            tickets.add(ticket);
        }

        return ticketRepository.saveAll(tickets);
    }
}
