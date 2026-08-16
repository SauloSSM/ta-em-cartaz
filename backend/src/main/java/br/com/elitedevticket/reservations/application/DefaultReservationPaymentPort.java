package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.reservations.domain.Reservation;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultReservationPaymentPort implements ReservationPaymentPort {

    private final ReservationRepository reservationRepository;
    private final ExpireReservationUseCase expireReservationUseCase;

    public DefaultReservationPaymentPort(
            ReservationRepository reservationRepository,
            ExpireReservationUseCase expireReservationUseCase
    ) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository must not be null");
        this.expireReservationUseCase = Objects.requireNonNull(expireReservationUseCase, "expireReservationUseCase must not be null");
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(UUID reservationId) {
        return reservationRepository.findByIdWithLock(reservationId);
    }

    @Override
    public void expireReservation(UUID reservationId) {
        expireReservationUseCase.execute(reservationId);
    }
}
