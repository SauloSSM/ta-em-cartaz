package br.com.elitedevticket.events.http;

import java.util.List;

public record PublicEventListResponse(List<PublicEventResponse> events) {
}
