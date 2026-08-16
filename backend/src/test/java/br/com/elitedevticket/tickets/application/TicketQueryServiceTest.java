package br.com.elitedevticket.tickets.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketNotFoundException;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketQueryServiceTest {

    private TicketRepository ticketRepository;
    private TicketQueryService service;

    private final UUID customer1Id = UUID.randomUUID();
    private final UUID customer2Id = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final UUID reservationId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        service = new TicketQueryService(ticketRepository);
    }

    @Test
    @DisplayName("listOwnTickets retorna exclusivamente os ingressos do customer informado")
    void shouldListOnlyCustomerTickets() {
        Ticket t1 = Ticket.createValid(
                UUID.randomUUID(), reservationId, eventId, sectorId, customer1Id, 1,
                "val-token-1", "MC00000001", "share-token-1", now
        );
        Ticket t2 = Ticket.createValid(
                UUID.randomUUID(), reservationId, eventId, sectorId, customer1Id, 2,
                "val-token-2", "MC00000002", "share-token-2", now
        );

        when(ticketRepository.findByCustomerId(customer1Id)).thenReturn(List.of(t1, t2));

        List<Ticket> result = service.listOwnTickets(customer1Id);
        assertThat(result).containsExactly(t1, t2);
    }

    @Test
    @DisplayName("getOwnTicket retorna o ingresso quando o customerId da sessão é o proprietário")
    void shouldReturnTicketWhenOwnerMatches() {
        UUID ticketId = UUID.randomUUID();
        Ticket t1 = Ticket.createValid(
                ticketId, reservationId, eventId, sectorId, customer1Id, 1,
                "val-token-1", "MC00000001", "share-token-1", now
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t1));

        Ticket result = service.getOwnTicket(customer1Id, ticketId);
        assertThat(result).isEqualTo(t1);
    }

    @Test
    @DisplayName("getOwnTicket lança TicketNotFoundException quando o ingresso não existe")
    void shouldThrowNotFoundWhenTicketDoesNotExist() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnTicket(customer1Id, ticketId))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ingresso não encontrado.");
    }

    @Test
    @DisplayName("getOwnTicket lança TicketNotFoundException (404) quando o customerId não for o proprietário (ownership protection)")
    void shouldThrowNotFoundWhenCustomerIdDoesNotMatchOwner() {
        UUID ticketId = UUID.randomUUID();
        // Ingresso pertence a customer2
        Ticket t2 = Ticket.createValid(
                ticketId, reservationId, eventId, sectorId, customer2Id, 1,
                "val-token-2", "MC00000002", "share-token-2", now
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t2));

        // Customer1 tenta abrir o ingresso de Customer2 -> 404 sem vazar existência
        assertThatThrownBy(() -> service.getOwnTicket(customer1Id, ticketId))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ingresso não encontrado.");
    }

    @Test
    @DisplayName("getTicketByShareToken retorna o ingresso quando o shareToken existe")
    void shouldReturnTicketWhenShareTokenExists() {
        String shareToken = "share-token-valid";
        Ticket t1 = Ticket.createValid(
                UUID.randomUUID(), reservationId, eventId, sectorId, customer1Id, 1,
                "val-token-1", "MC00000001", shareToken, now
        );

        when(ticketRepository.findByShareToken(shareToken)).thenReturn(Optional.of(t1));

        Ticket result = service.getTicketByShareToken(shareToken);
        assertThat(result).isEqualTo(t1);
    }

    @Test
    @DisplayName("getTicketByShareToken lança TicketNotFoundException quando o shareToken não existe")
    void shouldThrowNotFoundWhenShareTokenDoesNotExist() {
        String shareToken = "non-existent-token";
        when(ticketRepository.findByShareToken(shareToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicketByShareToken(shareToken))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ingresso não encontrado.");
    }

    @Test
    @DisplayName("getTicketByShareToken lança TicketNotFoundException quando shareToken é nulo ou vazio")
    void shouldThrowNotFoundWhenShareTokenIsNullOrBlank() {
        assertThatThrownBy(() -> service.getTicketByShareToken(null))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ingresso não encontrado.");

        assertThatThrownBy(() -> service.getTicketByShareToken("   "))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ingresso não encontrado.");
    }

    @Test
    @DisplayName("getTicketByShareToken faz trim do token antes da busca")
    void shouldTrimShareTokenBeforeQuery() {
        String shareToken = "share-token-valid";
        Ticket t1 = Ticket.createValid(
                UUID.randomUUID(), reservationId, eventId, sectorId, customer1Id, 1,
                "val-token-1", "MC00000001", shareToken, now
        );

        when(ticketRepository.findByShareToken(shareToken)).thenReturn(Optional.of(t1));

        Ticket result = service.getTicketByShareToken("  " + shareToken + "  ");
        assertThat(result).isEqualTo(t1);
    }
}
