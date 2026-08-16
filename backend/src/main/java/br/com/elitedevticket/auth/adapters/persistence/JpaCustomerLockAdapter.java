package br.com.elitedevticket.auth.adapters.persistence;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaCustomerLockAdapter implements CustomerLockPort {

    private final EntityManager entityManager;

    JpaCustomerLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockCustomer(UUID customerId) {
        UserEntity user = entityManager.find(UserEntity.class, customerId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
    }
}
