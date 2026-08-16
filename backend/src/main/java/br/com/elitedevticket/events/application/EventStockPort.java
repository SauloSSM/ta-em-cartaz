package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.TicketSector;
import java.util.Optional;
import java.util.UUID;

public interface EventStockPort {
    Optional<Event> findEventById(UUID eventId);

    Optional<TicketSector> findSectorByIdWithLock(UUID sectorId);

    TicketSector updateSectorAvailability(UUID sectorId, int newAvailableQuantity);
}
