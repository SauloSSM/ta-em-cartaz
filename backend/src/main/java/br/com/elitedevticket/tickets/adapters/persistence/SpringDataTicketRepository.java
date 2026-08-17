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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TicketEntity t WHERE t.manualCode = :manualCode")
    Optional<TicketEntity> findByManualCodeForUpdate(@org.springframework.data.repository.query.Param("manualCode") String manualCode);

    Optional<TicketEntity> findByShareToken(String shareToken);
}
