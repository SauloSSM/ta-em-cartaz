package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.CreateTicketSectorUseCase;
import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTicketSectorUseCaseTest {

    private EventRepository eventRepository;
    private TicketSectorRepository ticketSectorRepository;
    private Clock clock;
    private CreateTicketSectorUseCase useCase;

    private final UUID organizerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        ticketSectorRepository = mock(TicketSectorRepository.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new CreateTicketSectorUseCase(eventRepository, ticketSectorRepository, clock);
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
    void createsTicketSectorSuccessfullyForDraftEvent() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        when(ticketSectorRepository.save(any(TicketSector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSector created = useCase.execute(
                eventId,
                organizerId,
                "Pista",
                "Setor geral",
                100,
                new BigDecimal("150.00")
        );

        assertThat(created.eventId()).isEqualTo(eventId);
        assertThat(created.name()).isEqualTo("Pista");
        assertThat(created.description()).isEqualTo("Setor geral");
        assertThat(created.capacity()).isEqualTo(100);
        assertThat(created.availableQuantity()).isEqualTo(100);
        assertThat(created.price()).isEqualTo(new BigDecimal("150.00"));
        assertThat(created.createdAt()).isEqualTo(now);
        assertThat(created.updatedAt()).isEqualTo(now);
        verify(ticketSectorRepository).save(any(TicketSector.class));
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventNotFoundException.class)
          .hasMessageContaining("Evento não encontrado");
    }

    @Test
    void throwsForbiddenWhenUserIsNotOwner() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                otherUserId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventForbiddenException.class)
          .hasMessageContaining("proprietário");
    }

    @Test
    void throwsConflictWhenEventIsNotDraft() {
        Event publishedEvent = createEvent(EventStatus.PUBLISHED, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> useCase.execute(
                eventId,
                organizerId,
                "Pista",
                null,
                100,
                BigDecimal.TEN
        )).isInstanceOf(EventConflictException.class)
          .hasMessageContaining("rascunho");
    }

    @Test
    void throwsIllegalArgumentWhenParametersAreInvalid() {
        Event draftEvent = createEvent(EventStatus.DRAFT, organizerId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId, "", "desc", 100, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId, "Pista", "desc", 0, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId, "Pista", "desc", -5, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId, "Pista", "desc", 100, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
