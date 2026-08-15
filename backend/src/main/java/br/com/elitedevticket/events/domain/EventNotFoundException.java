package br.com.elitedevticket.events.domain;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(UUID eventId) {
        super("Evento não encontrado.");
    }
}
