package br.com.elitedevticket.events;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.UpdateDraftEventUseCase;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateDraftEventUseCaseTest {

    private final Instant fixedNow = Instant.parse("2026-08-15T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    private final InMemoryEventRepository repository = new InMemoryEventRepository();
    private final UpdateDraftEventUseCase useCase = new UpdateDraftEventUseCase(repository, clock);

    @Test
    void updatesDraftEventFieldsSuccessfully() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event initial = new Event(
                eventId, organizerId, "tm-1", "Original Title", "Old desc", null, "Rock",
                EventStatus.DRAFT, null, null, Instant.parse("2026-08-15T10:00:00Z"), Instant.parse("2026-08-15T10:00:00Z")
        );
        repository.save(initial);

        Instant startsAt = Instant.parse("2026-09-01T20:00:00Z");
        Event updated = useCase.execute(
                eventId,
                organizerId,
                "New Title",
                "New Description",
                "https://images.example.com/banner.jpg",
                "Pop",
                "Allianz Parque",
                startsAt
        );

        assertThat(updated.title()).isEqualTo("New Title");
        assertThat(updated.description()).isEqualTo("New Description");
        assertThat(updated.imageUrl()).isEqualTo("https://images.example.com/banner.jpg");
        assertThat(updated.category()).isEqualTo("Pop");
        assertThat(updated.venue()).isEqualTo("Allianz Parque");
        assertThat(updated.startsAt()).isEqualTo(startsAt);
        assertThat(updated.updatedAt()).isEqualTo(fixedNow);
        assertThat(updated.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Title", null, null, null, null, null
        )).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void throwsForbiddenWhenEventBelongsToAnotherOrganizer() {
        UUID organizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = new Event(
                eventId, otherOrganizerId, "tm-1", "Title", null, null, null,
                EventStatus.DRAFT, null, null, fixedNow, fixedNow
        );
        repository.save(event);

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "New Title", null, null, null, null, null
        )).isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void throwsIllegalArgumentWhenTitleIsBlank() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = new Event(
                eventId, organizerId, "tm-1", "Title", null, null, null,
                EventStatus.DRAFT, null, null, fixedNow, fixedNow
        );
        repository.save(event);

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "   ", null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsConflictWhenEventIsPublished() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event published = new Event(
                eventId, organizerId, "tm-1", "Title", null, null, null,
                EventStatus.PUBLISHED, "Venue", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        repository.save(published);

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Updated Title", null, null, null, null, null
        )).isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Apenas eventos em rascunho podem ser modificados.");
    }

    private static class InMemoryEventRepository implements EventRepository {
        private final java.util.Map<UUID, Event> store = new java.util.HashMap<>();

        @Override
        public Event save(Event event) {
            store.put(event.id(), event);
            return event;
        }

        @Override
        public Optional<Event> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Event> findByOrganizerId(UUID organizerId) {
            return store.values().stream()
                    .filter(e -> e.organizerId().equals(organizerId))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
