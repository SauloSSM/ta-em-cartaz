package br.com.elitedevticket.events.domain;

public class EventConflictException extends RuntimeException {

    private final String errorCode;

    public EventConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
