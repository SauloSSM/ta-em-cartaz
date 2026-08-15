package br.com.elitedevticket.auth.application;

public final class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciais inválidas.");
    }
}
