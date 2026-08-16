package br.com.elitedevticket.reservations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class ExpireReservationUseCaseTest {

    private ReservationRepository reservationRepository;
    private EventStockPort eventStockPort;
    private Clock clock;
    private ExpireReservationUseCase useCase;

    private final Instant now = Instant.parse("2026-08-16T15:00:00Z");

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        eventStockPort = mock(EventStockPort.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new ExpireReservationUseCase(reservationRepository, eventStockPort, clock);
    }

    @Test
    @DisplayName("Expira hold vencido (serverNow >= expiresAt), devolve estoque ao setor e respeita ordem de locks Reservation -> TicketSector")
    void shouldExpireVencidoHoldAndRestoreStock() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        // Expired 1 minute ago (expiresAt = now - 1 min)
        Instant expiresAt = now.minus(1, ChronoUnit.MINUTES);
        Instant createdAt = expiresAt.minus(10, ChronoUnit.MINUTES);

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                3,
                new BigDecimal("150.00"),
                new BigDecimal("450.00"),
                ReservationStatus.HOLDING,
                expiresAt,
                createdAt,
                null
        );

        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                "Descrição",
                100,
                10,
                new BigDecimal("150.00"),
                createdAt,
                createdAt
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));

        boolean result = useCase.execute(reservationId);

        assertThat(result).isTrue();

        InOrder inOrder = inOrder(reservationRepository, eventStockPort);
        inOrder.verify(reservationRepository).findByIdWithLock(reservationId);
        inOrder.verify(eventStockPort).findSectorByIdWithLock(sectorId);
        inOrder.verify(eventStockPort).updateSectorAvailability(sectorId, 13); // 10 + 3

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        inOrder.verify(reservationRepository).save(captor.capture());

        Reservation saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(saved.id()).isEqualTo(reservationId);
    }

    @Test
    @DisplayName("Não expira hold antes de expiresAt (serverNow < expiresAt)")
    void shouldNotExpireHoldBeforeExpiresAt() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        // Expires in 5 minutes
        Instant expiresAt = now.plus(5, ChronoUnit.MINUTES);
        Instant createdAt = now.minus(5, ChronoUnit.MINUTES);

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                ReservationStatus.HOLDING,
                expiresAt,
                createdAt,
                null
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        boolean result = useCase.execute(reservationId);

        assertThat(result).isFalse();
        verify(eventStockPort, never()).findSectorByIdWithLock(any());
        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Expira hold exatamente em serverNow == expiresAt")
    void shouldExpireHoldExactlyAtExpiresAt() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        // Exactly at boundary
        Instant expiresAt = now;
        Instant createdAt = now.minus(10, ChronoUnit.MINUTES);

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                ReservationStatus.HOLDING,
                expiresAt,
                createdAt,
                null
        );

        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                "Descrição",
                50,
                5,
                new BigDecimal("100.00"),
                createdAt,
                createdAt
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));

        boolean result = useCase.execute(reservationId);

        assertThat(result).isTrue();
        verify(eventStockPort).updateSectorAvailability(sectorId, 7);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Reservation já EXPIRED não devolve estoque novamente (idempotência)")
    void shouldNotRestoreStockIfAlreadyExpired() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                ReservationStatus.EXPIRED,
                now.minus(1, ChronoUnit.MINUTES),
                now.minus(11, ChronoUnit.MINUTES),
                null
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        boolean result = useCase.execute(reservationId);

        assertThat(result).isFalse();
        verify(eventStockPort, never()).findSectorByIdWithLock(any());
        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reservation CONFIRMED nunca é expirada e nunca devolve estoque")
    void shouldNeverExpireConfirmedReservation() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                ReservationStatus.CONFIRMED,
                now.minus(1, ChronoUnit.MINUTES),
                now.minus(11, ChronoUnit.MINUTES),
                now.minus(2, ChronoUnit.MINUTES)
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        boolean result = useCase.execute(reservationId);

        assertThat(result).isFalse();
        verify(eventStockPort, never()).findSectorByIdWithLock(any());
        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Inconsistência que faria o estoque ultrapassar capacity lança IllegalStateException sem alterar estoque nem reserva")
    void shouldThrowIllegalStateExceptionWhenRestoredStockExceedsCapacity() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Reservation reservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                4,
                new BigDecimal("100.00"),
                new BigDecimal("400.00"),
                ReservationStatus.HOLDING,
                now.minus(1, ChronoUnit.MINUTES),
                now.minus(11, ChronoUnit.MINUTES),
                null
        );

        // Capacity is 50, currently at 48 (48 + 4 = 52 > 50 -> invariant violation)
        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                "Descrição",
                50,
                48,
                new BigDecimal("100.00"),
                now.minus(11, ChronoUnit.MINUTES),
                now.minus(11, ChronoUnit.MINUTES)
        );

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));

        assertThatThrownBy(() -> useCase.execute(reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inconsistência de estoque");

        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }
}
