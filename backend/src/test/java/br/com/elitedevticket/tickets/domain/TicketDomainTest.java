package br.com.elitedevticket.tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TicketDomainTest {

    private final UUID id = UUID.randomUUID();
    private final UUID reservationId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");
    private final String validationToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private final String manualCode = "AB7K92QX4M";
    private final String shareToken = "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5";

    @Test
    @DisplayName("Cria Ticket válido com status VALID e credenciais completas via factory createValid")
    void shouldCreateValidTicketSuccessfully() {
        Ticket ticket = Ticket.createValid(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                validationToken,
                manualCode,
                shareToken,
                now
        );

        assertThat(ticket.id()).isEqualTo(id);
        assertThat(ticket.reservationId()).isEqualTo(reservationId);
        assertThat(ticket.eventId()).isEqualTo(eventId);
        assertThat(ticket.sectorId()).isEqualTo(sectorId);
        assertThat(ticket.customerId()).isEqualTo(customerId);
        assertThat(ticket.ordinal()).isEqualTo(1);
        assertThat(ticket.status()).isEqualTo(TicketStatus.VALID);
        assertThat(ticket.validationToken()).isEqualTo(validationToken);
        assertThat(ticket.manualCode()).isEqualTo(manualCode);
        assertThat(ticket.shareToken()).isEqualTo(shareToken);
        assertThat(ticket.createdAt()).isEqualTo(now);
        assertThat(ticket.usedAt()).isNull();
        assertThat(ticket.usedByGateUserId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Permite ordinais válidos de 1 até 6")
    void shouldAllowValidOrdinals(int validOrdinal) {
        Ticket ticket = Ticket.createValid(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                validOrdinal,
                validationToken,
                manualCode,
                shareToken,
                now
        );

        assertThat(ticket.ordinal()).isEqualTo(validOrdinal);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 10, 100})
    @DisplayName("Rejeita ordinais fora do limite 1..6")
    void shouldRejectInvalidOrdinals(int invalidOrdinal) {
        assertThatThrownBy(() -> Ticket.createValid(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                invalidOrdinal,
                validationToken,
                manualCode,
                shareToken,
                now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ordinal must be between 1 and 6");
    }

    @Test
    @DisplayName("Rejeita campos nulos obrigatórios na criação do Ticket")
    void shouldRejectNullFields() {
        assertThatThrownBy(() -> new Ticket(null, reservationId, eventId, sectorId, customerId, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("id must not be null");

        assertThatThrownBy(() -> new Ticket(id, null, eventId, sectorId, customerId, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("reservationId must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, null, sectorId, customerId, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("eventId must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, null, customerId, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("sectorId must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, null, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("customerId must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, customerId, 1, null, validationToken, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("status must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, customerId, 1, TicketStatus.VALID, null, manualCode, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("validationToken must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, customerId, 1, TicketStatus.VALID, validationToken, null, shareToken, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("manualCode must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, customerId, 1, TicketStatus.VALID, validationToken, manualCode, null, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("shareToken must not be null");

        assertThatThrownBy(() -> new Ticket(id, reservationId, eventId, sectorId, customerId, 1, TicketStatus.VALID, validationToken, manualCode, shareToken, null, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("createdAt must not be null");
    }

    @Test
    @DisplayName("Rejeita credenciais vazias ou em branco")
    void shouldRejectBlankCredentials() {
        assertThatThrownBy(() -> Ticket.createValid(id, reservationId, eventId, sectorId, customerId, 1, "   ", manualCode, shareToken, now))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("validationToken must not be blank");

        assertThatThrownBy(() -> Ticket.createValid(id, reservationId, eventId, sectorId, customerId, 1, validationToken, "   ", shareToken, now))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("manualCode must not be blank");

        assertThatThrownBy(() -> Ticket.createValid(id, reservationId, eventId, sectorId, customerId, 1, validationToken, manualCode, "   ", now))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("shareToken must not be blank");
    }

    @Test
    @DisplayName("Rejeita Ticket com validationToken idêntico ao shareToken")
    void shouldRejectIdenticalValidationAndShareTokens() {
        String token = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

        assertThatThrownBy(() -> Ticket.createValid(
                id,
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                token,
                manualCode,
                token,
                now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("validationToken and shareToken must not be equal");
    }
}
