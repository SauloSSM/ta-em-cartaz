package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.GetEventUseCase;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetEventUseCaseTest {

    private EventRepository eventRepository;
    private GetEventUseCase useCase;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        useCase = new GetEventUseCase(eventRepository);
    }

    @Test
    void getEventReturnsEventForOwnerOrganizer() {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Event event = new Event(
                eventId,
                organizerId,
                "tm-1",
                "Show",
                "Desc",
                null,
                null,
                EventStatus.DRAFT,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = useCase.getEvent(eventId, organizerId, true);
        assertThat(result).isEqualTo(event);
    }

    @Test
    void getEventThrowsForbiddenWhenDifferentUserAttemptsToAccessDraft() {
        UUID eventId = UUID.randomUUID();
        UUID ownerOrganizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        Event event = new Event(
                eventId,
                ownerOrganizerId,
                "tm-1",
                "Show",
                "Desc",
                null,
                null,
                EventStatus.DRAFT,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> useCase.getEvent(eventId, otherOrganizerId, true))
                .isInstanceOf(EventForbiddenException.class)
                .hasMessageContaining("Acesso negado ao rascunho");

        UUID customerId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.getEvent(eventId, customerId, false))
                .isInstanceOf(EventForbiddenException.class)
                .hasMessageContaining("Acesso negado ao rascunho");
    }

    @Test
    void getEventThrowsNotFoundWhenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getEvent(eventId, UUID.randomUUID(), true))
                .isInstanceOf(EventNotFoundException.class);
    }
}
