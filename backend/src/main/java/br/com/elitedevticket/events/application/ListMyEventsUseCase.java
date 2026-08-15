package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMyEventsUseCase {

    private final EventRepository eventRepository;

    public ListMyEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<Event> execute(UUID organizerId) {
        Objects.requireNonNull(organizerId, "organizerId must not be null");
        return eventRepository.findByOrganizerId(organizerId);
    }
}
