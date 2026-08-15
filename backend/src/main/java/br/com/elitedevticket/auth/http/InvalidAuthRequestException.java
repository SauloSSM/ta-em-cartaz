package br.com.elitedevticket.auth.http;

import java.util.List;

final class InvalidAuthRequestException extends RuntimeException {
    private final List<FieldErrorResponse> fieldErrors;

    InvalidAuthRequestException(List<FieldErrorResponse> fieldErrors) {
        super("Requisição de autenticação inválida.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    List<FieldErrorResponse> fieldErrors() {
        return fieldErrors;
    }
}
