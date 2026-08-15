package br.com.elitedevticket.events.adapters.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEventRepository extends JpaRepository<EventEntity, UUID> {
    List<EventEntity> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
}
