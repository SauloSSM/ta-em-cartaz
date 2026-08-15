package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.domain.Event;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaEventRepository implements EventRepository {

    private final SpringDataEventRepository repository;

    JpaEventRepository(SpringDataEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Event save(Event event) {
        EventEntity entity = new EventEntity(event);
        EventEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return repository.findById(id).map(EventEntity::toDomain);
    }
}
