package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.reservations.domain.Reservation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReservationPaymentPort {
    Optional<Reservation> findByIdForUpdate(UUID reservationId);
    void expireReservation(UUID reservationId);
    Reservation confirmReservation(UUID reservationId, Instant confirmedAt);
}

