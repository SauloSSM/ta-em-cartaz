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
class PublicTicketEndpointsIntegrationTest {

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

    private UUID customerId;
    private UUID organizerId;
    private UUID gateId;
    private UUID eventId;
    private UUID sectorId;
    private UUID reservationId;

    private Ticket validTicket;
    private Ticket usedTicket;

    private final Instant now = Instant.parse("2026-08-16T12:00:00Z");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM tickets");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM ticket_sectors");
        jdbcTemplate.update("DELETE FROM events");

        customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'customer.one@demo.elitedevticket.local'",
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
                VALUES (?, ?, 'TICKETMASTER', 'tm-public-1', 'Festival de Verão 2026', 'Música', 'Grande festival de música', 'Arena Central', 'Av. Principal, 1000', NOW() + INTERVAL '30 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId, organizerId
        );

        sectorId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Pista Premium', 'Acesso frontal ao palco', 100, 98, 150.00, NOW(), NOW())
                """,
                sectorId, eventId
        );

        reservationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 2, 150.00, 300.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservationId, customerId, eventId, sectorId
        );

        validTicket = Ticket.createValid(
                UUID.randomUUID(), reservationId, eventId, sectorId, customerId, 1,
                credentialGenerator.generateValidationToken(),
                credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(),
                now
        );
        ticketRepository.save(validTicket);

        usedTicket = new Ticket(
                UUID.randomUUID(), reservationId, eventId, sectorId, customerId, 2,
                TicketStatus.USED,
                credentialGenerator.generateValidationToken(),
                credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(),
                now,
                now.plusSeconds(3600),
                gateId
        );
        ticketRepository.save(usedTicket);
    }

    @Test
    @DisplayName("Acesso anônimo com shareToken válido retorna 200, status VALID, credenciais para QR e zero PII")
    void shouldGetPublicTicketAnonymouslyWithValidShareToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + validTicket.shareToken()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();

        // Dados públicos essenciais presentes
        assertThat(body).contains("\"id\":\"" + validTicket.id() + "\"");
        assertThat(body).contains("\"eventId\":\"" + eventId + "\"");
        assertThat(body).contains("\"sectorId\":\"" + sectorId + "\"");
        assertThat(body).contains("\"ordinal\":1");
        assertThat(body).contains("\"status\":\"VALID\"");
        assertThat(body).contains("\"manualCode\":\"" + validTicket.manualCode() + "\"");
        assertThat(body).contains("\"shareToken\":\"" + validTicket.shareToken() + "\"");
        assertThat(body).contains("\"validationToken\":\"" + validTicket.validationToken() + "\"");

        // Ausência de PII e identificadores internos
        assertThat(body).doesNotContain(customerId.toString());
        assertThat(body).doesNotContain("customer.one@demo.elitedevticket.local");
        assertThat(body).doesNotContain(reservationId.toString());
        assertThat(body).doesNotContain("reservationId");
        assertThat(body).doesNotContain("customerId");
    }

    @Test
    @DisplayName("Acesso anônimo com shareToken de ticket USED retorna 200 e status USED preservado")
    void shouldGetPublicTicketWithUsedStatus() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + usedTicket.shareToken()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();

        assertThat(body).contains("\"id\":\"" + usedTicket.id() + "\"");
        assertThat(body).contains("\"status\":\"USED\"");
        assertThat(body).contains("\"ordinal\":2");
        assertThat(body).contains("\"manualCode\":\"" + usedTicket.manualCode() + "\"");
        assertThat(body).contains("\"validationToken\":\"" + usedTicket.validationToken() + "\"");

        // Ausência de PII e dados de gate
        assertThat(body).doesNotContain(customerId.toString());
        assertThat(body).doesNotContain(gateId.toString());
        assertThat(body).doesNotContain(reservationId.toString());
    }

    @Test
    @DisplayName("shareToken inexistente ou inválido retorna 404 seguro com TICKET_NOT_FOUND sem vazar token")
    void shouldReturnSafe404ForInvalidShareToken() throws Exception {
        String invalidToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + invalidToken))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        String body = response.body();
        assertThat(body).contains("\"code\":\"TICKET_NOT_FOUND\"");
        assertThat(body).contains("\"message\":\"Ingresso não encontrado.\"");
        // Não deve ecoar o token da URL na mensagem de erro
        assertThat(body).doesNotContain(invalidToken);
    }

    @Test
    @DisplayName("Estabilidade do link: múltiplas consultas pelo shareToken retornam os mesmos dados sem gerar novo token")
    void shouldMaintainLinkStabilityAcrossMultipleRequests() throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + validTicket.shareToken());

        HttpRequest request1 = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertThat(response1.statusCode()).isEqualTo(200);

        HttpRequest request2 = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertThat(response2.statusCode()).isEqualTo(200);

        assertThat(response1.body()).isEqualTo(response2.body());

        // Verifica que no banco o shareToken permanece inalterado
        Ticket reloaded = ticketRepository.findById(validTicket.id()).orElseThrow();
        assertThat(reloaded.shareToken()).isEqualTo(validTicket.shareToken());
        assertThat(reloaded.validationToken()).isEqualTo(validTicket.validationToken());
    }

    @Test
    @DisplayName("Ownership permanece inalterado com o Customer original após acesso público")
    void shouldPreserveOriginalCustomerOwnership() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + validTicket.shareToken()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Banco de dados preserva o proprietário original
        Ticket persisted = ticketRepository.findById(validTicket.id()).orElseThrow();
        assertThat(persisted.customerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Tentativa de usar ticketId ou validationToken no lugar de shareToken retorna 404 seguro")
    void shouldNotResolveTicketWhenUsingTicketIdOrValidationToken() throws Exception {
        // Tentativa de passar ticketId (UUID) no endpoint público por shareToken
        HttpRequest requestTicketId = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + validTicket.id()))
                .GET()
                .build();

        HttpResponse<String> responseTicketId = client.send(requestTicketId, HttpResponse.BodyHandlers.ofString());
        assertThat(responseTicketId.statusCode()).isEqualTo(404);
        assertThat(responseTicketId.body()).contains("\"code\":\"TICKET_NOT_FOUND\"");

        // Tentativa de passar validationToken no endpoint público por shareToken
        HttpRequest requestValToken = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/public/tickets/" + validTicket.validationToken()))
                .GET()
                .build();

        HttpResponse<String> responseValToken = client.send(requestValToken, HttpResponse.BodyHandlers.ofString());
        assertThat(responseValToken.statusCode()).isEqualTo(404);
        assertThat(responseValToken.body()).contains("\"code\":\"TICKET_NOT_FOUND\"");
    }

    @Test
    @DisplayName("Acesso anônimo a endpoint autenticado /api/v1/my-tickets/{ticketId} retorna 401")
    void shouldRefuseAnonymousAccessToAuthenticatedMyTicketsEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/my-tickets/" + validTicket.id()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
