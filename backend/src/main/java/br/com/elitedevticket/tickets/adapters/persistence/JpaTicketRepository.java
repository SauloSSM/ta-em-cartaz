package br.com.elitedevticket.tickets.adapters.persistence;

import br.com.elitedevticket.tickets.application.TicketRepository;
import br.com.elitedevticket.tickets.domain.Ticket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaTicketRepository implements TicketRepository {

    private final SpringDataTicketRepository repository;

    JpaTicketRepository(SpringDataTicketRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = new TicketEntity(ticket);
        TicketEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<Ticket> saveAll(List<Ticket> tickets) {
        List<TicketEntity> entities = tickets.stream().map(TicketEntity::new).toList();
        List<TicketEntity> saved = repository.saveAll(entities);
        return saved.stream().map(TicketEntity::toDomain).toList();
    }

    @Override
    public List<Ticket> findByReservationId(UUID reservationId) {
        return repository.findByReservationIdOrderByOrdinalAsc(reservationId)
                .stream()
                .map(TicketEntity::toDomain)
                .toList();
    }

    @Override
    public List<Ticket> findByCustomerId(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(TicketEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        return repository.findById(id).map(TicketEntity::toDomain);
    }

    @Override
    public Optional<Ticket> findByValidationToken(String validationToken) {
        if (validationToken == null || validationToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByValidationToken(validationToken.trim()).map(TicketEntity::toDomain);
    }

    @Override
    public Optional<Ticket> findByManualCode(String manualCode) {
        if (manualCode == null || manualCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = br.com.elitedevticket.tickets.domain.TicketCredentialGenerator.normalizeManualCode(manualCode);
        if (normalized == null || normalized.isBlank()) {
            return Optional.empty();
        }
        return repository.findByManualCode(normalized).map(TicketEntity::toDomain);
    }

    @Override
    public Optional<Ticket> findByShareToken(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByShareToken(shareToken.trim()).map(TicketEntity::toDomain);
    }
}
