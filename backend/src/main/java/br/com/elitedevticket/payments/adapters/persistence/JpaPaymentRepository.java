package br.com.elitedevticket.payments.adapters.persistence;

import br.com.elitedevticket.payments.application.PaymentRepository;
import br.com.elitedevticket.payments.domain.Payment;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPaymentRepository repository;

    public JpaPaymentRepository(SpringDataPaymentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Payment save(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        PaymentEntity entity = new PaymentEntity(payment);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByReservationId(UUID reservationId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        return repository.findByReservationIdOrderByCreatedAtDesc(reservationId)
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }
}
