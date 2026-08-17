package br.com.elitedevticket.gate;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.tickets.application.TicketRepository;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class GateEndpointsIntegrationTest {

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

    @Autowired
    private TicketRepository ticketRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HttpClient client = HttpClient.newHttpClient();
    private final TicketCredentialGenerator credentialGenerator = new TicketCredentialGenerator();

    private UUID gateUserId;
    private UUID customerId;
    private UUID organizerId;
    private UUID eventId1;
    private UUID eventId2;
    private UUID sectorId1;
    private UUID sectorId2;
    private UUID reservation1Id;
    private UUID reservation2Id;

    private Ticket ticketEvent1;
    private Ticket ticketEvent2;

    private final Instant now = Instant.parse("2026-08-16T21:00:00Z");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM validation_attempts");
        jdbcTemplate.update("DELETE FROM tickets");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM ticket_sectors");
        jdbcTemplate.update("DELETE FROM events");

        gateUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'gate@demo.elitedevticket.local'",
                UUID.class
        );
        customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'customer.one@demo.elitedevticket.local'",
                UUID.class
        );
        organizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'organizer@demo.elitedevticket.local'",
                UUID.class
        );

        eventId1 = UUID.randomUUID();
        eventId2 = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'SEED', ?, 'Evento Principal', 'SHOW', 'Desc', 'Arena A', 'Rua A', NOW() + INTERVAL '10 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId1, organizerId, UUID.randomUUID().toString()
        );

        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'SEED', ?, 'Outro Evento', 'SHOW', 'Desc', 'Arena B', 'Rua B', NOW() + INTERVAL '12 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId2, organizerId, UUID.randomUUID().toString()
        );

        sectorId1 = UUID.randomUUID();
        sectorId2 = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Pista', 'Desc', 100, 100, 100.00, NOW(), NOW())
                """,
                sectorId1, eventId1
        );

        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'VIP', 'Desc', 50, 50, 200.00, NOW(), NOW())
                """,
                sectorId2, eventId2
        );

        reservation1Id = UUID.randomUUID();
        reservation2Id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 100.00, 100.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation1Id, customerId, eventId1, sectorId1
        );

        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 200.00, 200.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation2Id, customerId, eventId2, sectorId2
        );

        ticketEvent1 = Ticket.createValid(
                UUID.randomUUID(),
                reservation1Id,
                eventId1,
                sectorId1,
                customerId,
                1,
                credentialGenerator.generateValidationToken(),
                credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(),
                now.minusSeconds(1800)
        );
        ticketRepository.save(ticketEvent1);

        ticketEvent2 = Ticket.createValid(
                UUID.randomUUID(),
                reservation2Id,
                eventId2,
                sectorId2,
                customerId,
                1,
                credentialGenerator.generateValidationToken(),
                credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(),
                now.minusSeconds(1800)
        );
        ticketRepository.save(ticketEvent2);
    }

    @Test
    @DisplayName("Validacao manual com sucesso: resultado VALID, ingresso atualizado para USED e tentativa auditada")
    void validateValidManualCodeSuccessfully() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent1.manualCode().toLowerCase());

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("result").asText()).isEqualTo("VALID");
        assertThat(body.get("validationAttemptId").asText()).isEqualTo(attemptId.toString());
        assertThat(body.get("selectedEventId").asText()).isEqualTo(eventId1.toString());
        assertThat(body.get("ticketId").asText()).isEqualTo(ticketEvent1.id().toString());
        assertThat(body.get("method").asText()).isEqualTo("MANUAL");

        // Verify ticket state in DB
        Ticket updatedTicket = ticketRepository.findById(ticketEvent1.id()).orElseThrow();
        assertThat(updatedTicket.status()).isEqualTo(TicketStatus.USED);
        assertThat(updatedTicket.usedAt()).isNotNull();
        assertThat(updatedTicket.usedByGateUserId()).isEqualTo(gateUserId);

        // Verify audit record in DB (without raw secret!)
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ? AND result = 'VALID' AND ticket_id = ?",
                Integer.class,
                attemptId,
                ticketEvent1.id()
        );
        assertThat(attemptCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Ingresso de outro evento retorna WRONG_EVENT e permanece VALID")
    void validateWrongEventReturnsWrongEventAndDoesNotConsumeTicket() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        // Send ticket from event2 against event1
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent2.manualCode());

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("result").asText()).isEqualTo("WRONG_EVENT");
        assertThat(body.get("ticketId").asText()).isEqualTo(ticketEvent2.id().toString());

        // Verify ticket in event2 is NOT mutated
        Ticket ticket2After = ticketRepository.findById(ticketEvent2.id()).orElseThrow();
        assertThat(ticket2After.status()).isEqualTo(TicketStatus.VALID);
        assertThat(ticket2After.usedAt()).isNull();
    }

    @Test
    @DisplayName("Ingresso ja utilizado retorna ALREADY_USED e nao altera dados de consumo")
    void validateAlreadyUsedTicketReturnsAlreadyUsed() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        Instant originalUsedAt = now.minusSeconds(1000);
        Ticket usedTicket = ticketEvent1.markAsUsed(originalUsedAt, gateUserId);
        ticketRepository.save(usedTicket);

        UUID attemptId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent1.manualCode());

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("result").asText()).isEqualTo("ALREADY_USED");

        Ticket ticketAfter = ticketRepository.findById(ticketEvent1.id()).orElseThrow();
        assertThat(ticketAfter.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticketAfter.usedAt()).isEqualTo(originalUsedAt);
    }

    @Test
    @DisplayName("Codigo inexistente retorna INVALID")
    void validateNonExistentCodeReturnsInvalid() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "NON-EXISTENT-CODE",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1);

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("result").asText()).isEqualTo("INVALID");
        assertThat(body.hasNonNull("ticketId")).isFalse();
    }

    @Test
    @DisplayName("Replay com mesmo validationAttemptId e parametros identicos retorna o resultado original sem mutacao duplicada")
    void replayWithSameAttemptIdAndSameFingerprintReturnsOriginalResult() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent1.manualCode());

        HttpRequest request1 = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertThat(response1.statusCode()).isEqualTo(200);
        JsonNode body1 = objectMapper.readTree(response1.body());
        assertThat(body1.get("result").asText()).isEqualTo("VALID");
        String processedAt1 = body1.get("processedAt").asText();

        // Replay
        HttpRequest request2 = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertThat(response2.statusCode()).isEqualTo(200);
        JsonNode body2 = objectMapper.readTree(response2.body());
        assertThat(body2.get("result").asText()).isEqualTo("VALID");
        assertThat(body2.get("processedAt").asText()).isEqualTo(processedAt1);

        // Verify only 1 audit record exists in DB
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ?",
                Integer.class,
                attemptId
        );
        assertThat(attemptCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Replay com mesmo validationAttemptId mas parametros conflitantes retorna 409 Conflict")
    void replayWithSameAttemptIdAndDifferentParametersReturnsConflict() throws Exception {
        String gateSession = loginSession("gate@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        String jsonPayload1 = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent1.manualCode());

        HttpRequest request1 = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload1))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertThat(response1.statusCode()).isEqualTo(200);

        // Attempt reuse with different manual code (different fingerprint)
        String jsonPayload2 = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "OTHER-CODE-1234",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1);

        HttpRequest request2 = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + gateSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload2))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertThat(response2.statusCode()).isEqualTo(409);
        JsonNode errorBody = objectMapper.readTree(response2.body());
        assertThat(errorBody.get("code").asText()).isEqualTo("GATE_ATTEMPT_CONFLICT");

        // Verify DB unchanged (still exactly 1 record for attemptId)
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ?",
                Integer.class,
                attemptId
        );
        assertThat(attemptCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Usuarios sem papel GATE recebem 403 Forbidden")
    void customerCannotValidate() throws Exception {
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        UUID attemptId = UUID.randomUUID();
        String jsonPayload = """
                {
                    "validationAttemptId": "%s",
                    "selectedEventId": "%s",
                    "manualCode": "%s",
                    "method": "MANUAL"
                }
                """.formatted(attemptId, eventId1, ticketEvent1.manualCode());

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/gate/validations"))
                .header("Content-Type", "application/json")
                .header("Cookie", "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private String loginSession(String email) throws Exception {
        String csrf = bootstrapCsrf();
        HttpRequest loginRequest = HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"password\"}"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrf)
                .header("Cookie", "XSRF-TOKEN=" + csrf)
                .build();
        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return cookieValue(response, "EDT_SESSION");
    }

    private String bootstrapCsrf() throws Exception {
        HttpRequest sessionRequest = HttpRequest.newBuilder(uri("/api/v1/auth/session"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(sessionRequest, HttpResponse.BodyHandlers.ofString());
        return cookieValue(response, "XSRF-TOKEN");
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
}
