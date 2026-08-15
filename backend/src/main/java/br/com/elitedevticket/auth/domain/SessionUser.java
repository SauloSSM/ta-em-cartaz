package br.com.elitedevticket.auth.domain;

import java.util.UUID;

public record SessionUser(UUID id, String email, Role role) {
}
