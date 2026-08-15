package br.com.elitedevticket.auth.http;

import java.util.Objects;

public record AuthenticatedSessionResponse(boolean authenticated, UserResponse user) implements SessionResponse {
    public AuthenticatedSessionResponse(UserResponse user) {
        this(true, user);
    }

    public AuthenticatedSessionResponse {
        if (!authenticated) {
            throw new IllegalArgumentException("Uma sessão autenticada deve declarar authenticated=true.");
        }
        Objects.requireNonNull(user, "user é obrigatório");
    }
}
