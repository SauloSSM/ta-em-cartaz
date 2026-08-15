package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEventUseCase {

    private final EventRepository eventRepository;

    public GetEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID eventId, UUID currentUserId, boolean isOrganizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.status() == EventStatus.DRAFT) {
            if (!isOrganizer || currentUserId == null || !event.isOwnedBy(currentUserId)) {
                throw new EventForbiddenException("Acesso negado ao rascunho de evento.");
            }
        }

        return event;
    }
}
