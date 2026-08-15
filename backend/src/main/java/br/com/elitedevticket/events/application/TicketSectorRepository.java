package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.TicketSector;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketSectorRepository {
    TicketSector save(TicketSector ticketSector);

    Optional<TicketSector> findById(UUID id);

    List<TicketSector> findByEventId(UUID eventId);

    void deleteById(UUID id);
}
