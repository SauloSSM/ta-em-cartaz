package br.com.elitedevticket.gate.adapters.persistence;

import br.com.elitedevticket.gate.application.ValidationAttemptRepository;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaValidationAttemptRepository implements ValidationAttemptRepository {

    private final SpringDataValidationAttemptRepository repository;

    JpaValidationAttemptRepository(SpringDataValidationAttemptRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ValidationAttempt save(ValidationAttempt attempt) {
        ValidationAttemptEntity entity = new ValidationAttemptEntity(attempt);
        ValidationAttemptEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<ValidationAttempt> findById(UUID id) {
        return repository.findById(id).map(ValidationAttemptEntity::toDomain);
    }
}
