package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateTicketSectorUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;
    private final Clock clock;

    public CreateTicketSectorUseCase(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
        this.clock = clock;
    }

    public TicketSector execute(
            UUID eventId,
            UUID organizerId,
            String name,
            String description,
            int capacity,
            BigDecimal price
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado."));

        if (!event.isOwnedBy(organizerId)) {
            throw new EventForbiddenException("Apenas o organizador proprietário pode configurar setores do evento.");
        }

        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to zero");
        }

        Instant now = clock.instant();
        String trimmedDesc = description == null || description.trim().isEmpty() ? null : description.trim();
        TicketSector sector = new TicketSector(
                UUID.randomUUID(),
                eventId,
                name.trim(),
                trimmedDesc,
                capacity,
                capacity,
                price,
                now,
                now
        );

        return ticketSectorRepository.save(sector);
    }
}
