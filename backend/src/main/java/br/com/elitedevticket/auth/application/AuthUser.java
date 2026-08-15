package br.com.elitedevticket.auth.application;

import br.com.elitedevticket.auth.domain.Role;
import java.util.UUID;

public record AuthUser(UUID id, String email, Role role, String passwordHash) {
}
