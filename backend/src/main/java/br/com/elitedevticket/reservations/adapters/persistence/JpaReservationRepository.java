package br.com.elitedevticket.reservations.adapters.persistence;

import br.com.elitedevticket.reservations.application.ReservationRepository;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class JpaReservationRepository implements ReservationRepository {

    private final SpringDataReservationRepository repository;

    JpaReservationRepository(SpringDataReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity(reservation);
        ReservationEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return repository.findById(id).map(ReservationEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findByIdWithLock(UUID id) {
        return repository.findByIdForUpdate(id).map(ReservationEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findHoldingByCustomerAndEvent(UUID customerId, UUID eventId) {
        return repository.findByCustomerIdAndEventIdAndStatusOrderByCreatedAtDesc(customerId, eventId, ReservationStatus.HOLDING)
                .stream()
                .findFirst()
                .map(ReservationEntity::toDomain);
    }

    @Override
    public List<Reservation> findByCustomerId(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(ReservationEntity::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findExpiredHoldingIds(Instant serverNow, int limit) {
        return repository.findExpiredHoldingIds(
                ReservationStatus.HOLDING,
                serverNow,
                PageRequest.of(0, Math.max(1, limit))
        );
    }

    @Override
    public List<UUID> findExpiredHoldingIdsBySector(UUID sectorId, Instant serverNow) {
        return repository.findExpiredHoldingIdsBySector(
                sectorId,
                ReservationStatus.HOLDING,
                serverNow
        );
    }
}
