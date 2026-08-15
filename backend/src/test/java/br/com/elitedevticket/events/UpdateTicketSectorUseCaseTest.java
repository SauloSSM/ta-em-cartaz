package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.application.UpdateTicketSectorUseCase;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateTicketSectorUseCaseTest {

    private EventRepository eventRepository;
    private TicketSectorRepository ticketSectorRepository;
    private Clock clock;
    private UpdateTicketSectorUseCase useCase;

    private final UUID organizerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-08-15T12:00:00Z");
    private final Instant updatedAt = Instant.parse("2026-08-15T13:00:00Z");

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        ticketSectorRepository = mock(TicketSectorRepository.class);
        clock = Clock.fixed(updatedAt, ZoneOffset.UTC);
        useCase = new UpdateTicketSectorUseCase(eventRepository, ticketSectorRepository, clock);
    }

    private Event createEvent(EventStatus status, UUID owner) {
        return new Event(
                eventId,
                owner,
                "TICKETMASTER",
                "tm-100",
                "Show de Rock",
                null,
                null,
                null,
                status,
                "Local",
                "Endereço",
                createdAt.plusSeconds(3600),
                createdAt,
                createdAt
        );
    }

    @Test
    void updatesTicketSectorSuccessfully() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        TicketSector existingSector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                "Descrição antiga",
                100,
                100,
                new BigDecimal("100.00"),
                createdAt,
                createdAt
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.of(existingSector));
        when(ticketSectorRepository.save(any(TicketSector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSector updated = useCase.execute(
                eventId,
                sectorId,
                organizerId,
                "Pista Premium",
                "Nova descrição",
                250,
                new BigDecimal("180.00")
        );

        assertThat(updated.id()).isEqualTo(sectorId);
        assertThat(updated.eventId()).isEqualTo(eventId);
        assertThat(updated.name()).isEqualTo("Pista Premium");
        assertThat(updated.description()).isEqualTo("Nova descrição");
        assertThat(updated.capacity()).isEqualTo(250);
        assertThat(updated.availableQuantity()).isEqualTo(250);
        assertThat(updated.price()).isEqualTo(new BigDecimal("180.00"));
        assertThat(updated.createdAt()).isEqualTo(createdAt);
        assertThat(updated.updatedAt()).isEqualTo(updatedAt);
        verify(ticketSectorRepository).save(any(TicketSector.class));
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                sectorId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void throwsForbiddenWhenUserIsNotOwner() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                sectorId,
                otherUserId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void throwsConflictWhenEventIsNotDraft() {
        Event publishedEvent = createEvent(EventStatus.PUBLISHED, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                sectorId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventConflictException.class)
          .hasMessageContaining("rascunho");
    }

    @Test
    void throwsNotFoundWhenSectorDoesNotExist() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                sectorId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(TicketSectorNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenSectorBelongsToAnotherEvent() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        UUID differentEventId = UUID.randomUUID();
        TicketSector sector = new TicketSector(
                sectorId,
                differentEventId,
                "Pista",
                null,
                100,
                100,
                BigDecimal.TEN,
                createdAt,
                createdAt
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                sectorId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(TicketSectorNotFoundException.class);
    }
}
