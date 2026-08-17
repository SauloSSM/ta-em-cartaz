package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TicketValidationPort {

    Optional<Ticket> findByManualCodeForValidation(String manualCode);

    Ticket markTicketAsUsed(UUID ticketId, Instant usedAt, UUID gateUserId);
}
