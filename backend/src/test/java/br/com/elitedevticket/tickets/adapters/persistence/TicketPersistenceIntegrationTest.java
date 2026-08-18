package br.com.elitedevticket.tickets.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.tickets.application.TicketRepository;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class TicketPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TicketCredentialGenerator credentialGenerator = new TicketCredentialGenerator();

    private UUID customer1Id;
    private UUID customer2Id;
    private UUID eventId;
    private UUID sectorId;
    private UUID reservation1Id;
    private UUID reservation2Id;
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
        UUID organizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'organizer@demo.elitedevticket.local'",
                UUID.class
        );

        eventId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'INTERNAL', ?, 'Show Persistencia Tickets', 'Musica', 'Desc', 'Arena Teste', 'Av Brasil', NOW() + INTERVAL '10 days', 'PUBLISHED', NOW(), NOW())
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
    }

    @Test
    @DisplayName("Persiste Tickets e permite reexibição idêntica e estável através de múltiplas consultas")
    void shouldPersistAndReexhibitTicketsConsistently() {
        UUID ticket1Id = UUID.randomUUID();
        String valToken1 = credentialGenerator.generateValidationToken();
        String shareToken1 = credentialGenerator.generateShareToken();
        String manualCode1 = credentialGenerator.generateManualCode();

        Ticket ticket1 = Ticket.createValid(
                ticket1Id, reservation1Id, eventId, sectorId, customer1Id, 1,
                valToken1, manualCode1, shareToken1, now
        );

        UUID ticket2Id = UUID.randomUUID();
        String valToken2 = credentialGenerator.generateValidationToken();
        String shareToken2 = credentialGenerator.generateShareToken();
        String manualCode2 = credentialGenerator.generateManualCode();

        Ticket ticket2 = Ticket.createValid(
                ticket2Id, reservation1Id, eventId, sectorId, customer1Id, 2,
                valToken2, manualCode2, shareToken2, now
        );

        ticketRepository.saveAll(List.of(ticket1, ticket2));

        // Primeira consulta por reservationId (simula tela de confirmação pós-compra)
        List<Ticket> query1 = ticketRepository.findByReservationId(reservation1Id);
        assertThat(query1).hasSize(2);
        assertThat(query1.get(0).validationToken()).isEqualTo(valToken1);
        assertThat(query1.get(0).manualCode()).isEqualTo(manualCode1);
        assertThat(query1.get(0).shareToken()).isEqualTo(shareToken1);
        assertThat(query1.get(1).validationToken()).isEqualTo(valToken2);
        assertThat(query1.get(1).manualCode()).isEqualTo(manualCode2);
        assertThat(query1.get(1).shareToken()).isEqualTo(shareToken2);

        // Segunda consulta por id direto (simula reload ou abertura de detalhe de ticket)
        Optional<Ticket> reloaded = ticketRepository.findById(ticket1Id);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().validationToken()).isEqualTo(valToken1);
        assertThat(reloaded.get().manualCode()).isEqualTo(manualCode1);
        assertThat(reloaded.get().shareToken()).isEqualTo(shareToken1);

        // Terceira consulta por customerId (simula listagem de Meus Ingressos)
        List<Ticket> customerTickets = ticketRepository.findByCustomerId(customer1Id);
        assertThat(customerTickets).hasSize(2);
        assertThat(customerTickets.stream().map(Ticket::id)).containsExactlyInAnyOrder(ticket1Id, ticket2Id);
    }

    @Test
    @DisplayName("Garante constraint de unicidade no PostgreSQL para validation_token")
    void shouldEnforceUniqueConstraintOnValidationToken() {
        String duplicateValidationToken = credentialGenerator.generateValidationToken();

        Ticket ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                duplicateValidationToken, credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 2,
                duplicateValidationToken, credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );

        assertThatThrownBy(() -> ticketRepository.save(ticket2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Garante constraint de unicidade no PostgreSQL para share_token")
    void shouldEnforceUniqueConstraintOnShareToken() {
        String duplicateShareToken = credentialGenerator.generateShareToken();

        Ticket ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                duplicateShareToken, now
        );
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 2,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                duplicateShareToken, now
        );

        assertThatThrownBy(() -> ticketRepository.save(ticket2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Garante constraint de unicidade no PostgreSQL para manual_code")
    void shouldEnforceUniqueConstraintOnManualCode() {
        String duplicateManualCode = "AB7K92QX4M";

        Ticket ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), duplicateManualCode,
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 2,
                credentialGenerator.generateValidationToken(), duplicateManualCode,
                credentialGenerator.generateShareToken(), now
        );

        assertThatThrownBy(() -> ticketRepository.save(ticket2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Garante constraint de unicidade no PostgreSQL para (reservation_id, ordinal)")
    void shouldEnforceUniqueConstraintOnReservationAndOrdinal() {
        Ticket ticket1 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );

        assertThatThrownBy(() -> ticketRepository.save(ticket2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Busca por manualCode no repositório é case-insensitive, resiliente a separadores e resolve caracteres ambíguos")
    void shouldFindTicketByManualCodeWithVariousInputFormats() {
        // Código salvo com '0' e '1'
        String savedCode = "AB7K920X1M";
        Ticket ticket = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), savedCode,
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(ticket);

        // 1. Busca exata
        Optional<Ticket> exact = ticketRepository.findByManualCode("AB7K920X1M");
        assertThat(exact).isPresent().contains(ticket);

        // 2. Busca em minúsculas
        Optional<Ticket> lowercase = ticketRepository.findByManualCode("ab7k920x1m");
        assertThat(lowercase).isPresent().contains(ticket);

        // 3. Busca formatada agrupada
        Optional<Ticket> grouped = ticketRepository.findByManualCode("AB7K-920X-1M");
        assertThat(grouped).isPresent().contains(ticket);

        // 4. Busca agrupada em minúsculas
        Optional<Ticket> groupedLower = ticketRepository.findByManualCode("ab7k-920x-1m");
        assertThat(groupedLower).isPresent().contains(ticket);

        // 5. Busca com separadores variados e espaços
        Optional<Ticket> messy = ticketRepository.findByManualCode("  ab- 7k. 92/0x_1m  ");
        assertThat(messy).isPresent().contains(ticket);

        // 6. Busca digitada por humano com letras ambíguas 'O'/'o' e 'L'/'l' substituídas por '0' e '1'
        Optional<Ticket> ambiguous = ticketRepository.findByManualCode("ab-7k-92-ox-lm");
        assertThat(ambiguous).isPresent().contains(ticket);

        // 7. Código inexistente
        Optional<Ticket> notFound = ticketRepository.findByManualCode("ZZZZ999999");
        assertThat(notFound).isEmpty();

        // 8. Entrada nula ou em branco
        assertThat(ticketRepository.findByManualCode(null)).isEmpty();
        assertThat(ticketRepository.findByManualCode("   ")).isEmpty();
    }

    @Test
    @DisplayName("Busca por validationToken e shareToken funciona de forma exata e segura")
    void shouldFindTicketByValidationAndShareTokens() {
        String validationToken = credentialGenerator.generateValidationToken();
        String shareToken = credentialGenerator.generateShareToken();

        Ticket ticket = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                validationToken, credentialGenerator.generateManualCode(),
                shareToken, now
        );
        ticketRepository.save(ticket);

        // validationToken
        assertThat(ticketRepository.findByValidationToken(validationToken)).isPresent().contains(ticket);
        assertThat(ticketRepository.findByValidationToken("  " + validationToken + "  ")).isPresent().contains(ticket);
        assertThat(ticketRepository.findByValidationToken("nonexistent-token")).isEmpty();
        assertThat(ticketRepository.findByValidationToken(null)).isEmpty();
        assertThat(ticketRepository.findByValidationToken("   ")).isEmpty();

        // shareToken
        assertThat(ticketRepository.findByShareToken(shareToken)).isPresent().contains(ticket);
        assertThat(ticketRepository.findByShareToken("  " + shareToken + "  ")).isPresent().contains(ticket);
        assertThat(ticketRepository.findByShareToken("nonexistent-share-token")).isEmpty();
        assertThat(ticketRepository.findByShareToken(null)).isEmpty();
        assertThat(ticketRepository.findByShareToken("   ")).isEmpty();
    }

    @Test
    @DisplayName("Isolamento de cliente: consulta por customerId retorna somente os tickets do respectivo cliente")
    void shouldIsolateTicketsBetweenCustomers() {
        Ticket customer1Ticket = Ticket.createValid(
                UUID.randomUUID(), reservation1Id, eventId, sectorId, customer1Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(customer1Ticket);

        Ticket customer2Ticket = Ticket.createValid(
                UUID.randomUUID(), reservation2Id, eventId, sectorId, customer2Id, 1,
                credentialGenerator.generateValidationToken(), credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(), now
        );
        ticketRepository.save(customer2Ticket);

        List<Ticket> customer1Tickets = ticketRepository.findByCustomerId(customer1Id);
        assertThat(customer1Tickets).contains(customer1Ticket).doesNotContain(customer2Ticket);

        List<Ticket> customer2Tickets = ticketRepository.findByCustomerId(customer2Id);
        assertThat(customer2Tickets).contains(customer2Ticket).doesNotContain(customer1Ticket);
    }
}
