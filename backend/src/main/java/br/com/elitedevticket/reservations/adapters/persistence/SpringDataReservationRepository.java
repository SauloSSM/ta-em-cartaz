package br.com.elitedevticket.reservations.adapters.persistence;

import br.com.elitedevticket.reservations.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservationEntity r WHERE r.id = :id")
    Optional<ReservationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT r FROM ReservationEntity r WHERE r.customerId = :customerId AND r.eventId = :eventId AND r.status = :status ORDER BY r.createdAt DESC")
    List<ReservationEntity> findByCustomerIdAndEventIdAndStatusOrderByCreatedAtDesc(
            @Param("customerId") UUID customerId,
            @Param("eventId") UUID eventId,
            @Param("status") ReservationStatus status
    );

    List<ReservationEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Query("SELECT r.id FROM ReservationEntity r WHERE r.status = :status AND r.expiresAt <= :serverNow ORDER BY r.id ASC")
    List<UUID> findExpiredHoldingIds(
            @Param("status") ReservationStatus status,
            @Param("serverNow") Instant serverNow,
            Pageable pageable
    );

    @Query("SELECT r.id FROM ReservationEntity r WHERE r.sectorId = :sectorId AND r.status = :status AND r.expiresAt <= :serverNow ORDER BY r.id ASC")
    List<UUID> findExpiredHoldingIdsBySector(
            @Param("sectorId") UUID sectorId,
            @Param("status") ReservationStatus status,
            @Param("serverNow") Instant serverNow
    );
}
