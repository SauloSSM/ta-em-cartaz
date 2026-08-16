package br.com.elitedevticket.events;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.ListMyEventsUseCase;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListMyEventsUseCaseTest {

    private final InMemoryEventRepository repository = new InMemoryEventRepository();
    private final ListMyEventsUseCase useCase = new ListMyEventsUseCase(repository);

    @Test
    void listsEventsOwnedByOrganizer() {
        UUID organizerId = UUID.randomUUID();
        Event event1 = new Event(
                UUID.randomUUID(), organizerId, "TICKETMASTER", "tm-1", "Rock in Rio", null, null, null,
                EventStatus.DRAFT, null, null, null, Instant.now(), Instant.now()
        );
        Event event2 = new Event(
                UUID.randomUUID(), organizerId, "TICKETMASTER", "tm-2", "Lollapalooza", null, null, null,
                EventStatus.PUBLISHED, "Autódromo", "Interlagos", Instant.now().plusSeconds(3600), Instant.now(), Instant.now()
        );
        repository.save(event1);
        repository.save(event2);

        List<Event> result = useCase.execute(organizerId);

        assertThat(result).containsExactlyInAnyOrder(event1, event2);
    }

    @Test
    void rejectsNullOrganizerId() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
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
        public List<Event> findPublished(String titleSearch) {
            return store.values().stream()
                    .filter(e -> e.status() == EventStatus.PUBLISHED)
                    .filter(e -> titleSearch == null || titleSearch.isBlank()
                            || (e.title() != null && e.title().toLowerCase().contains(titleSearch.trim().toLowerCase())))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
