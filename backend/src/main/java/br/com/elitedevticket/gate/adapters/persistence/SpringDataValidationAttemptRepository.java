package br.com.elitedevticket.gate.adapters.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataValidationAttemptRepository extends JpaRepository<ValidationAttemptEntity, UUID> {
}
