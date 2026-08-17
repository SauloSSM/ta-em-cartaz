package br.com.elitedevticket.gate.application;

import br.com.elitedevticket.gate.domain.ValidationAttempt;
import java.util.Optional;
import java.util.UUID;

public interface ValidationAttemptRepository {

    ValidationAttempt save(ValidationAttempt attempt);

    Optional<ValidationAttempt> findById(UUID id);
}
