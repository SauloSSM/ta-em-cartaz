package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.http.FieldErrorResponse;
import java.util.List;

public class InvalidEventRequestException extends RuntimeException {
    private final List<FieldErrorResponse> fieldErrors;

    public InvalidEventRequestException(List<FieldErrorResponse> fieldErrors) {
        super("Requisição de evento inválida.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldErrorResponse> fieldErrors() {
        return fieldErrors;
    }
}
