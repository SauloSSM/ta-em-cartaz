package br.com.elitedevticket.tickets.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataTicketRepository extends JpaRepository<TicketEntity, UUID> {

    List<TicketEntity> findByReservationIdOrderByOrdinalAsc(UUID reservationId);

    List<TicketEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<TicketEntity> findByValidationToken(String validationToken);

    Optional<TicketEntity> findByManualCode(String manualCode);

    Optional<TicketEntity> findByShareToken(String shareToken);
}
