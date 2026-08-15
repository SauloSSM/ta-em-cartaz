package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteTicketSectorUseCase {

    private final EventRepository eventRepository;
    private final TicketSectorRepository ticketSectorRepository;

    public DeleteTicketSectorUseCase(
            EventRepository eventRepository,
            TicketSectorRepository ticketSectorRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketSectorRepository = ticketSectorRepository;
    }

    public void execute(UUID eventId, UUID sectorId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado."));

        if (!event.isOwnedBy(organizerId)) {
            throw new EventForbiddenException("Apenas o organizador proprietário pode excluir setores do evento.");
        }

        if (event.status() != EventStatus.DRAFT) {
            throw new EventConflictException("EVENT_CANNOT_BE_MODIFIED", "Apenas setores de eventos em rascunho podem ser excluídos.");
        }

        TicketSector sector = ticketSectorRepository.findById(sectorId)
                .orElseThrow(() -> new TicketSectorNotFoundException("Setor não encontrado."));

        if (!sector.eventId().equals(eventId)) {
            throw new TicketSectorNotFoundException("Setor não pertence ao evento especificado.");
        }

        ticketSectorRepository.deleteById(sectorId);
    }
}
