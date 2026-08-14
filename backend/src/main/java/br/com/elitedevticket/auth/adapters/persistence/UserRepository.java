package br.com.elitedevticket.auth.adapters.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, UUID> {
}
