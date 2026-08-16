package br.com.elitedevticket.reservations.domain;

public class ReservationDomainException extends RuntimeException {
    private final String errorCode;

    public ReservationDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
