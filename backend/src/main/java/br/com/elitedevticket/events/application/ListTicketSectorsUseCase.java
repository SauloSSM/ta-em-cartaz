package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListTicketSectorsUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;

    public ListTicketSectorsUseCase(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
    }

    public List<TicketSector> execute(UUID eventId, UUID userId, boolean isOrganizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado."));

        if (event.status() == EventStatus.DRAFT) {
            if (!isOrganizer || userId == null || !event.isOwnedBy(userId)) {
                throw new EventForbiddenException("Acesso não autorizado aos setores do evento em rascunho.");
            }
        }

        return ticketSectorRepository.findByEventId(eventId);
    }
}
