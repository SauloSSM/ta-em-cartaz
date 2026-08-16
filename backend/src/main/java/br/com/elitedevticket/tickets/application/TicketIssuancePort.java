package br.com.elitedevticket.tickets.application;

import br.com.elitedevticket.tickets.domain.Ticket;
import java.util.List;

public interface TicketIssuancePort {
    List<Ticket> issueTickets(IssueTicketsCommand command);
}
