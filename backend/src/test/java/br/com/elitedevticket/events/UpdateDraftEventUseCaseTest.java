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
                eventId, organizerId, "TICKETMASTER", "tm-1", "Original Title", "Old desc", null, "Rock",
                EventStatus.DRAFT, null, null, null, Instant.parse("2026-08-15T10:00:00Z"), Instant.parse("2026-08-15T10:00:00Z")
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
                "Av. Francisco Matarazzo, 1705, São Paulo - SP",
                startsAt
        );

        assertThat(updated.title()).isEqualTo("New Title");
        assertThat(updated.description()).isEqualTo("New Description");
        assertThat(updated.imageUrl()).isEqualTo("https://images.example.com/banner.jpg");
        assertThat(updated.category()).isEqualTo("Pop");
        assertThat(updated.venueName()).isEqualTo("Allianz Parque");
        assertThat(updated.venueAddress()).isEqualTo("Av. Francisco Matarazzo, 1705, São Paulo - SP");
        assertThat(updated.startsAt()).isEqualTo(startsAt);
        assertThat(updated.updatedAt()).isEqualTo(fixedNow);
        assertThat(updated.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Title", null, null, null, null, null, null
        )).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void throwsForbiddenWhenEventBelongsToAnotherOrganizer() {
        UUID organizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = new Event(
                eventId, otherOrganizerId, "TICKETMASTER", "tm-1", "Title", null, null, null,
                EventStatus.DRAFT, null, null, null, fixedNow, fixedNow
        );
        repository.save(event);

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "New Title", null, null, null, null, null, null
        )).isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void throwsIllegalArgumentWhenTitleIsBlank() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Title", null, null, null,
                EventStatus.DRAFT, null, null, null, fixedNow, fixedNow
        );
        repository.save(event);

        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "   ", null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsConflictWhenModifyingStructuralFieldsOfPublishedEvent() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant startsAt = fixedNow.plusSeconds(3600);

        Event published = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Original Title", "Old Desc", "https://old.jpg", "Rock",
                EventStatus.PUBLISHED, "Original Venue", "Original Address", startsAt, fixedNow, fixedNow
        );
        repository.save(published);

        // Attempt to change title
        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Updated Title", "Old Desc", "https://old.jpg", "Rock",
                "Original Venue", "Original Address", startsAt
        )).isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Campos estruturais de eventos publicados são imutáveis.");

        // Attempt to change venueName
        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Original Title", "Old Desc", "https://old.jpg", "Rock",
                "New Venue", "Original Address", startsAt
        )).isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Campos estruturais de eventos publicados são imutáveis.");

        // Attempt to change venueAddress
        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Original Title", "Old Desc", "https://old.jpg", "Rock",
                "Original Venue", "New Address", startsAt
        )).isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Campos estruturais de eventos publicados são imutáveis.");

        // Attempt to change startsAt
        assertThatThrownBy(() -> useCase.execute(
                eventId, organizerId, "Original Title", "Old Desc", "https://old.jpg", "Rock",
                "Original Venue", "Original Address", startsAt.plusSeconds(7200)
        )).isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Campos estruturais de eventos publicados são imutáveis.");
    }

    @Test
    void updatesNonStructuralFieldsOfPublishedEventSuccessfully() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant startsAt = fixedNow.plusSeconds(3600);

        Event published = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Original Title", "Old Desc", "https://old.jpg", "Rock",
                EventStatus.PUBLISHED, "Original Venue", "Original Address", startsAt, fixedNow, fixedNow
        );
        repository.save(published);

        Event updated = useCase.execute(
                eventId,
                organizerId,
                "Original Title",
                "New Description with details",
                "https://new.jpg",
                "Pop & Rock",
                "Original Venue",
                "Original Address",
                startsAt
        );

        assertThat(updated.title()).isEqualTo("Original Title");
        assertThat(updated.venueName()).isEqualTo("Original Venue");
        assertThat(updated.venueAddress()).isEqualTo("Original Address");
        assertThat(updated.startsAt()).isEqualTo(startsAt);
        assertThat(updated.externalSource()).isEqualTo("TICKETMASTER");
        assertThat(updated.externalId()).isEqualTo("tm-1");
        assertThat(updated.status()).isEqualTo(EventStatus.PUBLISHED);

        assertThat(updated.description()).isEqualTo("New Description with details");
        assertThat(updated.imageUrl()).isEqualTo("https://new.jpg");
        assertThat(updated.category()).isEqualTo("Pop & Rock");
        assertThat(updated.updatedAt()).isEqualTo(fixedNow);
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
