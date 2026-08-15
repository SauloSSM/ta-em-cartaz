package br.com.elitedevticket.catalog.adapters.ticketmaster;

import br.com.elitedevticket.catalog.application.CatalogProvider;
import br.com.elitedevticket.catalog.domain.CatalogEventReference;
import br.com.elitedevticket.catalog.domain.CatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TicketmasterDiscoveryAdapter implements CatalogProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketmasterDiscoveryAdapter.class);

    private final TicketmasterProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketmasterDiscoveryAdapter(
            TicketmasterProperties properties,
            Clock clock) {
        this(properties, createDefaultRestClient(properties), clock);
    }

    TicketmasterDiscoveryAdapter(
            TicketmasterProperties properties,
            RestClient restClient,
            Clock clock) {
        this.properties = properties;
        this.restClient = restClient;
        this.clock = clock;
    }

    private static RestClient createDefaultRestClient(TicketmasterProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<CatalogEventReference> searchEvents(String keyword) {
        Instant start = clock.instant();
        int maxAttempts = 1 + properties.getMaxRetries();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Duration elapsed = Duration.between(start, clock.instant());
            if (elapsed.compareTo(properties.getTotalBudget()) >= 0) {
                LOGGER.warn("Orçamento total de {} ms da Ticketmaster esgotado antes da tentativa {}",
                        properties.getTotalBudget().toMillis(), attempt);
                throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível (orçamento esgotado).");
            }

            try {
                TicketmasterResponse response = restClient.get()
                        .uri(uriBuilder -> {
                            var builder = uriBuilder.path("/events.json")
                                    .queryParam("apikey", properties.getApiKey());
                            if (keyword != null && !keyword.isBlank()) {
                                builder.queryParam("keyword", keyword.trim());
                            }
                            return builder.build();
                        })
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (request, clientResponse) -> {
                            throw new TicketmasterRateLimitException("Ticketmaster rate limit (429)");
                        })
                        .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                            throw new TicketmasterClientErrorException("Ticketmaster client error: " + clientResponse.getStatusCode());
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                            throw new TicketmasterServerErrorException("Ticketmaster server error: " + clientResponse.getStatusCode());
                        })
                        .body(TicketmasterResponse.class);

                return parseEvents(response);
            } catch (TicketmasterRateLimitException e) {
                LOGGER.warn("Ticketmaster respondeu 429 Too Many Requests. Indisponibilidade imediata sem retry.");
                throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível (rate limit).", e);
            } catch (TicketmasterClientErrorException e) {
                LOGGER.error("Ticketmaster respondeu com erro de cliente (4xx). Indisponibilidade imediata sem retry.");
                throw new CatalogUnavailableException("Catálogo Ticketmaster indisponível por erro de requisição.", e);
            } catch (TicketmasterServerErrorException | RestClientException e) {
                LOGGER.warn("Falha transitória na comunicação com Ticketmaster (tentativa {}/{}): {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt >= maxAttempts) {
                    throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível.", e);
                }

                Duration timeTaken = Duration.between(start, clock.instant());
                if (timeTaken.compareTo(properties.getTotalBudget()) >= 0) {
                    throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível (orçamento esgotado).", e);
                }
            } catch (CatalogUnavailableException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Erro inesperado ao consultar Ticketmaster: {}", e.getMessage());
                throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível.", e);
            }
        }

        throw new CatalogUnavailableException("Catálogo Ticketmaster temporariamente indisponível.");
    }

    private List<CatalogEventReference> parseEvents(TicketmasterResponse response) {
        if (response == null || response.embedded() == null || response.embedded().events() == null) {
            return List.of();
        }

        List<CatalogEventReference> references = new ArrayList<>();
        for (TicketmasterResponse.Event event : response.embedded().events()) {
            if (event.id() == null || event.id().isBlank() || event.name() == null || event.name().isBlank()) {
                continue;
            }

            String description = null;
            if (event.info() != null && !event.info().isBlank()) {
                description = event.info().trim();
            } else if (event.description() != null && !event.description().isBlank()) {
                description = event.description().trim();
            }

            String imageUrl = extractImageUrl(event.images());
            String category = extractCategory(event.classifications());

            references.add(new CatalogEventReference(
                    event.id().trim(),
                    event.name().trim(),
                    description,
                    imageUrl,
                    category));
        }

        return List.copyOf(references);
    }

    private String extractImageUrl(List<TicketmasterResponse.Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        // Prefer 16_9 ratio images with highest resolution
        return images.stream()
                .filter(img -> img.url() != null && !img.url().isBlank())
                .sorted(Comparator.comparing((TicketmasterResponse.Image img) -> "16_9".equalsIgnoreCase(img.ratio()))
                        .reversed()
                        .thenComparing(img -> img.width() != null ? img.width() : 0, Comparator.reverseOrder()))
                .map(TicketmasterResponse.Image::url)
                .findFirst()
                .orElse(null);
    }

    private String extractCategory(List<TicketmasterResponse.Classification> classifications) {
        if (classifications == null || classifications.isEmpty()) {
            return null;
        }

        TicketmasterResponse.Classification c0 = classifications.get(0);
        if (c0 == null) {
            return null;
        }

        if (c0.genre() != null && c0.genre().name() != null && !c0.genre().name().isBlank()
                && !"Undefined".equalsIgnoreCase(c0.genre().name().trim())) {
            return c0.genre().name().trim();
        }
        if (c0.segment() != null && c0.segment().name() != null && !c0.segment().name().isBlank()
                && !"Undefined".equalsIgnoreCase(c0.segment().name().trim())) {
            return c0.segment().name().trim();
        }
        if (c0.subGenre() != null && c0.subGenre().name() != null && !c0.subGenre().name().isBlank()
                && !"Undefined".equalsIgnoreCase(c0.subGenre().name().trim())) {
            return c0.subGenre().name().trim();
        }

        return null;
    }

    private static class TicketmasterRateLimitException extends RuntimeException {
        TicketmasterRateLimitException(String message) {
            super(message);
        }
    }

    private static class TicketmasterClientErrorException extends RuntimeException {
        TicketmasterClientErrorException(String message) {
            super(message);
        }
    }

    private static class TicketmasterServerErrorException extends RuntimeException {
        TicketmasterServerErrorException(String message) {
            super(message);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TicketmasterResponse(
            @JsonProperty("_embedded") Embedded embedded,
            Page page) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Embedded(List<Event> events) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Event(
                String id,
                String name,
                String info,
                String description,
                List<Image> images,
                List<Classification> classifications) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Image(
                String url,
                String ratio,
                Integer width,
                Integer height) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Classification(
                Segment segment,
                Genre genre,
                SubGenre subGenre) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            record Segment(String id, String name) {}

            @JsonIgnoreProperties(ignoreUnknown = true)
            record Genre(String id, String name) {}

            @JsonIgnoreProperties(ignoreUnknown = true)
            record SubGenre(String id, String name) {}
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Page(int size, int totalElements, int totalPages, int number) {}
    }
}
