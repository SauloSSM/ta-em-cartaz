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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @DisplayName("Emite exatamente N ingressos com ordinais sequenciais 1..N e credenciais únicas e distintas")
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
        Set<String> validationTokens = new HashSet<>();
        Set<String> shareTokens = new HashSet<>();
        Set<String> manualCodes = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            Ticket t = issued.get(i);
            assertThat(t.reservationId()).isEqualTo(reservationId);
            assertThat(t.eventId()).isEqualTo(eventId);
            assertThat(t.sectorId()).isEqualTo(sectorId);
            assertThat(t.customerId()).isEqualTo(customerId);
            assertThat(t.ordinal()).isEqualTo(i + 1);
            assertThat(t.status()).isEqualTo(TicketStatus.VALID);
            assertThat(t.createdAt()).isEqualTo(now);
            assertThat(t.validationToken()).isNotEmpty().hasSize(64);
            assertThat(t.shareToken()).isNotEmpty().hasSize(64);
            assertThat(t.manualCode()).isNotEmpty().hasSize(10);

            // validationToken != shareToken
            assertThat(t.validationToken()).isNotEqualTo(t.shareToken());

            validationTokens.add(t.validationToken());
            shareTokens.add(t.shareToken());
            manualCodes.add(t.manualCode());
        }

        // Cada ingresso recebe credenciais completamente únicas
        assertThat(validationTokens).hasSize(3);
        assertThat(shareTokens).hasSize(3);
        assertThat(manualCodes).hasSize(3);

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("Emite quantidade máxima de 6 ingressos garantindo ordinais 1..6 e credenciais independentes")
    void shouldIssueMaximumSixTicketsWithDistinctCredentials() {
        when(ticketRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        IssueTicketsCommand command = new IssueTicketsCommand(
                reservationId,
                eventId,
                sectorId,
                customerId,
                6,
                now
        );

        List<Ticket> issued = service.issueTickets(command);

        assertThat(issued).hasSize(6);
        for (int i = 0; i < 6; i++) {
            assertThat(issued.get(i).ordinal()).isEqualTo(i + 1);
            assertThat(issued.get(i).validationToken()).isNotEqualTo(issued.get(i).shareToken());
        }
    }

    @Test
    @DisplayName("Reexibição estável: Se ingressos já foram emitidos para a reserva, retorna os existentes sem regenerar tokens nem salvar novamente")
    void shouldReturnExistingTicketsIfAlreadyIssuedWithoutRegenerating() {
        Ticket existingTicket1 = Ticket.createValid(
                UUID.randomUUID(),
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                "token11111111111111111111111111111111111111111111111111111111111111",
                "CODE111111",
                "share11111111111111111111111111111111111111111111111111111111111111",
                now
        );
        Ticket existingTicket2 = Ticket.createValid(
                UUID.randomUUID(),
                reservationId,
                eventId,
                sectorId,
                customerId,
                2,
                "token22222222222222222222222222222222222222222222222222222222222222",
                "CODE222222",
                "share22222222222222222222222222222222222222222222222222222222222222",
                now
        );

        when(ticketRepository.findByReservationId(reservationId)).thenReturn(List.of(existingTicket1, existingTicket2));

        IssueTicketsCommand command = new IssueTicketsCommand(
                reservationId,
                eventId,
                sectorId,
                customerId,
                2,
                now
        );

        List<Ticket> result = service.issueTickets(command);

        assertThat(result).containsExactly(existingTicket1, existingTicket2);
        // Nenhum salvamento ou geração de tokens ocorre
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Credenciais emitidas são independentes e não são derivadas de IDs públicos ou previsíveis")
    void credentialsAreNotDerivedFromPredictableIdentifiers() {
        when(ticketRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        IssueTicketsCommand command = new IssueTicketsCommand(
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                now
        );

        List<Ticket> issued = service.issueTickets(command);
        Ticket ticket = issued.getFirst();

        // Não utiliza ticketId, reservationId, eventId, sectorId, customerId nem ordinal como segredo
        assertThat(ticket.validationToken()).isNotEqualTo(ticket.id().toString());
        assertThat(ticket.validationToken()).isNotEqualTo(reservationId.toString());
        assertThat(ticket.validationToken()).isNotEqualTo(customerId.toString());
        assertThat(ticket.validationToken()).isNotEqualTo(eventId.toString());
        assertThat(ticket.validationToken()).isNotEqualTo(String.valueOf(ticket.ordinal()));

        assertThat(ticket.shareToken()).isNotEqualTo(ticket.id().toString());
        assertThat(ticket.shareToken()).isNotEqualTo(reservationId.toString());
        assertThat(ticket.shareToken()).isNotEqualTo(customerId.toString());
        assertThat(ticket.shareToken()).isNotEqualTo(eventId.toString());
        assertThat(ticket.shareToken()).isNotEqualTo(String.valueOf(ticket.ordinal()));

        assertThat(ticket.manualCode()).isNotEqualTo(ticket.id().toString());
        assertThat(ticket.manualCode()).isNotEqualTo(reservationId.toString());
        assertThat(ticket.manualCode()).isNotEqualTo(String.valueOf(ticket.ordinal()));
    }
}
