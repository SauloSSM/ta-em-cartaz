package br.com.elitedevticket.events.http;

import java.util.List;

public record TicketSectorListResponse(
        List<TicketSectorResponse> sectors
) {
}
