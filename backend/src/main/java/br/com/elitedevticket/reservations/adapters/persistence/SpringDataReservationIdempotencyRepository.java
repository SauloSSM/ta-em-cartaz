package br.com.elitedevticket.reservations.adapters.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataReservationIdempotencyRepository extends JpaRepository<ReservationIdempotencyEntity, UUID> {
    Optional<ReservationIdempotencyEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}
