package br.com.elitedevticket.events.adapters.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTicketSectorRepository extends JpaRepository<TicketSectorEntity, UUID> {
    List<TicketSectorEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TicketSectorEntity s WHERE s.id = :id")
    Optional<TicketSectorEntity> findByIdForUpdate(@Param("id") UUID id);
}
