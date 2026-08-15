package br.com.elitedevticket.auth.application;

import java.util.Optional;

public interface AuthUserLookup {
    Optional<AuthUser> findByEmail(String email);
}
