package br.com.elitedevticket.reservations;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationsEndpointsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("Cliente autenticado cria hold com revalidação de estoque e vendas abertas")
    void customerCanCreateHoldOnPublishedEventWithAvailableStock() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar evento e setor, e publicar
        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Festival Primavera 2026",
                Instant.now().plus(10, ChronoUnit.DAYS).toString(),
                "Pista VIP",
                50,
                "150.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // 2. Customer cria hold de 3 ingressos
        HttpResponse<String> response = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":3}"
        );

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"quantity\":3");
        assertThat(response.body()).contains("\"unitPrice\":150.00");
        assertThat(response.body()).contains("\"totalAmount\":450.00");
        assertThat(response.body()).contains("\"status\":\"HOLDING\"");
        assertThat(response.body()).contains("\"expiresAt\":");
        assertThat(response.body()).contains("\"serverNow\":");

        // 3. Verificar que availableQuantity foi decrementado para 47
        Integer availableQuantity = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                UUID.fromString(sectorId)
        );
        assertThat(availableQuantity).isEqualTo(47);
    }

    @Test
    @DisplayName("Requisição anônima para criar hold retorna 401 Unauthenticated")
    void anonymousRequestToCreateReservationReturns401() throws Exception {
        String csrf = bootstrapCsrf();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        HttpResponse<String> response = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"AUTH_UNAUTHENTICATED\"");
    }

    @Test
    @DisplayName("Organizador ou Portaria não podem criar reservation e recebem 403 Forbidden")
    void organizerOrGateRoleCannotCreateReservationReturns403() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        HttpResponse<String> organizerResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(organizerResponse.statusCode()).isEqualTo(403);
        assertThat(organizerResponse.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");

        HttpResponse<String> gateResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(gateResponse.statusCode()).isEqualTo(403);
        assertThat(gateResponse.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");
    }

    @Test
    @DisplayName("Requisição sem header CSRF retorna 403 Forbidden")
    void requestWithoutCsrfReturns403() throws Exception {
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        HttpResponse<String> response = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession,
                "",
                "{\"quantity\":2}"
        );

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"AUTH_CSRF_INVALID\"");
    }

    @Test
    @DisplayName("Rejeita criação de reservation em evento em status DRAFT com 422 EVENT_NOT_PUBLISHED")
    void cannotCreateReservationOnDraftEventReturns422() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar evento DRAFT
        String eventPayload = """
                {
                  "title": "Evento Rascunho Teste",
                  "category": "Teatro"
                }
                """;
        HttpResponse<String> draftResponse = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                eventPayload
        );
        String eventId = extractJsonField(draftResponse.body(), "id");

        // Adicionar setor
        String sectorPayload = "{\"name\":\"Geral\",\"capacity\":10,\"price\":50.00}";
        HttpResponse<String> sectorResponse = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                sectorPayload
        );
        String sectorId = extractJsonField(sectorResponse.body(), "id");

        // Customer tenta reservar no evento DRAFT
        HttpResponse<String> reserveResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );

        assertThat(reserveResponse.statusCode()).isEqualTo(422);
        assertThat(reserveResponse.body()).contains("\"code\":\"EVENT_NOT_PUBLISHED\"");
    }

    @Test
    @DisplayName("Rejeita criação de reservation com estoque insuficiente com 422 INSUFFICIENT_AVAILABILITY")
    void cannotCreateReservationWithInsufficientStockReturns422() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Pequeno",
                Instant.now().plus(5, ChronoUnit.DAYS).toString(),
                "Mesa Premium",
                2,
                "300.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // Tentativa de reservar 4 ingressos quando há apenas 2 disponíveis
        HttpResponse<String> response = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":4}"
        );

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"code\":\"INSUFFICIENT_AVAILABILITY\"");

        // Estoque permanece 2
        Integer available = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                UUID.fromString(sectorId)
        );
        assertThat(available).isEqualTo(2);
    }

    @Test
    @DisplayName("Concorrência entre requisições de hold não causa overselling e estoque nunca fica negativo")
    void concurrentHoldRequestsDoNotOversell() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customer1Session = loginSession("customer.one@demo.elitedevticket.local");
        String customer2Session = loginSession("customer.two@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // Cria evento com exatamente 2 ingressos disponíveis
        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Disputado",
                Instant.now().plus(3, ChronoUnit.DAYS).toString(),
                "Setor Único",
                2,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<HttpResponse<String>> task1 = () -> post(
                    "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                    "EDT_SESSION=" + customer1Session + "; XSRF-TOKEN=" + csrf,
                    csrf,
                    "{\"quantity\":2}"
            );
            Callable<HttpResponse<String>> task2 = () -> post(
                    "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                    "EDT_SESSION=" + customer2Session + "; XSRF-TOKEN=" + csrf,
                    csrf,
                    "{\"quantity\":2}"
            );

            List<Future<HttpResponse<String>>> futures = executor.invokeAll(List.of(task1, task2));
            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get());
            }

            long successCount = responses.stream().filter(r -> r.statusCode() == 201).count();
            long failureCount = responses.stream().filter(r -> r.statusCode() == 422).count();

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);

            Integer finalAvailable = jdbcTemplate.queryForObject(
                    "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                    Integer.class,
                    UUID.fromString(sectorId)
            );
            assertThat(finalAvailable).isEqualTo(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Snapshot de preço da Reservation permanece imutável mesmo se o organizador alterar o preço do setor posteriormente")
    void priceSnapshotRemainsImmutableWhenSectorPriceChangesLater() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar e publicar evento com setor a R$ 150.00
        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Festival Rock In Rio 2026",
                Instant.now().plus(15, ChronoUnit.DAYS).toString(),
                "Pista Comum",
                100,
                "150.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // 2. Customer cria hold de 2 ingressos (R$ 300.00)
        HttpResponse<String> reserveResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(reserveResponse.statusCode()).isEqualTo(201);
        String reservationId = extractJsonField(reserveResponse.body(), "id");
        assertThat(reserveResponse.body()).contains("\"unitPrice\":150.00");
        assertThat(reserveResponse.body()).contains("\"totalAmount\":300.00");

        // 3. Organizador altera o preço do setor para R$ 220.00
        String updateSectorPayload = """
                {
                  "name": "Pista Comum",
                  "description": "Setor Comum",
                  "capacity": 100,
                  "price": 220.00
                }
                """;
        HttpResponse<String> updateSectorResponse = put(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                updateSectorPayload
        );
        assertThat(updateSectorResponse.statusCode()).isEqualTo(200);

        // 4. Verificar no banco que a Reservation mantém os valores originais do snapshot
        BigDecimal unitPriceInDb = jdbcTemplate.queryForObject(
                "SELECT unit_price FROM reservations WHERE id = ?",
                BigDecimal.class,
                UUID.fromString(reservationId)
        );
        BigDecimal totalAmountInDb = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM reservations WHERE id = ?",
                BigDecimal.class,
                UUID.fromString(reservationId)
        );
        assertThat(unitPriceInDb).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(totalAmountInDb).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Cliente não pode sobrescrever preço no payload e backend calcula valor autoritativo")
    void clientCannotOverridePriceInPayload() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Concerto Sinfônico",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Plateia Nobre",
                50,
                "180.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // 1. Cliente tenta injetar campos de preço não permitidos -> rejeitado com 400 Bad Request
        String maliciousPayload = "{\"quantity\":2,\"unitPrice\":1.00,\"totalAmount\":2.00}";
        HttpResponse<String> maliciousResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                maliciousPayload
        );
        assertThat(maliciousResponse.statusCode()).isEqualTo(400);

        // 2. Requisição canônica enviando apenas quantity -> calculada exclusivamente pelo backend
        HttpResponse<String> validResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(validResponse.statusCode()).isEqualTo(201);
        assertThat(validResponse.body()).contains("\"unitPrice\":180.00");
        assertThat(validResponse.body()).contains("\"totalAmount\":360.00");
    }

    private String createAndPublishEvent(
            String organizerSession,
            String csrf,
            String title,
            String startsAt,
            String sectorName,
            int capacity,
            String price
    ) throws Exception {
        // 1. Create draft
        String eventPayload = """
                {
                  "externalSource": "TICKETMASTER",
                  "externalId": "tm-%s",
                  "title": "%s",
                  "category": "Música",
                  "description": "Descrição completa do evento"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), title);

        HttpResponse<String> draftRes = post(
                "/api/v1/events/drafts",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                eventPayload
        );
        assertThat(draftRes.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(draftRes.body(), "id");

        // Update draft with venue and startsAt
        String updatePayload = """
                {
                  "title": "%s",
                  "venueName": "Allianz Parque",
                  "venueAddress": "Av. Francisco Matarazzo, 1705",
                  "startsAt": "%s"
                }
                """.formatted(title, startsAt);
        HttpResponse<String> updateRes = put(
                "/api/v1/events/" + eventId,
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                updatePayload
        );
        assertThat(updateRes.statusCode()).isEqualTo(200);

        // 2. Add sector
        String sectorPayload = """
                {
                  "name": "%s",
                  "description": "Setor do evento",
                  "capacity": %d,
                  "price": %s
                }
                """.formatted(sectorName, capacity, price);
        HttpResponse<String> sectorRes = post(
                "/api/v1/events/" + eventId + "/sectors",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                sectorPayload
        );
        assertThat(sectorRes.statusCode()).isEqualTo(201);

        // 3. Publish
        HttpResponse<String> publishRes = post(
                "/api/v1/events/" + eventId + "/publish",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                ""
        );
        assertThat(publishRes.statusCode()).isEqualTo(200);

        return eventId;
    }

    private String getFirstSectorId(String eventId) throws Exception {
        HttpResponse<String> response = get("/api/v1/events/" + eventId + "/sectors", "");
        assertThat(response.statusCode()).isEqualTo(200);
        return extractJsonField(response.body(), "id");
    }

    private String loginSession(String email) throws Exception {
        String csrf = bootstrapCsrf();
        HttpResponse<String> login = post(
                "/api/v1/auth/login",
                "XSRF-TOKEN=" + csrf,
                csrf,
                "{\"email\":\"" + email + "\",\"password\":\"password\"}"
        );
        assertThat(login.statusCode()).isEqualTo(200);
        return cookieValue(login, "EDT_SESSION");
    }

    private String bootstrapCsrf() throws Exception {
        HttpResponse<String> response = get("/api/v1/auth/session", "");
        return cookieValue(response, "XSRF-TOKEN");
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String cookie, String csrf, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (!csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String cookie, String csrf, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (!csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String cookieValue(HttpResponse<String> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .reduce((first, second) -> second)
                .map(cookie -> cookie.substring(name.length() + 1, cookie.indexOf(';')))
                .orElseThrow();
    }

    private String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start != -1) {
            int valueStart = start + marker.length();
            int end = json.indexOf("\"", valueStart);
            return json.substring(valueStart, end);
        }
        marker = "\"" + field + "\":";
        start = json.indexOf(marker);
        if (start != -1) {
            int valueStart = start + marker.length();
            int end = json.indexOf(",", valueStart);
            if (end == -1) end = json.indexOf("}", valueStart);
            return json.substring(valueStart, end).trim();
        }
        throw new IllegalArgumentException("Field " + field + " not found in JSON: " + json);
    }
}
