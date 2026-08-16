package br.com.elitedevticket.reservations.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record Reservation(
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
        Instant confirmedAt
) {
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 6;
    public static final long HOLD_DURATION_MINUTES = 10;

    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(sectorId, "sectorId must not be null");
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("quantity must be between 1 and 6");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must be greater than or equal to zero");
        }
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalAmount must be greater than or equal to zero");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Reservation createHolding(
            UUID id,
            UUID customerId,
            UUID eventId,
            UUID sectorId,
            int quantity,
            BigDecimal unitPrice,
            Instant serverNow
    ) {
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(serverNow, "serverNow must not be null");
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Instant expiresAt = serverNow.plus(HOLD_DURATION_MINUTES, ChronoUnit.MINUTES);
        return new Reservation(
                id,
                customerId,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                totalAmount,
                ReservationStatus.HOLDING,
                expiresAt,
                serverNow,
                null
        );
    }

    public boolean isExpired(Instant serverNow) {
        return serverNow.isAfter(expiresAt) || serverNow.equals(expiresAt);
    }

    public Reservation expire() {
        if (this.status != ReservationStatus.HOLDING) {
            return this;
        }
        return new Reservation(
                id,
                customerId,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                totalAmount,
                ReservationStatus.EXPIRED,
                expiresAt,
                createdAt,
                confirmedAt
        );
    }

    public Reservation confirm(Instant confirmedAt) {
        if (this.status != ReservationStatus.HOLDING) {
            throw new IllegalStateException("Only HOLDING reservation can be confirmed");
        }
        Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
        return new Reservation(
                id,
                customerId,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                totalAmount,
                ReservationStatus.CONFIRMED,
                expiresAt,
                createdAt,
                confirmedAt
        );
    }
}
