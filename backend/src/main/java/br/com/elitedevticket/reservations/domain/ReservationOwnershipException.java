package br.com.elitedevticket.reservations.domain;

public class ReservationOwnershipException extends ReservationDomainException {
    public ReservationOwnershipException(String message) {
        super("AUTH_FORBIDDEN", message);
    }
}
