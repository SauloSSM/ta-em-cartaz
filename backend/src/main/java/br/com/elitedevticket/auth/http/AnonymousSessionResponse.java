package br.com.elitedevticket.auth.http;

public record AnonymousSessionResponse(boolean authenticated) implements SessionResponse {
    public AnonymousSessionResponse() {
        this(false);
    }

    public AnonymousSessionResponse {
        if (authenticated) {
            throw new IllegalArgumentException("Uma sessão anônima não pode estar autenticada.");
        }
    }
}
