package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.reservations.domain.ReservationIdempotencyRecord;
import java.util.Optional;
import java.util.UUID;

public interface ReservationIdempotencyRepository {
    Optional<ReservationIdempotencyRecord> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
    ReservationIdempotencyRecord save(ReservationIdempotencyRecord record);
}
