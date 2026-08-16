package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.TicketSector;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultEventStockPort implements EventStockPort {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;
    private final Clock clock;

    public DefaultEventStockPort(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
        this.clock = clock;
    }

    @Override
    public Optional<Event> findEventById(UUID eventId) {
        return eventRepository.findById(eventId);
    }

    @Override
    public Optional<TicketSector> findSectorByIdWithLock(UUID sectorId) {
        return ticketSectorRepository.findByIdWithLock(sectorId);
    }

    @Override
    public TicketSector updateSectorAvailability(UUID sectorId, int newAvailableQuantity) {
        TicketSector sector = ticketSectorRepository.findByIdWithLock(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("TicketSector not found: " + sectorId));
        TicketSector updated = sector.withUpdatedDetails(
                sector.name(),
                sector.description(),
                sector.capacity(),
                newAvailableQuantity,
                sector.price(),
                clock.instant()
        );
        return ticketSectorRepository.save(updated);
    }
}
