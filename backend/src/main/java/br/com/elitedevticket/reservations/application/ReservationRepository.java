package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.reservations.domain.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(UUID id);

    Optional<Reservation> findByIdWithLock(UUID id);

    Optional<Reservation> findHoldingByCustomerAndEvent(UUID customerId, UUID eventId);

    List<Reservation> findByCustomerId(UUID customerId);

    List<UUID> findExpiredHoldingIds(Instant serverNow, int limit);

    List<UUID> findExpiredHoldingIdsBySector(UUID sectorId, Instant serverNow);
}
