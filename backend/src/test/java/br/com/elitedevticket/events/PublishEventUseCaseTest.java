package br.com.elitedevticket.events;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.PublishEventUseCase;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishEventUseCaseTest {

    private final Instant fixedNow = Instant.parse("2026-08-15T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    private final InMemoryEventRepository eventRepository = new InMemoryEventRepository();
    private final InMemoryTicketSectorRepository sectorRepository = new InMemoryTicketSectorRepository();
    private final PublishEventUseCase useCase = new PublishEventUseCase(eventRepository, sectorRepository, clock);

    @Test
    void publishesDraftEventSuccessfully() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant startsAt = fixedNow.plusSeconds(86400); // 1 day in the future

        Event draft = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-reference-123", "Show ao Vivo", "Descrição detalhada",
                "https://images.example.com/banner.jpg", "Música", EventStatus.DRAFT,
                "Allianz Parque", "Av. Francisco Matarazzo, 1705, São Paulo - SP",
                startsAt, fixedNow.minusSeconds(3600), fixedNow.minusSeconds(3600)
        );
        eventRepository.save(draft);

        TicketSector sector1 = new TicketSector(
                UUID.randomUUID(), eventId, "Pista", "Pista geral", 500, 500,
                new BigDecimal("150.00"), fixedNow.minusSeconds(1800), fixedNow.minusSeconds(1800)
        );
        TicketSector sector2 = new TicketSector(
                UUID.randomUUID(), eventId, "VIP", "Área VIP", 100, 100,
                new BigDecimal("350.00"), fixedNow.minusSeconds(1800), fixedNow.minusSeconds(1800)
        );
        sectorRepository.save(sector1);
        sectorRepository.save(sector2);

        Event published = useCase.execute(eventId, organizerId);

        assertThat(published.id()).isEqualTo(eventId);
        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(published.updatedAt()).isEqualTo(fixedNow);
        assertThat(published.title()).isEqualTo("Show ao Vivo");
        assertThat(published.externalSource()).isEqualTo("TICKETMASTER");
        assertThat(published.externalId()).isEqualTo("tm-reference-123");
        assertThat(published.venueName()).isEqualTo("Allianz Parque");
        assertThat(published.venueAddress()).isEqualTo("Av. Francisco Matarazzo, 1705, São Paulo - SP");
        assertThat(published.startsAt()).isEqualTo(startsAt);

        // Setores permanecem inalterados em sua disponibilidade inicial
        List<TicketSector> sectors = sectorRepository.findByEventId(eventId);
        assertThat(sectors).hasSize(2);
        assertThat(sectors).extracting(TicketSector::availableQuantity)
                .containsExactlyInAnyOrder(500, 100);
    }

    @Test
    void publishesDraftEventWithoutOptionalFields() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant startsAt = fixedNow.plusSeconds(7200);

        Event draft = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-ref-no-opt", "Evento Sem Opcionais", null, null, null,
                EventStatus.DRAFT, "Teatro Municipal", "Praça Ramos de Azevedo, s/n", startsAt, fixedNow, fixedNow
        );
        eventRepository.save(draft);

        TicketSector sector = new TicketSector(
                UUID.randomUUID(), eventId, "Plateia", null, 200, 200,
                new BigDecimal("80.00"), fixedNow, fixedNow
        );
        sectorRepository.save(sector);

        Event published = useCase.execute(eventId, organizerId);

        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(published.description()).isNull();
        assertThat(published.imageUrl()).isNull();
        assertThat(published.category()).isNull();
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
                eventId, otherOrganizerId, "TICKETMASTER", "tm-1", "Outro Evento", null, null, null,
                EventStatus.DRAFT, "Local", "Endereço", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(draft);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(EventForbiddenException.class);
    }

    @Test
    void throwsConflictWhenEventIsAlreadyPublished() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event alreadyPublished = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Evento Já Publicado", null, null, null,
                EventStatus.PUBLISHED, "Local", "Endereço", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(alreadyPublished);

        TicketSector sector = new TicketSector(
                UUID.randomUUID(), eventId, "Pista", null, 100, 100,
                new BigDecimal("50.00"), fixedNow, fixedNow
        );
        sectorRepository.save(sector);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(EventConflictException.class)
                .hasMessageContaining("Apenas eventos em rascunho podem ser publicados.");
    }

    @Test
    void throwsIllegalArgumentWhenEventHasNoSectors() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event draft = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Evento Sem Setores", null, null, null,
                EventStatus.DRAFT, "Local", "Endereço", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(draft);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("setor de ingressos");
    }

    @Test
    void throwsIllegalArgumentWhenStartsAtIsInThePast() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant pastDate = fixedNow.minusSeconds(60);

        Event draft = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Evento Passado", null, null, null,
                EventStatus.DRAFT, "Local", "Endereço", pastDate, fixedNow, fixedNow
        );
        eventRepository.save(draft);

        TicketSector sector = new TicketSector(
                UUID.randomUUID(), eventId, "Pista", null, 100, 100,
                new BigDecimal("50.00"), fixedNow, fixedNow
        );
        sectorRepository.save(sector);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futura");
    }

    @Test
    void throwsIllegalArgumentWhenVenueNameOrAddressIsMissing() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        // Sem venueName
        Event draftNoVenueName = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Evento Sem Nome do Local", null, null, null,
                EventStatus.DRAFT, null, "Endereço", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(draftNoVenueName);

        TicketSector sector = new TicketSector(
                UUID.randomUUID(), eventId, "Pista", null, 100, 100,
                new BigDecimal("50.00"), fixedNow, fixedNow
        );
        sectorRepository.save(sector);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome do local");

        // Sem venueAddress
        Event draftNoVenueAddress = new Event(
                eventId, organizerId, "TICKETMASTER", "tm-1", "Evento Sem Endereço", null, null, null,
                EventStatus.DRAFT, "Local", null, fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(draftNoVenueAddress);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endereço do local");
    }

    @Test
    void throwsIllegalArgumentWhenExternalSourceOrIdIsMissing() {
        UUID organizerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event draftNoExternalSource = new Event(
                eventId, organizerId, null, "tm-1", "Evento Sem Origem", null, null, null,
                EventStatus.DRAFT, "Local", "Endereço", fixedNow.plusSeconds(3600), fixedNow, fixedNow
        );
        eventRepository.save(draftNoExternalSource);

        TicketSector sector = new TicketSector(
                UUID.randomUUID(), eventId, "Pista", null, 100, 100,
                new BigDecimal("50.00"), fixedNow, fixedNow
        );
        sectorRepository.save(sector);

        assertThatThrownBy(() -> useCase.execute(eventId, organizerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origem da referência");
    }

    private static class InMemoryEventRepository implements EventRepository {
        private final Map<UUID, Event> store = new HashMap<>();

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

    private static class InMemoryTicketSectorRepository implements TicketSectorRepository {
        private final Map<UUID, TicketSector> store = new HashMap<>();

        @Override
        public TicketSector save(TicketSector ticketSector) {
            store.put(ticketSector.id(), ticketSector);
            return ticketSector;
        }

        @Override
        public Optional<TicketSector> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<TicketSector> findByIdWithLock(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<TicketSector> findByEventId(UUID eventId) {
            return store.values().stream()
                    .filter(s -> s.eventId().equals(eventId))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
