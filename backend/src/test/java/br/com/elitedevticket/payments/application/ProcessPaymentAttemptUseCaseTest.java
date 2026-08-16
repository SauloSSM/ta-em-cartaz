package br.com.elitedevticket.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import br.com.elitedevticket.tickets.application.IssueTicketsCommand;
import br.com.elitedevticket.tickets.application.TicketIssuancePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessPaymentAttemptUseCaseTest {

    private CustomerLockPort customerLockPort;
    private ReservationPaymentPort reservationPaymentPort;
    private TicketIssuancePort ticketIssuancePort;
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
        ticketIssuancePort = mock(TicketIssuancePort.class);
        paymentRepository = mock(PaymentRepository.class);
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        paymentGateway = new FakePaymentGateway(clock);

        useCase = new ProcessPaymentAttemptUseCase(
                customerLockPort,
                reservationPaymentPort,
                ticketIssuancePort,
                paymentRepository,
                paymentGateway,
                clock
        );
    }

    private Reservation createHoldingReservation(UUID customer, int quantity, Instant createdAt, Instant expiresAt) {
        BigDecimal unitPrice = new BigDecimal("150.00");
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new Reservation(
                reservationId,
                customer,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                totalAmount,
                ReservationStatus.HOLDING,
                expiresAt,
                createdAt,
                null
        );
    }

    @Test
    @DisplayName("Processa tentativa DECLINED com sucesso mantendo a reserva HOLDING e gravando o Payment com snapshots")
    void shouldProcessDeclinedPaymentSuccessfully() {
        Reservation reservation = createHoldingReservation(customerId, 2, now, now.plusSeconds(600));
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
        verify(reservationPaymentPort, never()).confirmReservation(any(), any());
        verify(ticketIssuancePort, never()).issueTickets(any());

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
    @DisplayName("Processa tentativa APPROVED com sucesso: confirma Reservation, emite exatamente quantity Tickets e persiste Payment APPROVED")
    void shouldProcessApprovedPaymentSuccessfully() {
        Reservation reservation = createHoldingReservation(customerId, 2, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationPaymentPort.confirmReservation(eq(reservationId), eq(now))).thenReturn(reservation.confirm(now));
        when(ticketIssuancePort.issueTickets(any())).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        Payment result = useCase.execute(command);

        verify(customerLockPort).lockCustomer(customerId);
        verify(reservationPaymentPort).findByIdForUpdate(reservationId);
        verify(reservationPaymentPort).confirmReservation(reservationId, now);

        ArgumentCaptor<IssueTicketsCommand> ticketCaptor = ArgumentCaptor.forClass(IssueTicketsCommand.class);
        verify(ticketIssuancePort).issueTickets(ticketCaptor.capture());
        IssueTicketsCommand issuedCommand = ticketCaptor.getValue();
        assertThat(issuedCommand.reservationId()).isEqualTo(reservationId);
        assertThat(issuedCommand.eventId()).isEqualTo(eventId);
        assertThat(issuedCommand.sectorId()).isEqualTo(sectorId);
        assertThat(issuedCommand.customerId()).isEqualTo(customerId);
        assertThat(issuedCommand.quantity()).isEqualTo(2);
        assertThat(issuedCommand.serverNow()).isEqualTo(now);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(paymentAttemptId);
        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.currency()).isEqualTo("BRL");
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.provider()).isEqualTo("FAKE");
        assertThat(result.declineReason()).isNull();
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.processedAt()).isEqualTo(now);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Processa tentativa APPROVED com quantity 1 emitindo exatamente 1 Ticket")
    void shouldProcessApprovedPaymentWithQuantity1() {
        Reservation reservation = createHoldingReservation(customerId, 1, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationPaymentPort.confirmReservation(eq(reservationId), eq(now))).thenReturn(reservation.confirm(now));
        when(ticketIssuancePort.issueTickets(any())).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        useCase.execute(command);

        ArgumentCaptor<IssueTicketsCommand> ticketCaptor = ArgumentCaptor.forClass(IssueTicketsCommand.class);
        verify(ticketIssuancePort).issueTickets(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("Processa tentativa APPROVED com quantity 6 emitindo exatamente 6 Tickets")
    void shouldProcessApprovedPaymentWithQuantity6() {
        Reservation reservation = createHoldingReservation(customerId, 6, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationPaymentPort.confirmReservation(eq(reservationId), eq(now))).thenReturn(reservation.confirm(now));
        when(ticketIssuancePort.issueTickets(any())).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        useCase.execute(command);

        ArgumentCaptor<IssueTicketsCommand> ticketCaptor = ArgumentCaptor.forClass(IssueTicketsCommand.class);
        verify(ticketIssuancePort).issueTickets(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().quantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("Replay idempotente com mesmo paymentAttemptId e mesmo fingerprint retorna exatamente o Payment persistido sem novo processamento")
    void shouldReturnPersistedPaymentOnIdempotentReplay() {
        String expectedFingerprint = ProcessPaymentAttemptUseCase.calculateFingerprint(
                customerId,
                reservationId,
                PaymentSimulatedOutcome.APPROVED
        );

        Payment existingPayment = new Payment(
                paymentAttemptId,
                reservationId,
                customerId,
                new BigDecimal("300.00"),
                "BRL",
                PaymentStatus.APPROVED,
                "FAKE",
                null,
                expectedFingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        Payment result = useCase.execute(command);

        verify(customerLockPort).lockCustomer(customerId);
        verify(paymentRepository).findById(paymentAttemptId);
        verify(reservationPaymentPort, never()).findByIdForUpdate(any());
        verify(reservationPaymentPort, never()).confirmReservation(any(), any());
        verify(ticketIssuancePort, never()).issueTickets(any());
        verify(paymentRepository, never()).save(any());

        assertThat(result).isSameAs(existingPayment);
    }

    @Test
    @DisplayName("Reutilização de paymentAttemptId com fingerprint divergente lança IdempotencyConflictException")
    void shouldThrowConflictWhenReusingPaymentAttemptIdWithDifferentFingerprint() {
        String divergentFingerprint = "v1:otherCustomer:otherReservation:APPROVED";

        Payment existingPayment = new Payment(
                paymentAttemptId,
                reservationId,
                customerId,
                new BigDecimal("300.00"),
                "BRL",
                PaymentStatus.APPROVED,
                "FAKE",
                null,
                divergentFingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
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
        Reservation otherUserReservation = createHoldingReservation(otherCustomerId, 2, now, now.plusSeconds(600));
        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(otherUserReservation));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationOwnershipException.class)
                .hasMessageContaining("pertence a outro usuário");

        verify(paymentRepository, never()).save(any());
        verify(reservationPaymentPort, never()).confirmReservation(any(), any());
        verify(ticketIssuancePort, never()).issueTickets(any());
    }

    @Test
    @DisplayName("Tentativa de pagamento em reserva expirada pelo tempo lança ReservationExpiredException e dispara expiração")
    void shouldThrowAndExpireWhenReservationTimeIsExpired() {
        Instant expiredAt = now.minusSeconds(1);
        Reservation expiredReservation = createHoldingReservation(customerId, 2, now.minusSeconds(601), expiredAt);

        when(paymentRepository.findById(paymentAttemptId)).thenReturn(Optional.empty());
        when(reservationPaymentPort.findByIdForUpdate(reservationId)).thenReturn(Optional.of(expiredReservation));

        ProcessPaymentAttemptCommand command = new ProcessPaymentAttemptCommand(
                customerId,
                reservationId,
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationExpiredException.class)
                .hasMessageContaining("expirou");

        verify(reservationPaymentPort).expireReservation(reservationId);
        verify(reservationPaymentPort, never()).confirmReservation(any(), any());
        verify(ticketIssuancePort, never()).issueTickets(any());
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
                PaymentSimulatedOutcome.APPROVED
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ReservationAlreadyConfirmedException.class)
                .hasMessageContaining("já foi confirmada");

        verify(paymentRepository, never()).save(any());
        verify(reservationPaymentPort, never()).confirmReservation(any(), any());
        verify(ticketIssuancePort, never()).issueTickets(any());
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
                PaymentSimulatedOutcome.APPROVED
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
