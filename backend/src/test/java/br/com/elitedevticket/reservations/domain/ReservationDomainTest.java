package br.com.elitedevticket.reservations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationDomainTest {

    @Test
    @DisplayName("Cria Reservation com status HOLDING e expiração autoritativa de exatamente 10 minutos")
    void shouldCreateHoldingReservationWithTenMinuteExpiration() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        int quantity = 3;
        BigDecimal unitPrice = new BigDecimal("150.00");
        Instant now = Instant.parse("2026-08-16T15:00:00Z");

        Reservation reservation = Reservation.createHolding(
                id,
                customerId,
                eventId,
                sectorId,
                quantity,
                unitPrice,
                now
        );

        assertThat(reservation.id()).isEqualTo(id);
        assertThat(reservation.customerId()).isEqualTo(customerId);
        assertThat(reservation.eventId()).isEqualTo(eventId);
        assertThat(reservation.sectorId()).isEqualTo(sectorId);
        assertThat(reservation.quantity()).isEqualTo(3);
        assertThat(reservation.unitPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(reservation.totalAmount()).isEqualTo(new BigDecimal("450.00"));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.HOLDING);
        assertThat(reservation.createdAt()).isEqualTo(now);
        assertThat(reservation.expiresAt()).isEqualTo(now.plus(10, ChronoUnit.MINUTES));
        assertThat(reservation.confirmedAt()).isNull();

        assertThat(reservation.isExpired(now.plus(9, ChronoUnit.MINUTES))).isFalse();
        assertThat(reservation.isExpired(now.plus(10, ChronoUnit.MINUTES))).isTrue();
        assertThat(reservation.isExpired(now.plus(11, ChronoUnit.MINUTES))).isTrue();
    }

    @Test
    @DisplayName("Rejeita quantidade fora do intervalo 1 a 6")
    void shouldRejectInvalidQuantity() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        BigDecimal unitPrice = new BigDecimal("100.00");
        Instant now = Instant.parse("2026-08-16T15:00:00Z");

        assertThatThrownBy(() -> Reservation.createHolding(id, customerId, eventId, sectorId, 0, unitPrice, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be between 1 and 6");

        assertThatThrownBy(() -> Reservation.createHolding(id, customerId, eventId, sectorId, 7, unitPrice, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be between 1 and 6");
    }

    @Test
    @DisplayName("Rejeita preço unitário negativo")
    void shouldRejectNegativeUnitPrice() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        BigDecimal negativePrice = new BigDecimal("-10.00");
        Instant now = Instant.parse("2026-08-16T15:00:00Z");

        assertThatThrownBy(() -> Reservation.createHolding(id, customerId, eventId, sectorId, 2, negativePrice, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitPrice must be greater than or equal to zero");
    }
}
