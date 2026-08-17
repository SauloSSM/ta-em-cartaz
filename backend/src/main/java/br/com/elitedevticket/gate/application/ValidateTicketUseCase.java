package br.com.elitedevticket.gate.application;

import br.com.elitedevticket.gate.domain.GateAttemptConflictException;
import br.com.elitedevticket.gate.domain.GateOwnershipException;
import br.com.elitedevticket.gate.domain.GateValidationResult;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import br.com.elitedevticket.gate.domain.ValidationMethod;
import br.com.elitedevticket.tickets.application.TicketValidationPort;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidateTicketUseCase {

    private final ValidationAttemptRepository validationAttemptRepository;
    private final TicketValidationPort ticketValidationPort;
    private final Clock clock;

    public ValidateTicketUseCase(
            ValidationAttemptRepository validationAttemptRepository,
            TicketValidationPort ticketValidationPort,
            Clock clock
    ) {
        this.validationAttemptRepository = Objects.requireNonNull(validationAttemptRepository, "validationAttemptRepository must not be null");
        this.ticketValidationPort = Objects.requireNonNull(ticketValidationPort, "ticketValidationPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ValidationAttempt execute(ValidateTicketCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String fingerprint = calculateFingerprint(
                command.gateUserId(),
                command.selectedEventId(),
                command.method(),
                command.manualCode()
        );

        // 1. Check existing attempt (idempotent replay)
        Optional<ValidationAttempt> existingAttemptOpt = validationAttemptRepository.findById(command.validationAttemptId());
        if (existingAttemptOpt.isPresent()) {
            ValidationAttempt existing = existingAttemptOpt.get();
            if (!existing.gateUserId().equals(command.gateUserId())) {
                throw new GateOwnershipException("A tentativa de validacao pertence a outro operador.");
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new GateAttemptConflictException("Chave de tentativa de validacao reutilizada com parametros incompativeis.");
            }
            return existing;
        }

        Instant serverNow = clock.instant().truncatedTo(ChronoUnit.MICROS);

        GateValidationResult outcome;
        UUID matchedTicketId = null;

        if (command.method() == ValidationMethod.MANUAL) {
            String rawCode = command.manualCode();
            String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawCode);

            if (normalizedCode == null || normalizedCode.isBlank()) {
                outcome = GateValidationResult.INVALID;
            } else {
                Optional<Ticket> ticketOpt = ticketValidationPort.findByManualCodeForValidation(normalizedCode);
                if (ticketOpt.isEmpty()) {
                    outcome = GateValidationResult.INVALID;
                } else {
                    Ticket ticket = ticketOpt.get();
                    matchedTicketId = ticket.id();

                    if (!ticket.eventId().equals(command.selectedEventId())) {
                        outcome = GateValidationResult.WRONG_EVENT;
                    } else if (ticket.status() == TicketStatus.USED) {
                        outcome = GateValidationResult.ALREADY_USED;
                    } else if (ticket.status() == TicketStatus.VALID) {
                        outcome = GateValidationResult.VALID;
                        ticketValidationPort.markTicketAsUsed(ticket.id(), serverNow, command.gateUserId());
                    } else {
                        outcome = GateValidationResult.INVALID;
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("Metodo de validacao ainda nao suportado: " + command.method());
        }

        ValidationAttempt attempt = new ValidationAttempt(
                command.validationAttemptId(),
                command.gateUserId(),
                command.selectedEventId(),
                matchedTicketId,
                command.method(),
                outcome,
                fingerprint,
                serverNow,
                serverNow
        );

        return validationAttemptRepository.save(attempt);
    }

    public static String calculateFingerprint(
            UUID gateUserId,
            UUID selectedEventId,
            ValidationMethod method,
            String credential
    ) {
        Objects.requireNonNull(gateUserId, "gateUserId must not be null");
        Objects.requireNonNull(selectedEventId, "selectedEventId must not be null");
        Objects.requireNonNull(method, "method must not be null");

        String normalizedCredential;
        if (method == ValidationMethod.MANUAL) {
            normalizedCredential = TicketCredentialGenerator.normalizeManualCode(credential);
            if (normalizedCredential == null) {
                normalizedCredential = "";
            }
        } else {
            normalizedCredential = credential != null ? credential : "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] credDigest = digest.digest(normalizedCredential.getBytes(StandardCharsets.UTF_8));
            String credDigestHex = HexFormat.of().formatHex(credDigest);

            String canonical = "v1:" + gateUserId + ":" + selectedEventId + ":" + method.name() + ":" + credDigestHex;
            byte[] fullDigest = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(fullDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }
}
