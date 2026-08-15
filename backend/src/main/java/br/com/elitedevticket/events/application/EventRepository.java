package br.com.elitedevticket.events.application;

import br.com.elitedevticket.events.domain.Event;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(UUID id);
}
