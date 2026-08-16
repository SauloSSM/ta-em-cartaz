package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.domain.Event;
import java.util.List;
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

    @Override
    public List<Event> findByOrganizerId(UUID organizerId) {
        return repository.findByOrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(EventEntity::toDomain)
                .toList();
    }

    @Override
    public List<Event> findPublished(String titleSearch) {
        if (titleSearch == null || titleSearch.isBlank()) {
            return repository.findByStatusOrderByStartsAtAscCreatedAtDesc(br.com.elitedevticket.events.domain.EventStatus.PUBLISHED)
                    .stream()
                    .map(EventEntity::toDomain)
                    .toList();
        }
        return repository.findByStatusAndTitleContainingIgnoreCaseOrderByStartsAtAscCreatedAtDesc(
                        br.com.elitedevticket.events.domain.EventStatus.PUBLISHED,
                        titleSearch.trim()
                )
                .stream()
                .map(EventEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
