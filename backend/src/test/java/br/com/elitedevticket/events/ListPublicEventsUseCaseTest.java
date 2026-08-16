package br.com.elitedevticket.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.events.application.EventRepository;
import br.com.elitedevticket.events.application.ListPublicEventsUseCase;
import br.com.elitedevticket.events.application.PublicEventItem;
import br.com.elitedevticket.events.application.TicketSectorRepository;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPublicEventsUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketSectorRepository ticketSectorRepository;

    private Clock clock;
    private ListPublicEventsUseCase useCase;

    private final Instant now = Instant.parse("2026-08-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new ListPublicEventsUseCase(eventRepository, ticketSectorRepository, clock);
    }

    @Test
    void returnsPublishedEventsWithCalculatedStartingPriceAndSalesStatus() {
        UUID eventId1 = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Event event1 = new Event(
                eventId1,
                organizerId,
                "TICKETMASTER",
                "tm-1",
                "Festival Tropical",
                "O festival do ano",
                "https://cdn.example.com/tropical.jpg",
                "Música",
                EventStatus.PUBLISHED,
                "Sambódromo",
                "Av. Paulista, 1000",
                now.plusSeconds(3600), // starts in the future
                now.minusSeconds(86400),
                now.minusSeconds(3600)
        );

        TicketSector sectorPista = new TicketSector(
                UUID.randomUUID(),
                eventId1,
                "Pista",
                "Acesso geral",
                500,
                500,
                new BigDecimal("150.00"),
                now,
                now
        );
        TicketSector sectorVip = new TicketSector(
                UUID.randomUUID(),
                eventId1,
                "VIP",
                "Área VIP",
                100,
                100,
                new BigDecimal("350.00"),
                now,
                now
        );

        when(eventRepository.findPublished(null)).thenReturn(List.of(event1));
        when(ticketSectorRepository.findByEventId(eventId1)).thenReturn(List.of(sectorPista, sectorVip));

        List<PublicEventItem> result = useCase.execute(null);

        assertThat(result).hasSize(1);
        PublicEventItem item = result.getFirst();
        assertThat(item.event().id()).isEqualTo(eventId1);
        assertThat(item.event().title()).isEqualTo("Festival Tropical");
        assertThat(item.event().status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(item.startingPrice()).isEqualByComparingTo("150.00");
        assertThat(item.salesClosed()).isFalse();
    }

    @Test
    void setsSalesClosedTrueWhenEventStartsAtIsPastOrEqualsNow() {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Event pastEvent = new Event(
                eventId,
                organizerId,
                null,
                null,
                "Show Histórico",
                "Show gravado",
                null,
                "Show",
                EventStatus.PUBLISHED,
                "Teatro Municipal",
                "Praça Ramos, s/n",
                now.minusSeconds(60), // started 60 seconds ago
                now.minusSeconds(86400),
                now.minusSeconds(3600)
        );

        TicketSector sector = new TicketSector(
                UUID.randomUUID(),
                eventId,
                "Único",
                null,
                100,
                100,
                new BigDecimal("80.00"),
                now,
                now
        );

        when(eventRepository.findPublished("Show")).thenReturn(List.of(pastEvent));
        when(ticketSectorRepository.findByEventId(eventId)).thenReturn(List.of(sector));

        List<PublicEventItem> result = useCase.execute("Show");

        assertThat(result).hasSize(1);
        PublicEventItem item = result.getFirst();
        assertThat(item.salesClosed()).isTrue();
        assertThat(item.startingPrice()).isEqualByComparingTo("80.00");
        verify(eventRepository).findPublished("Show");
    }

    @Test
    void returnsEmptyListWhenNoPublishedEventsMatch() {
        when(eventRepository.findPublished("Inexistente")).thenReturn(List.of());

        List<PublicEventItem> result = useCase.execute("Inexistente");

        assertThat(result).isEmpty();
    }
}
