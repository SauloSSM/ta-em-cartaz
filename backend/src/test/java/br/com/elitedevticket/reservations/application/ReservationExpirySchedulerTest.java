package br.com.elitedevticket.reservations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationExpirySchedulerTest {

    private ReservationRepository reservationRepository;
    private ExpireReservationUseCase expireReservationUseCase;
    private Clock clock;
    private ReservationExpiryScheduler scheduler;

    private final Instant now = Instant.parse("2026-08-16T15:00:00Z");

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        expireReservationUseCase = mock(ExpireReservationUseCase.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        scheduler = new ReservationExpiryScheduler(reservationRepository, expireReservationUseCase, clock);
    }

    @Test
    @DisplayName("Executa limpeza em lote de reservas vencidas e chama ExpireReservationUseCase para cada uma")
    void shouldExecuteCleanupInBatchForExpiredHoldings() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(reservationRepository.findExpiredHoldingIds(eq(now), eq(50))).thenReturn(List.of(id1, id2));
        when(expireReservationUseCase.execute(id1)).thenReturn(true);
        when(expireReservationUseCase.execute(id2)).thenReturn(true);

        int count = scheduler.runCleanup();

        assertThat(count).isEqualTo(2);
        verify(expireReservationUseCase).execute(id1);
        verify(expireReservationUseCase).execute(id2);
    }

    @Test
    @DisplayName("Não executa nenhuma expiração quando não há reservas vencidas")
    void shouldDoNothingWhenNoExpiredHoldingsFound() {
        when(reservationRepository.findExpiredHoldingIds(eq(now), eq(50))).thenReturn(Collections.emptyList());

        int count = scheduler.runCleanup();

        assertThat(count).isZero();
        verify(expireReservationUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Tolera falhas individuais sem interromper a reconciliação das demais no lote")
    void shouldHandleIndividualFailuresGracefully() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(reservationRepository.findExpiredHoldingIds(eq(now), eq(50))).thenReturn(List.of(id1, id2));
        when(expireReservationUseCase.execute(id1)).thenThrow(new RuntimeException("DB temporary glitch"));
        when(expireReservationUseCase.execute(id2)).thenReturn(true);

        int count = scheduler.runCleanup();

        assertThat(count).isEqualTo(1);
        verify(expireReservationUseCase).execute(id1);
        verify(expireReservationUseCase).execute(id2);
    }
}
