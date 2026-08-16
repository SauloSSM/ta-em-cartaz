package br.com.elitedevticket.reservations.domain;

public class ReservationExpiredException extends ReservationDomainException {
    public ReservationExpiredException(String message) {
        super("RESERVATION_EXPIRED", message);
    }
}
