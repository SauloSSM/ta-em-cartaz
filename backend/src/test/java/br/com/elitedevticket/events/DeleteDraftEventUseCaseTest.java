package br.com.elitedevticket.events;

import br.com.elitedevticket.events.application.DeleteDraftEventUseCase;
import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventConflictException;
import br.com.elitedevticket.events.domain.EventForbiddenException;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteDraftEventUseCaseTest {

    private final InMemoryEventRepository repository = new InMemoryEventRepository();
    private final DeleteDraftEventUseCase useCase = new DeleteDraftEventUseCase(repository);

    @Test
    void deletesDraftEventOwnedByOrganizer() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event draft = new Event(
                eventId, organizerId, "tm-1", "Festival", null, null, null,
                EventStatus.DRAFT, null, null, Instant.now(), Instant.now()
        );
        repository.save(draft);

        useCase.execute(eventId, organizerId);

        assertThat(repository.findById(eventId)).isEmpty();
    }

    @Test
    void throwsNotFoundWhenEventDoesNotExist() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void throwsForbiddenWhenEventBelongsToAnotherOrganizer() {
        UUID organizerId = UUID.randomUUID();
        UUID otherOrganizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event draft = new Event(
                eventId, otherOrganizerId, "tm-1", "Festival", null, null, null,
                EventStatus.DRAFT, null, null, Instant.now(), Instant.now()
        );
        repository.save(draft);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(EventForbiddenException.class);
        assertThat(repository.findById(eventId)).isPresent();
    }

    @Test
    void throwsConflictWhenTryingToDeletePublishedEvent() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event published = new Event(
                eventId, organizerId, "tm-1", "Festival", null, null, null,
                EventStatus.PUBLISHED, "Venue", Instant.now().plusSeconds(3600), Instant.now(), Instant.now()
        );
        repository.save(published);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Eventos publicados não podem ser excluídos.");
        assertThat(repository.findById(eventId)).isPresent();
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
