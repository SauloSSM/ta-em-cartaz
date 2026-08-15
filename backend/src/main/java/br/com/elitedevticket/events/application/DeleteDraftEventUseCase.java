package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteDraftEventUseCase {

    private final EventRepository eventRepository;

    public DeleteDraftEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void execute(UUID eventId, UUID organizerId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");

        Event existing = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        if (!existing.isOwnedBy(organizerId)) {
            throw new EventForbiddenException();
        }

        existing.assertCanBeDeleted();
        eventRepository.deleteById(eventId);
    }
}
