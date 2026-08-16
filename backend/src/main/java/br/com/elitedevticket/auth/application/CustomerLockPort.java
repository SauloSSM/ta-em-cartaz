package br.com.elitedevticket.auth.application;

import java.util.UUID;

public interface CustomerLockPort {
    void lockCustomer(UUID customerId);
}
