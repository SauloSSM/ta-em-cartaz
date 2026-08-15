package br.com.elitedevticket.catalog.adapters.ticketmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import br.com.elitedevticket.catalog.domain.CatalogUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class TicketmasterDiscoveryAdapterTest {

    private TicketmasterProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private MutableClock clock;
    private TicketmasterDiscoveryAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new TicketmasterProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("https://app.ticketmaster.com/discovery/v2");
        properties.setMaxRetries(1);
        properties.setTotalBudget(Duration.ofSeconds(5));

        restClientBuilder = RestClient.builder().baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        clock = new MutableClock(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);
        adapter = new TicketmasterDiscoveryAdapter(properties, restClientBuilder.build(), clock);
    }

    @Test
    void searchEventsSuccessWithFullMapping() {
        String responseJson = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "id": "tm-event-1",
                        "name": "Rock in Rio 2026",
                        "info": "O maior festival de música do mundo",
                        "images": [
                          { "url": "https://images.example.com/small.jpg", "ratio": "4_3", "width": 300 },
                          { "url": "https://images.example.com/hero-16-9.jpg", "ratio": "16_9", "width": 1024 },
                          { "url": "https://images.example.com/banner-16-9.jpg", "ratio": "16_9", "width": 640 }
                        ],
                        "classifications": [
                          {
                            "segment": { "id": "KZFzniwnSyZfZ7v7nJ", "name": "Music" },
                            "genre": { "id": "KnvZfZ7vAv6", "name": "Rock" }
                          }
                        ]
                      },
                      {
                        "id": "tm-event-2",
                        "name": "Teatro dos Sonhos",
                        "description": "Uma experiência teatral única",
                        "images": [
                          { "url": "https://images.example.com/poster.jpg", "ratio": "3_2", "width": 800 }
                        ],
                        "classifications": [
                          {
                            "genre": { "name": "Theatre" }
                          }
                        ]
                      }
                    ]
                  },
                  "page": { "size": 2, "totalElements": 2, "totalPages": 1, "number": 0 }
                }
                """;

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("apikey", "test-api-key"))
                .andExpect(queryParam("keyword", "Rock"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CatalogEventReference> results = adapter.searchEvents("Rock");

        server.verify();
        assertThat(results).hasSize(2);

        CatalogEventReference first = results.get(0);
        assertThat(first.externalId()).isEqualTo("tm-event-1");
        assertThat(first.title()).isEqualTo("Rock in Rio 2026");
        assertThat(first.description()).isEqualTo("O maior festival de música do mundo");
        assertThat(first.imageUrl()).isEqualTo("https://images.example.com/hero-16-9.jpg");
        assertThat(first.category()).isEqualTo("Rock");

        CatalogEventReference second = results.get(1);
        assertThat(second.externalId()).isEqualTo("tm-event-2");
        assertThat(second.title()).isEqualTo("Teatro dos Sonhos");
        assertThat(second.description()).isEqualTo("Uma experiência teatral única");
        assertThat(second.imageUrl()).isEqualTo("https://images.example.com/poster.jpg");
        assertThat(second.category()).isEqualTo("Theatre");
    }

    @Test
    void searchEventsEmptyResponseReturnsEmptyList() {
        String responseJson = """
                {
                  "page": { "size": 20, "totalElements": 0, "totalPages": 0, "number": 0 }
                }
                """;

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CatalogEventReference> results = adapter.searchEvents("NadaEncontrado");

        server.verify();
        assertThat(results).isEmpty();
    }

    @Test
    void searchEventsHandlesMissingOptionalFieldsGracefully() {
        String responseJson = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "id": "tm-simple-1",
                        "name": "Show Acústico",
                        "images": [],
                        "classifications": []
                      }
                    ]
                  }
                }
                """;

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CatalogEventReference> results = adapter.searchEvents(null);

        server.verify();
        assertThat(results).hasSize(1);
        CatalogEventReference ref = results.get(0);
        assertThat(ref.externalId()).isEqualTo("tm-simple-1");
        assertThat(ref.title()).isEqualTo("Show Acústico");
        assertThat(ref.description()).isNull();
        assertThat(ref.imageUrl()).isNull();
        assertThat(ref.category()).isNull();
    }

    @Test
    void rateLimit429ThrowsImmediatelyWithoutRetry() {
        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> adapter.searchEvents("Show"))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("rate limit");

        server.verify();
    }

    @Test
    void clientError4xxThrowsImmediatelyWithoutRetry() {
        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> adapter.searchEvents("Invalid"))
                .isInstanceOf(CatalogUnavailableException.class);

        server.verify();
    }

    @Test
    void transientServerErrorRecoversOnSecondAttempt() {
        String successJson = """
                {
                  "_embedded": {
                    "events": [
                      { "id": "tm-retry-1", "name": "Show Recuperado" }
                    ]
                  }
                }
                """;

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));

        List<CatalogEventReference> results = adapter.searchEvents("Recuperar");

        server.verify();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Show Recuperado");
    }

    @Test
    void transientNetworkIoErrorRecoversOnSecondAttempt() {
        String successJson = """
                {
                  "_embedded": {
                    "events": [
                      { "id": "tm-io-1", "name": "Show Recuperado de IO" }
                    ]
                  }
                }
                """;

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection reset by peer");
                });

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successJson, MediaType.APPLICATION_JSON));

        List<CatalogEventReference> results = adapter.searchEvents("RecuperarIO");

        server.verify();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Show Recuperado de IO");
    }

    @Test
    void persistentServerErrorFailsAfterMaxRetries() {
        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.searchEvents("FalhaPersistente"))
                .isInstanceOf(CatalogUnavailableException.class);

        server.verify();
    }

    @Test
    void budgetExceededStopsBeforeSecondAttempt() {
        server.expect(ExpectedCount.once(), requestTo(Matchers.startsWith("https://app.ticketmaster.com/discovery/v2/events.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    clock.advance(Duration.ofSeconds(6));
                    return withStatus(HttpStatus.INTERNAL_SERVER_ERROR).createResponse(request);
                });

        assertThatThrownBy(() -> adapter.searchEvents("Timeout"))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("orçamento esgotado");

        server.verify();
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initial, ZoneId zone) {
            this.currentInstant = initial;
            this.zone = zone;
        }

        void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(this.currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
