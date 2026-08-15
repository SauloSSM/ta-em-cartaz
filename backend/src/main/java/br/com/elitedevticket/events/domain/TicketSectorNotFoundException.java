package br.com.elitedevticket.events.domain;

public class TicketSectorNotFoundException extends RuntimeException {
    public TicketSectorNotFoundException(String message) {
        super(message);
    }
}
