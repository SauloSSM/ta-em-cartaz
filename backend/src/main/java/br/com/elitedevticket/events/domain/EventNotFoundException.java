package br.com.elitedevticket.events.domain;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(UUID eventId) {
        super("Evento não encontrado: " + eventId);
    }

    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException() {
        super("Evento não encontrado.");
    }
}
