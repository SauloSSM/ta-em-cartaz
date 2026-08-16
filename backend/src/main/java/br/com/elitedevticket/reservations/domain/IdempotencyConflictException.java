package br.com.elitedevticket.reservations.domain;

public class IdempotencyConflictException extends ReservationDomainException {
    public IdempotencyConflictException(String message) {
        super("IDEMPOTENCY_CONFLICT", message);
    }
}
