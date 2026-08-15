package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.domain.TicketSector;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaTicketSectorRepository implements TicketSectorRepository {

    private final SpringDataTicketSectorRepository repository;

    JpaTicketSectorRepository(SpringDataTicketSectorRepository repository) {
        this.repository = repository;
    }

    @Override
    public TicketSector save(TicketSector ticketSector) {
        TicketSectorEntity entity = new TicketSectorEntity(ticketSector);
        TicketSectorEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<TicketSector> findById(UUID id) {
        return repository.findById(id).map(TicketSectorEntity::toDomain);
    }

    @Override
    public List<TicketSector> findByEventId(UUID eventId) {
        return repository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream()
                .map(TicketSectorEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
