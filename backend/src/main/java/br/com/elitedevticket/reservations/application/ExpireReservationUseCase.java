package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpireReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final EventStockPort eventStockPort;
    private final Clock clock;

    public ExpireReservationUseCase(
            ReservationRepository reservationRepository,
            EventStockPort eventStockPort,
            Clock clock
    ) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository must not be null");
        this.eventStockPort = Objects.requireNonNull(eventStockPort, "eventStockPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public boolean execute(UUID reservationId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Instant serverNow = clock.instant();

        // 1. Lock Reservation with PESSIMISTIC_WRITE (AD-5: Reservation -> TicketSector)
        Optional<Reservation> lockedReservationOpt = reservationRepository.findByIdWithLock(reservationId);
        if (lockedReservationOpt.isEmpty()) {
            return false;
        }

        Reservation reservation = lockedReservationOpt.get();

        // If not HOLDING or not yet expired, do nothing (idempotent)
        if (reservation.status() != ReservationStatus.HOLDING || !reservation.isExpired(serverNow)) {
            return false;
        }

        // 2. Lock TicketSector with PESSIMISTIC_WRITE
        Optional<TicketSector> lockedSectorOpt = eventStockPort.findSectorByIdWithLock(reservation.sectorId());
        if (lockedSectorOpt.isPresent()) {
            TicketSector sector = lockedSectorOpt.get();
            int restoredAvailable = sector.availableQuantity() + reservation.quantity();
            if (restoredAvailable > sector.capacity()) {
                throw new IllegalStateException(
                        "Inconsistência de estoque: a devolução de " + reservation.quantity() +
                        " unidades excederia a capacidade máxima (" + sector.capacity() +
                        ") para o setor " + sector.id() + " com estoque atual " + sector.availableQuantity()
                );
            }
            eventStockPort.updateSectorAvailability(sector.id(), restoredAvailable);
        }

        // 3. Mark reservation as EXPIRED
        Reservation expiredReservation = reservation.expire();
        reservationRepository.save(expiredReservation);

        return true;
    }
}
