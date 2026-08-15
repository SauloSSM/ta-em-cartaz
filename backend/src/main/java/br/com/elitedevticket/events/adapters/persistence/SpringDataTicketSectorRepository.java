package br.com.elitedevticket.events.adapters.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTicketSectorRepository extends JpaRepository<TicketSectorEntity, UUID> {
    List<TicketSectorEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);
}
