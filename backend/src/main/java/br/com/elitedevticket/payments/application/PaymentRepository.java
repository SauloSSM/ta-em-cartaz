package br.com.elitedevticket.payments.application;

import br.com.elitedevticket.payments.domain.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    List<Payment> findByReservationId(UUID reservationId);
}
