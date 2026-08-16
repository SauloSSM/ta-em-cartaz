package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    List<Ticket> saveAll(List<Ticket> tickets);

    List<Ticket> findByReservationId(UUID reservationId);

    List<Ticket> findByCustomerId(UUID customerId);

    Optional<Ticket> findById(UUID id);

    Optional<Ticket> findByValidationToken(String validationToken);

    Optional<Ticket> findByManualCode(String manualCode);

    Optional<Ticket> findByShareToken(String shareToken);
}
