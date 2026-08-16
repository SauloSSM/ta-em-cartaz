package br.com.elitedevticket.reservations.domain;

public class SalesClosedException extends ReservationDomainException {
    public SalesClosedException(String message) {
        super("SALES_CLOSED", message);
    }
}
