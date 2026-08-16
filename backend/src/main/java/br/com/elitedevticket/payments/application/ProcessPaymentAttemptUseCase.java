package br.com.elitedevticket.payments.application;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import br.com.elitedevticket.payments.domain.Payment;
import br.com.elitedevticket.payments.domain.PaymentDomainException;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessPaymentAttemptUseCase {

    private final CustomerLockPort customerLockPort;
    private final ReservationPaymentPort reservationPaymentPort;
    private final TicketIssuancePort ticketIssuancePort;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public ProcessPaymentAttemptUseCase(
            CustomerLockPort customerLockPort,
            ReservationPaymentPort reservationPaymentPort,
            TicketIssuancePort ticketIssuancePort,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            Clock clock
    ) {
        this.customerLockPort = Objects.requireNonNull(customerLockPort, "customerLockPort must not be null");
        this.reservationPaymentPort = Objects.requireNonNull(reservationPaymentPort, "reservationPaymentPort must not be null");
        this.ticketIssuancePort = Objects.requireNonNull(ticketIssuancePort, "ticketIssuancePort must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional(noRollbackFor = {ReservationExpiredException.class})
    public Payment execute(ProcessPaymentAttemptCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.customerId(), "customerId must not be null");
        Objects.requireNonNull(command.reservationId(), "reservationId must not be null");
        Objects.requireNonNull(command.paymentAttemptId(), "paymentAttemptId must not be null");
        Objects.requireNonNull(command.simulatedOutcome(), "simulatedOutcome must not be null");

        String fingerprint = calculateFingerprint(
                command.customerId(),
                command.reservationId(),
                command.simulatedOutcome()
        );

        // 1. Lock Customer first to preserve canonical lock ordering (AD-4, AD-5)
        customerLockPort.lockCustomer(command.customerId());

        // 2. Check idempotent claim on paymentAttemptId (AD-9, AD-23)
        Optional<Payment> existingPaymentOpt = paymentRepository.findById(command.paymentAttemptId());
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            if (!existingPayment.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException("Chave de tentativa de pagamento reutilizada com parâmetros incompatíveis.");
            }
            return existingPayment;
        }

        Instant serverNow = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        // 3. Lock & validate Reservation (AD-5, AD-8)
        Reservation reservation = reservationPaymentPort.findByIdForUpdate(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException("Reserva não encontrada."));

        // 4. Validate Reservation Ownership
        if (!reservation.customerId().equals(command.customerId())) {
            throw new ReservationOwnershipException("A reserva informada pertence a outro usuário.");
        }

        // 5. Validate Reservation Status & Expiration (AD-6, AD-8)
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            throw new ReservationAlreadyConfirmedException("A reserva já foi confirmada anteriormente.");
        }

        if (reservation.status() == ReservationStatus.EXPIRED || reservation.isExpired(serverNow)) {
            if (reservation.status() == ReservationStatus.HOLDING) {
                reservationPaymentPort.expireReservation(reservation.id());
            }
            throw new ReservationExpiredException("A reserva expirou e não pode mais ser paga.");
        }

        if (reservation.status() != ReservationStatus.HOLDING) {
            throw new PaymentDomainException("A reserva não está em estado HOLDING para pagamento.");
        }

        // 6. Process via PaymentGateway (FakePaymentGateway)
        PaymentGatewayCommand gatewayCommand = new PaymentGatewayCommand(
                command.paymentAttemptId(),
                reservation.id(),
                reservation.totalAmount(),
                command.simulatedOutcome()
        );
        PaymentGatewayResult gatewayResult = paymentGateway.process(gatewayCommand);

        if (gatewayResult.status() == PaymentStatus.DECLINED) {
            Payment payment = new Payment(
                    command.paymentAttemptId(),
                    reservation.id(),
                    command.customerId(),
                    reservation.totalAmount(),
                    Payment.DEFAULT_CURRENCY,
                    PaymentStatus.DECLINED,
                    Payment.DEFAULT_PROVIDER,
                    gatewayResult.declineReason() != null ? gatewayResult.declineReason() : "SIMULATED_DECLINE",
                    fingerprint,
                    serverNow,
                    gatewayResult.processedAt()
            );

            return paymentRepository.save(payment);
        }

        if (gatewayResult.status() == PaymentStatus.APPROVED) {
            Payment payment = new Payment(
                    command.paymentAttemptId(),
                    reservation.id(),
                    command.customerId(),
                    reservation.totalAmount(),
                    Payment.DEFAULT_CURRENCY,
                    PaymentStatus.APPROVED,
                    Payment.DEFAULT_PROVIDER,
                    null,
                    fingerprint,
                    serverNow,
                    gatewayResult.processedAt()
            );

            // 1. Confirm Reservation (AD-8)
            reservationPaymentPort.confirmReservation(reservation.id(), serverNow);

            // 2. Issue exactly reservation.quantity() Tickets (AD-8, AD-13)
            IssueTicketsCommand issueCommand = new IssueTicketsCommand(
                    reservation.id(),
                    reservation.eventId(),
                    reservation.sectorId(),
                    reservation.customerId(),
                    reservation.quantity(),
                    serverNow
            );
            ticketIssuancePort.issueTickets(issueCommand);

            // 3. Save Payment
            return paymentRepository.save(payment);
        }

        throw new PaymentDomainException("Resultado de gateway não suportado: " + gatewayResult.status());
    }

    public static String calculateFingerprint(UUID customerId, UUID reservationId, PaymentSimulatedOutcome simulatedOutcome) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(simulatedOutcome, "simulatedOutcome must not be null");

        String canonical = "v1:" + customerId + ":" + reservationId + ":" + simulatedOutcome.name();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível no ambiente", e);
        }
    }
}
