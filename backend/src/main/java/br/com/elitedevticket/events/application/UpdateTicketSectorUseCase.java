package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTicketSectorUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;
    private final Clock clock;

    public UpdateTicketSectorUseCase(
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
            UUID sectorId,
            UUID organizerId,
            String name,
            String description,
            int capacity,
            BigDecimal price
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado."));

        if (!event.isOwnedBy(organizerId)) {
            throw new EventForbiddenException("Apenas o organizador proprietário pode alterar setores do evento.");
        }

        if (event.status() != EventStatus.DRAFT) {
            throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Apenas eventos em rascunho podem ter setores modificados.");
        }

        TicketSector sector = ticketSectorRepository.findById(sectorId)
                .orElseThrow(() -> new TicketSectorNotFoundException("Setor não encontrado."));

        if (!sector.eventId().equals(eventId)) {
            throw new TicketSectorNotFoundException("Setor não pertence ao evento especificado.");
        }

        Instant now = clock.instant();
        TicketSector updated = sector.withUpdatedDraftDetails(
                name.trim(),
                description,
                capacity,
                price,
                now
        );

        return ticketSectorRepository.save(updated);
    }
}
