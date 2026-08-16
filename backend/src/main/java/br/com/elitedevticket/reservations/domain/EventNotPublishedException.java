package br.com.elitedevticket.reservations.domain;

public class EventNotPublishedException extends ReservationDomainException {
    public EventNotPublishedException(String message) {
        super("EVENT_NOT_PUBLISHED", message);
    }
}
