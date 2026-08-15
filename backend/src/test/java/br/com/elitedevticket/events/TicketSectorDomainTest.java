package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketSectorDomainTest {

    @Test
    void instantiatesValidTicketSector() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        TicketSector sector = new TicketSector(
                id,
                eventId,
                "Pista Premium",
                "Área em frente ao palco",
                500,
                500,
                new BigDecimal("250.00"),
                now,
                now
        );

        assertThat(sector.id()).isEqualTo(id);
        assertThat(sector.eventId()).isEqualTo(eventId);
        assertThat(sector.name()).isEqualTo("Pista Premium");
        assertThat(sector.description()).isEqualTo("Área em frente ao palco");
        assertThat(sector.capacity()).isEqualTo(500);
        assertThat(sector.availableQuantity()).isEqualTo(500);
        assertThat(sector.price()).isEqualTo(new BigDecimal("250.00"));
        assertThat(sector.createdAt()).isEqualTo(now);
        assertThat(sector.updatedAt()).isEqualTo(now);
    }

    @Test
    void rejectsCapacityZeroOrNegative() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "Pista",
                null,
                0,
                0,
                BigDecimal.ZERO,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("capacity must be greater than zero");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "Pista",
                null,
                -10,
                0,
                BigDecimal.ZERO,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("capacity must be greater than zero");
    }

    @Test
    void rejectsNegativePrice() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "Pista",
                null,
                100,
                100,
                new BigDecimal("-0.01"),
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("price must be greater than or equal to zero");
    }

    @Test
    void acceptsZeroPriceForFreeSector() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        TicketSector sector = new TicketSector(
                id,
                eventId,
                "Entrada Gratuita",
                null,
                100,
                100,
                BigDecimal.ZERO,
                now,
                now
        );

        assertThat(sector.price()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsAvailableQuantityOutOfBounds() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "Pista",
                null,
                100,
                -1,
                BigDecimal.TEN,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("availableQuantity must be between 0 and capacity");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "Pista",
                null,
                100,
                101,
                BigDecimal.TEN,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("availableQuantity must be between 0 and capacity");
    }

    @Test
    void rejectsBlankName() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        assertThatThrownBy(() -> new TicketSector(
                id,
                eventId,
                "   ",
                null,
                100,
                100,
                BigDecimal.TEN,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name must not be blank");
    }

    @Test
    void withUpdatedDraftDetailsReturnsUpdatedRecordWithResetAvailability() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-08-15T12:00:00Z");
        Instant t2 = Instant.parse("2026-08-15T13:00:00Z");

        TicketSector sector = new TicketSector(
                id,
                eventId,
                "Pista",
                "Descrição antiga",
                100,
                100,
                new BigDecimal("50.00"),
                t1,
                t1
        );

        TicketSector updated = sector.withUpdatedDraftDetails(
                "Pista VIP",
                "Nova descrição",
                200,
                new BigDecimal("80.00"),
                t2
        );

        assertThat(updated.id()).isEqualTo(id);
        assertThat(updated.eventId()).isEqualTo(eventId);
        assertThat(updated.name()).isEqualTo("Pista VIP");
        assertThat(updated.description()).isEqualTo("Nova descrição");
        assertThat(updated.capacity()).isEqualTo(200);
        assertThat(updated.availableQuantity()).isEqualTo(200);
        assertThat(updated.price()).isEqualTo(new BigDecimal("80.00"));
        assertThat(updated.createdAt()).isEqualTo(t1);
        assertThat(updated.updatedAt()).isEqualTo(t2);
    }

    @Test
    void calculatesCommittedQuantityCorrectly() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T12:00:00Z");

        TicketSector sector = new TicketSector(
                id, eventId, "Pista", null, 100, 75, new BigDecimal("50.00"), now, now
        );

        assertThat(sector.committedQuantity()).isEqualTo(25);
    }

    @Test
    void withUpdatedDetailsPreservesCommittedQuantityWhenCapacityChanges() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-08-15T12:00:00Z");
        Instant t2 = Instant.parse("2026-08-15T13:00:00Z");

        TicketSector sector = new TicketSector(
                id, eventId, "Pista", "Desc", 100, 75, new BigDecimal("50.00"), t1, t1
        );
        // committed = 100 - 75 = 25

        // Increasing capacity to 150 -> newAvailable = 150 - 25 = 125
        TicketSector expanded = sector.withUpdatedDetails(
                "Pista Expanded", "New Desc", 150, 125, new BigDecimal("60.00"), t2
        );
        assertThat(expanded.capacity()).isEqualTo(150);
        assertThat(expanded.availableQuantity()).isEqualTo(125);
        assertThat(expanded.committedQuantity()).isEqualTo(25);
        assertThat(expanded.price()).isEqualTo(new BigDecimal("60.00"));
        assertThat(expanded.name()).isEqualTo("Pista Expanded");

        // Decreasing capacity to 50 -> newAvailable = 50 - 25 = 25
        TicketSector shrunk = sector.withUpdatedDetails(
                "Pista Shrunk", null, 50, 25, new BigDecimal("50.00"), t2
        );
        assertThat(shrunk.capacity()).isEqualTo(50);
        assertThat(shrunk.availableQuantity()).isEqualTo(25);
        assertThat(shrunk.committedQuantity()).isEqualTo(25);
    }
}
