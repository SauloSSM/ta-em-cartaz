package br.com.elitedevticket.reservations.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);
    private static final int BATCH_SIZE = 50;

    private final ReservationRepository reservationRepository;
    private final ExpireReservationUseCase expireReservationUseCase;
    private final Clock clock;

    public ReservationExpiryScheduler(
            ReservationRepository reservationRepository,
            ExpireReservationUseCase expireReservationUseCase,
            Clock clock
    ) {
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository must not be null");
        this.expireReservationUseCase = Objects.requireNonNull(expireReservationUseCase, "expireReservationUseCase must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelay = 30000)
    public int runCleanup() {
        Instant serverNow = clock.instant();
        List<UUID> expiredHoldingIds = reservationRepository.findExpiredHoldingIds(serverNow, BATCH_SIZE);

        if (expiredHoldingIds.isEmpty()) {
            return 0;
        }

        int expiredCount = 0;
        for (UUID reservationId : expiredHoldingIds) {
            try {
                boolean expired = expireReservationUseCase.execute(reservationId);
                if (expired) {
                    expiredCount++;
                }
            } catch (Exception e) {
                log.warn("Erro ao expirar reserva vencida no scheduler: {}", reservationId, e);
            }
        }

        if (expiredCount > 0) {
            log.info("Scheduler de expiração reconciliou {} reservas vencidas", expiredCount);
        }

        return expiredCount;
    }
}
