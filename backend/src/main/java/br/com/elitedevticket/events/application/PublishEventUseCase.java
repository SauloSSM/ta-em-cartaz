package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.TicketSector;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishEventUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;
    private final Clock clock;

    public PublishEventUseCase(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
        this.clock = clock;
    }

    @Transactional
    public Event execute(UUID eventId, UUID organizerId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        if (!event.isOwnedBy(organizerId)) {
            throw new EventForbiddenException();
        }

        List<TicketSector> sectors = ticketSectorRepository.findByEventId(eventId);
        Instant now = Instant.now(clock);
        Event published = event.publish(now, sectors, clock);

        return eventRepository.save(published);
    }
}
