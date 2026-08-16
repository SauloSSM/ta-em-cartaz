package br.com.elitedevticket.reservations.domain;

public class InsufficientAvailabilityException extends ReservationDomainException {
    public InsufficientAvailabilityException(String message) {
        super("INSUFFICIENT_AVAILABILITY", message);
    }
}
