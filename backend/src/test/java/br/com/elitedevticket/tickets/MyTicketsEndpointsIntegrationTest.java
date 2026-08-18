package br.com.elitedevticket.tickets;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.tickets.application.TicketRepository;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
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
class MyTicketsEndpointsIntegrationTest {

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

    private final HttpClient client = HttpClient.newHttpClient();
    private final TicketCredentialGenerator credentialGenerator = new TicketCredentialGenerator();

    private UUID customer1Id;
    private UUID customer2Id;
    private UUID organizerId;
    private UUID gateId;
    private UUID eventId;
    private UUID sectorId;
    private UUID reservation1Id;
    private UUID reservation2Id;

    private Ticket customer1Ticket1;
    private Ticket customer1Ticket2;
    private Ticket customer2Ticket1;

    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM validation_attempts");
        jdbcTemplate.update("DELETE FROM tickets");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM ticket_sectors");
        jdbcTemplate.update("DELETE FROM events");

        customer1Id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'customer.one@demo.elitedevticket.local'",
                UUID.class
        );
        customer2Id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'customer.two@demo.elitedevticket.local'",
                UUID.class
        );
        organizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'organizer@demo.elitedevticket.local'",
                UUID.class
        );
        gateId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'gate@demo.elitedevticket.local'",
                UUID.class
        );

        eventId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'INTERNAL', ?, 'Show Meus Ingressos Test', 'Musica', 'Desc', 'Arena Teste', 'Av Brasil', NOW() + INTERVAL '10 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId,
                organizerId,
                "ext-" + UUID.randomUUID()
        );

        sectorId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Pista', 'Desc', 100, 100, 150.00, NOW(), NOW())
                """,
                sectorId,
                eventId
        );

        reservation1Id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 2, 150.00, 300.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation1Id,
                customer1Id,
                eventId,
                sectorId
        );

        reservation2Id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 150.00, 150.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation2Id,
                customer2Id,
                eventId,
                sectorId
        );

        // Cria ingressos para Customer 1
        customer1Ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );
        customer1Ticket2 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 2,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );

        // Cria ingresso para Customer 2
        customer2Ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation2Id, eventId, sectorId, customer2Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );

        ticketRepository.saveAll(List.of(customer1Ticket1, customer1Ticket2, customer2Ticket1));
    }

    @Test
    @DisplayName("Requisição não autenticada para /api/v1/my-tickets retorna 401 UNAUTHENTICATED")
    void unauthenticatedRequestReturns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/my-tickets"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Papéis ORGANIZER e GATE não possuem acesso a Meus Ingressos (retorna 403 FORBIDDEN)")
    void organizerAndGateRolesAreForbidden() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String gateSession = loginSession("gate@demo.elitedevticket.local");

        // Organizer
        HttpRequest orgReq = HttpRequest.newBuilder(uri("/api/v1/my-tickets"))
                .GET()
                .header("Cookie", "EDT_SESSION=" + organizerSession)
                .build();
        HttpResponse<String> orgResp = client.send(orgReq, HttpResponse.BodyHandlers.ofString());
        assertThat(orgResp.statusCode()).isEqualTo(403);

        // Gate
        HttpRequest gateReq = HttpRequest.newBuilder(uri("/api/v1/my-tickets"))
                .GET()
                .header("Cookie", "EDT_SESSION=" + gateSession)
                .build();
        HttpResponse<String> gateResp = client.send(gateReq, HttpResponse.BodyHandlers.ofString());
        assertThat(gateResp.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Customer autenticado lista exclusivamente seus próprios ingressos")
    void customerListsOnlyOwnTickets() throws Exception {
        String customer1Session = loginSession("customer.one@demo.elitedevticket.local");

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/my-tickets"))
                .GET()
                .header("Cookie", "EDT_SESSION=" + customer1Session)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).contains(customer1Ticket1.id().toString());
        assertThat(body).contains(customer1Ticket2.id().toString());
        assertThat(body).doesNotContain(customer2Ticket1.id().toString());
    }

    @Test
    @DisplayName("Customer autenticado obtém detalhe do seu ingresso com credenciais completas")
    void customerCanGetOwnTicketDetail() throws Exception {
        String customer1Session = loginSession("customer.one@demo.elitedevticket.local");

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/my-tickets/" + customer1Ticket1.id()))
                .GET()
                .header("Cookie", "EDT_SESSION=" + customer1Session)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).contains("\"id\":\"" + customer1Ticket1.id() + "\"");
        assertThat(body).contains("\"validationToken\":\"" + customer1Ticket1.validationToken() + "\"");
        assertThat(body).contains("\"manualCode\":\"" + customer1Ticket1.manualCode() + "\"");
        assertThat(body).contains("\"shareToken\":\"" + customer1Ticket1.shareToken() + "\"");
        assertThat(body).contains("\"status\":\"VALID\"");
        assertThat(body).contains("\"ordinal\":1");
    }

    @Test
    @DisplayName("Tentativa de acesso a ingresso de outro cliente retorna 404 seguro (não revela existência)")
    void customerCannotAccessAnotherCustomerTicket() throws Exception {
        String customer1Session = loginSession("customer.one@demo.elitedevticket.local");

        // Customer1 tenta abrir o ingresso do Customer2
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/my-tickets/" + customer2Ticket1.id()))
                .GET()
                .header("Cookie", "EDT_SESSION=" + customer1Session)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"TICKET_NOT_FOUND\"");
    }

    @Test
    @DisplayName("Ingresso com status USED continua consultável pelo proprietário")
    void usedTicketRemainsConsultableByOwner() throws Exception {
        // Marca ingresso 1 como USED
        jdbcTemplate.update(
                "UPDATE tickets SET status = 'USED', used_at = NOW(), used_by_gate_user_id = ? WHERE id = ?",
                gateId,
                customer1Ticket1.id()
        );

        String customer1Session = loginSession("customer.one@demo.elitedevticket.local");

        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/my-tickets/" + customer1Ticket1.id()))
                .GET()
                .header("Cookie", "EDT_SESSION=" + customer1Session)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"USED\"");
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
