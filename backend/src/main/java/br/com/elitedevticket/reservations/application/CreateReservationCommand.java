package br.com.elitedevticket.reservations.application;

import java.util.UUID;

public record CreateReservationCommand(
        UUID customerId,
        UUID eventId,
        UUID sectorId,
        int quantity,
        String idempotencyKey
) {
    public CreateReservationCommand(UUID customerId, UUID eventId, UUID sectorId, int quantity) {
        this(customerId, eventId, sectorId, quantity, null);
    }
}
