package br.com.elitedevticket.auth.http;

import br.com.elitedevticket.auth.application.AuthUser;
import br.com.elitedevticket.auth.domain.Role;
import br.com.elitedevticket.auth.domain.SessionUser;
import java.util.UUID;

public record UserResponse(UUID id, String email, Role role) {
    static UserResponse from(AuthUser user) {
        return new UserResponse(user.id(), user.email(), user.role());
    }

    static UserResponse from(SessionUser user) {
        return new UserResponse(user.id(), user.email(), user.role());
    }
}
