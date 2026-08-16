package br.com.elitedevticket.reservations.adapters.persistence;

import br.com.elitedevticket.reservations.application.ReservationIdempotencyRepository;
import br.com.elitedevticket.reservations.domain.ReservationIdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaReservationIdempotencyRepository implements ReservationIdempotencyRepository {

    private final SpringDataReservationIdempotencyRepository springDataRepository;

    public JpaReservationIdempotencyRepository(SpringDataReservationIdempotencyRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<ReservationIdempotencyRecord> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey) {
        return springDataRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(ReservationIdempotencyEntity::toDomain);
    }

    @Override
    public ReservationIdempotencyRecord save(ReservationIdempotencyRecord record) {
        ReservationIdempotencyEntity entity = ReservationIdempotencyEntity.fromDomain(record);
        return springDataRepository.save(entity).toDomain();
    }
}
