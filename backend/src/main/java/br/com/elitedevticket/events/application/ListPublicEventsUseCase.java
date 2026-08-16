package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPublicEventsUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;
    private final Clock clock;

    public ListPublicEventsUseCase(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PublicEventItem> execute(String search) {
        List<Event> events = eventRepository.findPublished(search);
        return events.stream()
                .map(this::toPublicItem)
                .toList();
    }

    private PublicEventItem toPublicItem(Event event) {
        List<TicketSector> sectors = ticketSectorRepository.findByEventId(event.id());
        BigDecimal startingPrice = sectors.stream()
                .map(TicketSector::price)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        boolean salesClosed = event.startsAt() != null && !clock.instant().isBefore(event.startsAt());
        return new PublicEventItem(event, startingPrice, salesClosed);
    }
}
