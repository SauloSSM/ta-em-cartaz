package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.ListTicketSectorsUseCase;
import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListTicketSectorsUseCaseTest {

    private EventRepository eventRepository;
    private TicketSectorRepository ticketSectorRepository;
    private ListTicketSectorsUseCase useCase;

    private final UUID organizerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        ticketSectorRepository = mock(TicketSectorRepository.class);
        useCase = new ListTicketSectorsUseCase(eventRepository, ticketSectorRepository);
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
    void listsSectorsSuccessfullyForOwnerOrganizerOnDraftEvent() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        TicketSector s1 = new TicketSector(UUID.randomUUID(), eventId, "Pista", null, 100, 100, new BigDecimal("50.00"), now, now);
        TicketSector s2 = new TicketSector(UUID.randomUUID(), eventId, "Camarote", null, 50, 50, new BigDecimal("150.00"), now, now);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.findByEventId(eventId)).thenReturn(List.of(s1, s2));

        List<TicketSector> sectors = useCase.execute(eventId, organizerId, true);

        assertThat(sectors).hasSize(2);
        assertThat(sectors.get(0).name()).isEqualTo("Pista");
        assertThat(sectors.get(1).name()).isEqualTo("Camarote");
    }

    @Test
    void throwsForbiddenWhenOtherUserAccessesDraftEventSectors() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> useCase.execute(eventId, otherUserId, true))
                .isInstanceOf(EventForbiddenException.class);

        assertThatThrownBy(() -> useCase.execute(eventId, otherUserId, false))
                .isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void allowsAuthenticatedUserToListSectorsOfPublishedEvent() {
        Event publishedEvent = createEvent(EventStatus.PUBLISHED, organizerId);
        TicketSector s1 = new TicketSector(UUID.randomUUID(), eventId, "Pista", null, 100, 100, new BigDecimal("50.00"), now, now);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(ticketSectorRepository.findByEventId(eventId)).thenReturn(List.of(s1));

        List<TicketSector> sectors = useCase.execute(eventId, otherUserId, false);

        assertThat(sectors).hasSize(1);
        assertThat(sectors.get(0).name()).isEqualTo("Pista");
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId, true))
                .isInstanceOf(EventNotFoundException.class);
    }
}
