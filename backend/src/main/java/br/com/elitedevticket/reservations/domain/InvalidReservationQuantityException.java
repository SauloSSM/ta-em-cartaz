package br.com.elitedevticket.reservations.domain;

public class InvalidReservationQuantityException extends ReservationDomainException {
    public InvalidReservationQuantityException(String message) {
        super("AUTH_INVALID_REQUEST", message);
    }
}
