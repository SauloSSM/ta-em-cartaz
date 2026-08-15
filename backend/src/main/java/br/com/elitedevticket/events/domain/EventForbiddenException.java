package br.com.elitedevticket.events.domain;

public class EventForbiddenException extends RuntimeException {
    public EventForbiddenException(String message) {
        super(message);
    }
}
