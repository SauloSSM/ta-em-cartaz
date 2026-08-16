package br.com.elitedevticket.tickets.http;

import java.util.List;

public record MyTicketListResponse(List<MyTicketResponse> tickets) {
}
