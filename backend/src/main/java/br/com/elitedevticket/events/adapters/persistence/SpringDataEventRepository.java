package br.com.elitedevticket.events.adapters.persistence;

import br.com.elitedevticket.events.domain.EventStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEventRepository extends JpaRepository<EventEntity, UUID> {
    List<EventEntity> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    List<EventEntity> findByStatusOrderByStartsAtAscCreatedAtDesc(EventStatus status);

    List<EventEntity> findByStatusAndTitleContainingIgnoreCaseOrderByStartsAtAscCreatedAtDesc(
            EventStatus status,
            String title
    );
}
