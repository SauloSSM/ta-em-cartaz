package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.CreateDraftEventUseCase;
import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateDraftEventUseCaseTest {

    private EventRepository eventRepository;
    private Clock clock;
    private CreateDraftEventUseCase useCase;
    private final Instant now = Instant.parse("2026-08-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new CreateDraftEventUseCase(eventRepository, clock);

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createDraftSavesEventInDraftStatusWithSnapshotData() {
        UUID organizerId = UUID.randomUUID();

        Event created = useCase.createDraft(
                organizerId,
                " Rock In Rio 2026 ",
                " tm-123 ",
                " Descrição do evento ",
                " https://images.example.com/banner.jpg ",
                " Rock "
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.organizerId()).isEqualTo(organizerId);
        assertThat(created.title()).isEqualTo("Rock In Rio 2026");
        assertThat(created.externalSource()).isEqualTo("TICKETMASTER");
        assertThat(created.externalId()).isEqualTo("tm-123");
        assertThat(created.description()).isEqualTo("Descrição do evento");
        assertThat(created.imageUrl()).isEqualTo("https://images.example.com/banner.jpg");
        assertThat(created.category()).isEqualTo("Rock");
        assertThat(created.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(created.venueName()).isNull();
        assertThat(created.venueAddress()).isNull();
        assertThat(created.startsAt()).isNull();
        assertThat(created.createdAt()).isEqualTo(now);
        assertThat(created.updatedAt()).isEqualTo(now);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(created.id());
    }

    @Test
    void createDraftAllowsReusingSameExternalIdWithIndependentIds() {
        UUID organizerId = UUID.randomUUID();

        Event first = useCase.createDraft(organizerId, "Show 1", "tm-same", null, null, null);
        Event second = useCase.createDraft(organizerId, "Show 2", "tm-same", null, null, null);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.externalId()).isEqualTo(second.externalId()).isEqualTo("tm-same");
        assertThat(first.externalSource()).isEqualTo("TICKETMASTER");
    }

    @Test
    void createDraftRejectsBlankTitle() {
        UUID organizerId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.createDraft(organizerId, "   ", "tm-1", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Título do evento é obrigatório");

        assertThatThrownBy(() -> useCase.createDraft(organizerId, null, "tm-1", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Título do evento é obrigatório");
    }
}
