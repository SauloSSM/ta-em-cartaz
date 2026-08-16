package br.com.elitedevticket.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import br.com.elitedevticket.payments.adapters.gateway.FakePaymentGateway;
import br.com.elitedevticket.payments.domain.Payment;
import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import br.com.elitedevticket.payments.domain.PaymentStatus;
import br.com.elitedevticket.reservations.application.ReservationPaymentPort;
import br.com.elitedevticket.reservations.domain.IdempotencyConflictException;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationAlreadyConfirmedException;
import br.com.elitedevticket.reservations.domain.ReservationExpiredException;
import br.com.elitedevticket.reservations.domain.ReservationNotFoundException;
import br.com.elitedevticket.reservations.domain.ReservationOwnershipException;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessPaymentAttemptUseCaseTest {

    private CustomerLockPort customerLockPort;
    private ReservationPaymentPort reservationPaymentPort;
    private PaymentRepository paymentRepository;
    private PaymentGateway paymentGateway;
    private Clock clock;
    private ProcessPaymentAttemptUseCase useCase;

    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");
    private final UUID customerId = UUID.randomUUID();
    private final UUID otherCustomerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final UUID reservationId = UUID.randomUUID();
    private final UUID paymentAttemptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerLockPort = mock(CustomerLockPort.class);
        reservationPaymentPort = mock(ReservationPaymentPort.class);
        paymentRepository = mock(PaymentRepository.class);
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        paymentGateway = new FakePaymentGateway(clock);

        useCase = new ProcessPaymentAttemptUseCase(
                customerLockPort,
                reservationPaymentPort,
                paymentRepository,
                paymentGateway,
                clock
        );
    }

    private Reservation createHoldingReservation(UUID customer, Instant createdAt, Instant expiresAt) {
        return new Reservation(
                reservationId,
                customer,
                eventId,
                sectorId,
                2,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"),
                ReservationStatus.HOLDING,
                expiresAt,
                createdAt,
                null
        );
    }

    @Test
    @DisplayName("Processa tentativa DECLINED com sucesso mantendo a reserva HOLDING e gravando o Payment com snapshots")
    void shouldProcessDeclinedPaymentSuccessfully() {
        Reservation reservation = createHoldingReservation(customerId, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        Payment result = useCase.execute(command);

        verify(customerLockPort).lockCustomer(customerId);
        verify(reservationPaymentPort).findByIdForUpdate(reservationId);
        verify(reservationPaymentPort, never()).expireReservation(any());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(paymentAttemptId);
        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.currency()).isEqualTo("BRL");
        assertThat(result.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(result.provider()).isEqualTo("FAKE");
        assertThat(result.declineReason()).isEqualTo("SIMULATED_DECLINE");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.processedAt()).isEqualTo(now);
        assertThat(result.fingerprint()).isNotEmpty();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(saved.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Replay idempotente com mesmo paymentAttemptId e mesmo fingerprint retorna exatamente o Payment persistido sem novo processamento")
    void shouldReturnPersistedPaymentOnIdempotentReplay() {
        String expectedFingerprint = ProcessPaymentAttemptUseCase.calculateFingerprint(
                customerId,
                reservationId,
                PaymentSimulatedOutcome.DECLINED
        );

        Payment existingPayment = new Payment(
                paymentAttemptId,
                reservationId,
                customerId,
                new BigDecimal("300.00"),
                "BRL",
                PaymentStatus.DECLINED,
                "FAKE",
                "SIMULATED_DECLINE",
                expectedFingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        Payment result = useCase.execute(command);

        verify(customerLockPort).lockCustomer(customerId);
        verify(paymentRepository).findById(paymentAttemptId);
        verify(reservationPaymentPort, never()).findByIdForUpdate(any());
        verify(paymentRepository, never()).save(any());

        assertThat(result).isSameAs(existingPayment);
    }

    @Test
    @DisplayName("Reutilização de paymentAttemptId com fingerprint divergente lança IdempotencyConflictException")
    void shouldThrowConflictWhenReusingPaymentAttemptIdWithDifferentFingerprint() {
        String divergentFingerprint = "v1:otherCustomer:otherReservation:DECLINED";

        Payment existingPayment = new Payment(
                paymentAttemptId,
                reservationId,
                customerId,
                new BigDecimal("300.00"),
                "BRL",
                PaymentStatus.DECLINED,
                "FAKE",
                "SIMULATED_DECLINE",
                divergentFingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("parâmetros incompatíveis");

        verify(customerLockPort).lockCustomer(customerId);
        verify(reservationPaymentPort, never()).findByIdForUpdate(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tentativa de pagamento em reserva de outro usuário lança ReservationOwnershipException")
    void shouldThrowWhenUserDoesNotOwnReservation() {
        Reservation otherUserReservation = createHoldingReservation(otherCustomerId, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(otherUserReservation));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationOwnershipException.class)
                .hasMessageContaining("pertence a outro usuário");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tentativa de pagamento em reserva expirada pelo tempo lança ReservationExpiredException e dispara expiração")
    void shouldThrowAndExpireWhenReservationTimeIsExpired() {
        Instant expiredAt = now.minusSeconds(1);
        Reservation expiredReservation = createHoldingReservation(customerId, now.minusSeconds(601), expiredAt);

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(expiredReservation));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationExpiredException.class)
                .hasMessageContaining("expirou");

        verify(reservationPaymentPort).expireReservation(reservationId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tentativa de pagamento em reserva já confirmada lança ReservationAlreadyConfirmedException")
    void shouldThrowWhenReservationIsAlreadyConfirmed() {
        Reservation confirmedReservation = new Reservation(
                reservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("150.00"),
                new BigDecimal("300.00"),
                ReservationStatus.CONFIRMED,
                now.plusSeconds(600),
                now.minusSeconds(60),
                now.minusSeconds(10)
        );

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(confirmedReservation));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationAlreadyConfirmedException.class)
                .hasMessageContaining("já foi confirmada");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tentativa de pagamento para reserva inexistente lança ReservationNotFoundException")
    void shouldThrowWhenReservationNotFound() {
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.empty());

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.DECLINED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining("não encontrada");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cálculo do fingerprint v1 é determinístico e diferencia cliente, reserva e outcome")
    void shouldCalculateCanonicalFingerprintCorrectly() {
        String fp1 = ProcessPaymentAttemptUseCase.calculateFingerprint(customerId, reservationId, PaymentSimulatedOutcome.DECLINED);
        String fp2 = ProcessPaymentAttemptUseCase.calculateFingerprint(customerId, reservationId, PaymentSimulatedOutcome.DECLINED);
        String fpDifferentOutcome = ProcessPaymentAttemptUseCase.calculateFingerprint(customerId, reservationId, PaymentSimulatedOutcome.APPROVED);
        String fpDifferentRes = ProcessPaymentAttemptUseCase.calculateFingerprint(customerId, UUID.randomUUID(), PaymentSimulatedOutcome.DECLINED);

        assertThat(fp1).isEqualTo(fp2);
        assertThat(fp1).isNotEqualTo(fpDifferentOutcome);
        assertThat(fp1).isNotEqualTo(fpDifferentRes);
        assertThat(fp1).matches("^[a-f0-9]{64}$");
    }
}
