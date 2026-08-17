package br.com.elitedevticket.gate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.gate.domain.GateAttemptConflictException;
import br.com.elitedevticket.gate.domain.GateOwnershipException;
import br.com.elitedevticket.gate.domain.GateValidationResult;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import br.com.elitedevticket.gate.domain.ValidationMethod;
import br.com.elitedevticket.tickets.application.TicketValidationPort;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidateTicketUseCaseTest {

    private ValidationAttemptRepository validationAttemptRepository;
    private TicketValidationPort ticketValidationPort;
    private Clock clock;
    private ValidateTicketUseCase useCase;

    private final UUID gateUserId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID selectedEventId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-16T21:00:00Z");

    @BeforeEach
    void setUp() {
        validationAttemptRepository = mock(ValidationAttemptRepository.class);
        ticketValidationPort = mock(TicketValidationPort.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new ValidateTicketUseCase(validationAttemptRepository, ticketValidationPort, clock);

        when(validationAttemptRepository.save(any(ValidationAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Ingresso valido do evento selecionado e validado e marcado como USED")
    void validTicketOfSelectedEventTransitionsToUsed() {
        UUID ticketId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        String rawManualCode = "ABCD-1234-EF";
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawManualCode);

        Ticket ticket = Ticket.createValid(
                ticketId,
                reservationId,
                selectedEventId,
                sectorId,
                customerId,
                1,
                "validationToken123",
                normalizedCode,
                "shareToken123",
                now.minusSeconds(3600)
        );

        when(ticketValidationPort.findByManualCodeForValidation(normalizedCode))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.VALID);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);
        assertThat(attempt.gateUserId()).isEqualTo(gateUserId);
        assertThat(attempt.selectedEventId()).isEqualTo(selectedEventId);

        verify(ticketValidationPort).markTicketAsUsed(eq(ticketId), eq(now), eq(gateUserId));
        verify(validationAttemptRepository).save(any(ValidationAttempt.class));
    }

    @Test
    @DisplayName("Ingresso pertencente a outro evento retorna WRONG_EVENT e nao altera estado")
    void ticketOfDifferentEventReturnsWrongEventWithoutMutation() {
        UUID ticketId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();
        String rawManualCode = "XYZ9-9876-00";
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawManualCode);

        Ticket ticket = Ticket.createValid(
                ticketId,
                UUID.randomUUID(),
                otherEventId,
                UUID.randomUUID(),
                customerId,
                1,
                "validationTokenOther",
                normalizedCode,
                "shareTokenOther",
                now.minusSeconds(3600)
        );

        when(ticketValidationPort.findByManualCodeForValidation(normalizedCode))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.WRONG_EVENT);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("Ingresso ja utilizado retorna ALREADY_USED e nao altera estado")
    void ticketAlreadyUsedReturnsAlreadyUsed() {
        UUID ticketId = UUID.randomUUID();
        String rawManualCode = "USED-1234-00";
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawManualCode);

        Ticket ticket = new Ticket(
                ticketId,
                UUID.randomUUID(),
                selectedEventId,
                UUID.randomUUID(),
                customerId,
                1,
                TicketStatus.USED,
                "validationTokenUsed",
                normalizedCode,
                "shareTokenUsed",
                now.minusSeconds(7200),
                now.minusSeconds(1800),
                gateUserId
        );

        when(ticketValidationPort.findByManualCodeForValidation(normalizedCode))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.ALREADY_USED);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("Codigo inexistente retorna INVALID")
    void nonExistentCodeReturnsInvalid() {
        String rawManualCode = "NONEXISTENT";
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawManualCode);

        when(ticketValidationPort.findByManualCodeForValidation(normalizedCode))
                .thenReturn(Optional.empty());

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.INVALID);
        assertThat(attempt.ticketId()).isNull();

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("Tentativa repetida com mesmo ID e mesmo fingerprint e idempotente")
    void idempotentReplayReturnsExistingAttempt() {
        UUID attemptId = UUID.randomUUID();
        String rawManualCode = "ABCD-1234-EF";
        String fingerprint = ValidateTicketUseCase.calculateFingerprint(
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt existing = new ValidationAttempt(
                attemptId,
                gateUserId,
                selectedEventId,
                UUID.randomUUID(),
                ValidationMethod.MANUAL,
                GateValidationResult.VALID,
                fingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(validationAttemptRepository.findById(attemptId))
                .thenReturn(Optional.of(existing));

        ValidateTicketCommand command = new ValidateTicketCommand(
                attemptId,
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawManualCode
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt).isSameAs(existing);
        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("Tentativa repetida com parametros conflitantes lanca GateAttemptConflictException")
    void replayWithConflictingFingerprintThrowsConflict() {
        UUID attemptId = UUID.randomUUID();
        String fingerprint = "different_fingerprint_hash";

        ValidationAttempt existing = new ValidationAttempt(
                attemptId,
                gateUserId,
                selectedEventId,
                UUID.randomUUID(),
                ValidationMethod.MANUAL,
                GateValidationResult.VALID,
                fingerprint,
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(validationAttemptRepository.findById(attemptId))
                .thenReturn(Optional.of(existing));

        ValidateTicketCommand command = new ValidateTicketCommand(
                attemptId,
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                "CODE-XYZ"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(GateAttemptConflictException.class);
    }

    @Test
    @DisplayName("Tentativa com ID pertencente a outro operador lanca GateOwnershipException")
    void replayWithDifferentUserThrowsForbidden() {
        UUID attemptId = UUID.randomUUID();
        UUID otherGateUser = UUID.randomUUID();

        ValidationAttempt existing = new ValidationAttempt(
                attemptId,
                otherGateUser,
                selectedEventId,
                UUID.randomUUID(),
                ValidationMethod.MANUAL,
                GateValidationResult.VALID,
                "fingerprint",
                now.minusSeconds(10),
                now.minusSeconds(10)
        );

        when(validationAttemptRepository.findById(attemptId))
                .thenReturn(Optional.of(existing));

        ValidateTicketCommand command = new ValidateTicketCommand(
                attemptId,
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                "CODE-XYZ"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(GateOwnershipException.class);
    }

    @Test
    @DisplayName("QR: Ingresso valido do evento selecionado e validado e marcado como USED")
    void qrValidTicketOfSelectedEventTransitionsToUsed() {
        UUID ticketId = UUID.randomUUID();
        String validationToken = "a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0";

        Ticket ticket = Ticket.createValid(
                ticketId,
                UUID.randomUUID(),
                selectedEventId,
                UUID.randomUUID(),
                customerId,
                1,
                validationToken,
                "ABCD-1234-EF",
                "shareToken123",
                now.minusSeconds(3600)
        );

        when(ticketValidationPort.findByValidationTokenForValidation(validationToken))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.QR,
                validationToken
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.VALID);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);
        assertThat(attempt.validationMethod()).isEqualTo(ValidationMethod.QR);

        verify(ticketValidationPort).markTicketAsUsed(eq(ticketId), eq(now), eq(gateUserId));
        verify(ticketValidationPort, never()).findByManualCodeForValidation(any());
    }

    @Test
    @DisplayName("QR: Ingresso de outro evento retorna WRONG_EVENT sem consumir")
    void qrTicketOfDifferentEventReturnsWrongEventWithoutMutation() {
        UUID ticketId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();
        String validationToken = "tokenOtherEvent123";

        Ticket ticket = Ticket.createValid(
                ticketId,
                UUID.randomUUID(),
                otherEventId,
                UUID.randomUUID(),
                customerId,
                1,
                validationToken,
                "ABCD-1234-EF",
                "shareTokenOther",
                now.minusSeconds(3600)
        );

        when(ticketValidationPort.findByValidationTokenForValidation(validationToken))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.QR,
                validationToken
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.WRONG_EVENT);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("QR: Ingresso ja utilizado retorna ALREADY_USED")
    void qrTicketAlreadyUsedReturnsAlreadyUsed() {
        UUID ticketId = UUID.randomUUID();
        String validationToken = "tokenUsed123";

        Ticket ticket = new Ticket(
                ticketId,
                UUID.randomUUID(),
                selectedEventId,
                UUID.randomUUID(),
                customerId,
                1,
                TicketStatus.USED,
                validationToken,
                "ABCD-1234-EF",
                "shareTokenUsed",
                now.minusSeconds(7200),
                now.minusSeconds(1800),
                gateUserId
        );

        when(ticketValidationPort.findByValidationTokenForValidation(validationToken))
                .thenReturn(Optional.of(ticket));

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.QR,
                validationToken
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.ALREADY_USED);
        assertThat(attempt.ticketId()).isEqualTo(ticketId);

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("QR: Token inexistente retorna INVALID")
    void qrNonExistentTokenReturnsInvalid() {
        String validationToken = "nonExistentToken";

        when(ticketValidationPort.findByValidationTokenForValidation(validationToken))
                .thenReturn(Optional.empty());

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.QR,
                validationToken
        );

        ValidationAttempt attempt = useCase.execute(command);

        assertThat(attempt.result()).isEqualTo(GateValidationResult.INVALID);
        assertThat(attempt.ticketId()).isNull();

        verify(ticketValidationPort, never()).markTicketAsUsed(any(), any(), any());
    }

    @Test
    @DisplayName("Isolamento de credencial: QR busca exclusivamente via validationToken e nao normaliza como Crockford")
    void qrResolvesExclusivelyViaValidationTokenWithoutCrockfordNormalization() {
        String rawToken = "0123456789abcdef";

        when(ticketValidationPort.findByValidationTokenForValidation(rawToken))
                .thenReturn(Optional.empty());

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.QR,
                rawToken
        );

        useCase.execute(command);

        verify(ticketValidationPort).findByValidationTokenForValidation(eq(rawToken));
        verify(ticketValidationPort, never()).findByManualCodeForValidation(any());
    }

    @Test
    @DisplayName("Isolamento de credencial: MANUAL busca exclusivamente via manualCode normalizado")
    void manualResolvesExclusivelyViaNormalizedManualCode() {
        String rawCode = "abcd-1234-ef";
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawCode);

        when(ticketValidationPort.findByManualCodeForValidation(normalizedCode))
                .thenReturn(Optional.empty());

        ValidateTicketCommand command = new ValidateTicketCommand(
                UUID.randomUUID(),
                gateUserId,
                selectedEventId,
                ValidationMethod.MANUAL,
                rawCode
        );

        useCase.execute(command);

        verify(ticketValidationPort).findByManualCodeForValidation(eq(normalizedCode));
        verify(ticketValidationPort, never()).findByValidationTokenForValidation(any());
    }
}
