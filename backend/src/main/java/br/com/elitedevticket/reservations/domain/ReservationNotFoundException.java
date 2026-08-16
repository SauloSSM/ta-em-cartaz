package br.com.elitedevticket.reservations.domain;

public class ReservationNotFoundException extends ReservationDomainException {
    public ReservationNotFoundException(String message) {
        super("RESERVATION_NOT_FOUND", message);
    }
}
