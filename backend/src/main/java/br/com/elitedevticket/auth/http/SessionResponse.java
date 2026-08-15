package br.com.elitedevticket.auth.http;

public sealed interface SessionResponse permits AnonymousSessionResponse, AuthenticatedSessionResponse {
    boolean authenticated();
}
