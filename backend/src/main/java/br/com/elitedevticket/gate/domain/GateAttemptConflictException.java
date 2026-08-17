package br.com.elitedevticket.gate.domain;

public class GateAttemptConflictException extends GateDomainException {

    public GateAttemptConflictException(String message) {
        super(message);
    }
}
