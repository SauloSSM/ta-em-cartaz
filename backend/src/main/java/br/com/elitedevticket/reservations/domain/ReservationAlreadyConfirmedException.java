package br.com.elitedevticket.reservations.domain;

public class ReservationAlreadyConfirmedException extends ReservationDomainException {
    public ReservationAlreadyConfirmedException(String message) {
        super("RESERVATION_ALREADY_CONFIRMED", message);
    }
}
