package br.com.elitedevticket.reservations.http;

import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID customerId,
        UUID eventId,
        UUID sectorId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        ReservationStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant serverNow
) {
    public static ReservationResponse fromDomain(Reservation reservation, Instant serverNow) {
        return new ReservationResponse(
                reservation.id(),
                reservation.customerId(),
                reservation.eventId(),
                reservation.sectorId(),
                reservation.quantity(),
                reservation.unitPrice(),
                reservation.totalAmount(),
                reservation.status(),
                reservation.expiresAt(),
                reservation.createdAt(),
                serverNow
        );
    }
}
