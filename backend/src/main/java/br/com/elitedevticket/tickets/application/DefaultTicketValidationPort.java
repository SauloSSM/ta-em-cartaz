package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultTicketValidationPort implements TicketValidationPort {

    private final TicketRepository ticketRepository;

    public DefaultTicketValidationPort(TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository must not be null");
    }

    @Override
    @Transactional
    public Optional<Ticket> findByManualCodeForValidation(String normalizedManualCode) {
        if (normalizedManualCode == null || normalizedManualCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = TicketCredentialGenerator.normalizeManualCode(normalizedManualCode);
        return ticketRepository.findByManualCodeForUpdate(normalized);
    }

    @Override
    @Transactional
    public Optional<Ticket> findByValidationTokenForValidation(String validationToken) {
        if (validationToken == null || validationToken.isBlank()) {
            return Optional.empty();
        }
        return ticketRepository.findByValidationTokenForUpdate(validationToken.trim());
    }

    @Override
    @Transactional
    public Ticket markTicketAsUsed(UUID ticketId, Instant usedAt, UUID gateUserId) {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Objects.requireNonNull(usedAt, "usedAt must not be null");
        Objects.requireNonNull(gateUserId, "gateUserId must not be null");

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalStateException("Ingresso nao encontrado para atualizacao de status: " + ticketId));

        Ticket usedTicket = ticket.markAsUsed(usedAt, gateUserId);
        return ticketRepository.save(usedTicket);
    }
}
