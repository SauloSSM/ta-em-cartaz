package br.com.elitedevticket.tickets.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TicketIssuanceServiceTest {

    private TicketRepository ticketRepository;
    private TicketCredentialGenerator credentialGenerator;
    private TicketIssuanceService service;

    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");
    private final UUID reservationId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        credentialGenerator = new TicketCredentialGenerator();
        service = new TicketIssuanceService(ticketRepository, credentialGenerator);
    }

    @Test
    @DisplayName("Emite exatamente N ingressos com ordinais sequenciais 1..N e credenciais únicas")
    void shouldIssueExactNumberOfTicketsWithOrdinals() {
        when(ticketRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        IssueTicketsCommand command = new IssueTicketsCommand(
                reservationId,
                eventId,
                sectorId,
                customerId,
                3,
                now
        );

        List<Ticket> issued = service.issueTickets(command);

        assertThat(issued).hasSize(3);
        for (int i = 0; i < 3; i++) {
            Ticket t = issued.get(i);
            assertThat(t.reservationId()).isEqualTo(reservationId);
            assertThat(t.eventId()).isEqualTo(eventId);
            assertThat(t.sectorId()).isEqualTo(sectorId);
            assertThat(t.customerId()).isEqualTo(customerId);
            assertThat(t.ordinal()).isEqualTo(i + 1);
            assertThat(t.status()).isEqualTo(TicketStatus.VALID);
            assertThat(t.createdAt()).isEqualTo(now);
            assertThat(t.validationToken()).isNotEmpty();
            assertThat(t.shareToken()).isNotEmpty();
            assertThat(t.manualCode()).isNotEmpty();
        }

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("Se ingressos já foram emitidos para a reserva, retorna os existentes sem duplicar")
    void shouldReturnExistingTicketsIfAlreadyIssued() {
        Ticket existingTicket = Ticket.createValid(
                UUID.randomUUID(),
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                "token1",
                "CODE1",
                "share1",
                now
        );

        when(ticketRepository.findByReservationId(reservationId)).thenReturn(List.of(existingTicket));

        IssueTicketsCommand command = new IssueTicketsCommand(
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                now
        );

        List<Ticket> result = service.issueTickets(command);

        assertThat(result).containsExactly(existingTicket);
        verify(ticketRepository, never()).saveAll(anyList());
    }
}
