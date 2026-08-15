package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.DeleteTicketSectorUseCase;
import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteTicketSectorUseCaseTest {

    private EventRepository eventRepository;
    private TicketSectorRepository ticketSectorRepository;
    private DeleteTicketSectorUseCase useCase;

    private final UUID organizerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID sectorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        ticketSectorRepository = mock(TicketSectorRepository.class);
        useCase = new DeleteTicketSectorUseCase(eventRepository, ticketSectorRepository);
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
                now.plusSeconds(3600),
                now,
                now
        );
    }

    @Test
    void deletesTicketSectorSuccessfully() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                null,
                100,
                100,
                new BigDecimal("50.00"),
                now,
                now
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        useCase.execute(eventId, sectorId, organizerId);

        verify(ticketSectorRepository).deleteById(sectorId);
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(eventId, sectorId, organizerId))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void throwsForbiddenWhenUserIsNotOwner() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> useCase.execute(eventId, sectorId, otherUserId))
                .isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void throwsConflictWhenEventIsNotDraft() {
        Event publishedEvent = createEvent(EventStatus.PUBLISHED, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> useCase.execute(eventId, sectorId, organizerId))
                .isInstanceOf(EventConflictException.class)
                .hasMessageContaining("rascunho");
    }

    @Test
    void throwsNotFoundWhenSectorDoesNotExist() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(eventId, sectorId, organizerId))
                .isInstanceOf(TicketSectorNotFoundException.class);
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
                new BigDecimal("50.00"),
                now,
                now
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        assertThatThrownBy(() -> useCase.execute(eventId, sectorId, organizerId))
                .isInstanceOf(TicketSectorNotFoundException.class);
    }
}
