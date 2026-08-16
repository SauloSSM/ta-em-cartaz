package br.com.elitedevticket.payments.adapters.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findByReservationIdOrderByCreatedAtDesc(UUID reservationId);
}
