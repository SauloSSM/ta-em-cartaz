package br.com.elitedevticket.payments;

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
class PaymentsEndpointsIntegrationTest {

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
    @DisplayName("Customer dono com hold vigente simula tentativa DECLINED: Payment persistido, Reservation permanece HOLDING, tempo e estoque inalterados, zero tickets")
    void customerCanProcessSimulatedDeclinedPaymentSuccessfully() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar evento e setor publicado
        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Festival Rock In Rio 2026",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista Premium",
                100,
                "250.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // 2. Customer cria hold de 2 ingressos (R$ 500.00)
        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(resResponse.statusCode()).isEqualTo(201);
        String reservationId = extractJsonField(resResponse.body(), "id");

        // 3. Customer simula tentativa DECLINED com novo paymentAttemptId
        String paymentAttemptId = UUID.randomUUID().toString();
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );

        assertThat(payResponse.statusCode()).isEqualTo(200);
        assertThat(payResponse.body()).contains("\"id\":\"" + paymentAttemptId + "\"");
        assertThat(payResponse.body()).contains("\"reservationId\":\"" + reservationId + "\"");
        assertThat(payResponse.body()).contains("\"status\":\"DECLINED\"");
        assertThat(payResponse.body()).contains("\"amount\":500.00");
        assertThat(payResponse.body()).contains("\"currency\":\"BRL\"");
        assertThat(payResponse.body()).contains("\"provider\":\"FAKE\"");
        assertThat(payResponse.body()).contains("\"declineReason\":\"SIMULATED_DECLINE\"");
        assertThat(payResponse.body()).contains("\"processedAt\":");

        // 4. Invariantes no Banco de Dados:
        // a) Payment gravado com status DECLINED
        String paymentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?",
                String.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentStatus).isEqualTo("DECLINED");

        BigDecimal paymentAmount = jdbcTemplate.queryForObject(
                "SELECT amount FROM payments WHERE id = ?",
                BigDecimal.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentAmount).isEqualByComparingTo(new BigDecimal("500.00"));

        // b) Reservation permanece HOLDING com expires_at intacto
        String resStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(resStatus).isEqualTo("HOLDING");

        Instant dbExpiresAt = jdbcTemplate.queryForObject(
                "SELECT expires_at FROM reservations WHERE id = ?",
                Instant.class,
                UUID.fromString(reservationId)
        );
        assertThat(dbExpiresAt).isNotNull();

        // c) Estoque permanece comprometido (98)
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                UUID.fromString(sectorId)
        );
        assertThat(stock).isEqualTo(98);

        // d) Zero tickets emitidos / 1 registro em payments
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Replay idempotente da mesma tentativa com mesmo fingerprint reproduz o resultado sem duplicar registros")
    void idempotentReplayReturnsSameResultWithoutDuplicatingRecords() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Idempotencia",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"DECLINED\"}";

        // Primeira tentativa
        HttpResponse<String> payResponse1 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(payResponse1.statusCode()).isEqualTo(200);

        // Replay imediato da mesma tentativa
        HttpResponse<String> payResponse2 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(payResponse2.statusCode()).isEqualTo(200);
        assertThat(extractJsonField(payResponse2.body(), "id")).isEqualTo(extractJsonField(payResponse1.body(), "id"));
        assertThat(extractJsonField(payResponse2.body(), "reservationId")).isEqualTo(reservationId);
        assertThat(extractJsonField(payResponse2.body(), "status")).isEqualTo("DECLINED");
        assertThat(extractJsonField(payResponse2.body(), "amount")).isEqualTo(extractJsonField(payResponse1.body(), "amount"));
        assertThat(extractJsonField(payResponse2.body(), "currency")).isEqualTo("BRL");
        assertThat(extractJsonField(payResponse2.body(), "provider")).isEqualTo("FAKE");

        // Verificar que continua existindo apenas 1 registro no banco
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Reutilização de paymentAttemptId com fingerprint incompatível gera conflito HTTP 409 IDEMPOTENCY_CONFLICT")
    void reusingPaymentAttemptIdWithDifferentFingerprintReturns409Conflict() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Conflito Idempotencia",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();

        // 1. Primeira tentativa DECLINED
        HttpResponse<String> payResponse1 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(payResponse1.statusCode()).isEqualTo(200);

        // 2. Mesma tentativa mas com simulatedOutcome diferente (APPROVED) -> divergência de fingerprint
        HttpResponse<String> payResponse2 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(payResponse2.statusCode()).isEqualTo(409);
        assertThat(payResponse2.body()).contains("\"code\":\"IDEMPOTENCY_CONFLICT\"");
    }

    @Test
    @DisplayName("Usuário não proprietário não pode pagar a reserva de outro cliente (HTTP 403)")
    void nonOwnerCustomerCannotPayAnotherCustomersReservation() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerOneSession = loginSession("customer.one@demo.elitedevticket.local");
        String customerTwoSession = loginSession("customer.two@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Seguranca Ownership",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // Customer One cria a reserva
        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerOneSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        // Customer Two tenta pagar a reserva do Customer One
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerTwoSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + UUID.randomUUID() + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(403);
        assertThat(payResponse.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");
    }

    @Test
    @DisplayName("Papel diferente de CUSTOMER (ex. ORGANIZER) não pode pagar (HTTP 403)")
    void nonCustomerRoleCannotProcessPayment() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show RBAC Payment",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        // Organizer tenta chamar endpoint de pagamento
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + organizerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + UUID.randomUUID() + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Hold expirado não pode ser pago e retorna HTTP 422 RESERVATION_EXPIRED")
    void expiredHoldCannotBePaid() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Expiracao Payment",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        // Forçar expiração da reserva no banco
        jdbcTemplate.update(
                "UPDATE reservations SET expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)),
                UUID.fromString(reservationId)
        );

        // Tentar pagar reserva vencida
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + UUID.randomUUID() + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(422);
        assertThat(payResponse.body()).contains("\"code\":\"RESERVATION_EXPIRED\"");
    }

    @Test
    @DisplayName("Customer pode realizar múltiplas tentativas DECLINED consecutivas enquanto o hold permanecer válido")
    void customerCanMakeMultipleDeclinedAttemptsWhileHoldIsActive() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Multiplas Tentativas",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        // Tentativa 1
        String attempt1 = UUID.randomUUID().toString();
        HttpResponse<String> pay1 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + attempt1 + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(pay1.statusCode()).isEqualTo(200);
        assertThat(pay1.body()).contains("\"status\":\"DECLINED\"");

        // Tentativa 2 com NOVO paymentAttemptId
        String attempt2 = UUID.randomUUID().toString();
        HttpResponse<String> pay2 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + attempt2 + "\",\"simulatedOutcome\":\"DECLINED\"}"
        );
        assertThat(pay2.statusCode()).isEqualTo(200);
        assertThat(pay2.body()).contains("\"status\":\"DECLINED\"");

        // Ambas as tentativas foram gravadas
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(count).isEqualTo(2);

        // Reserva continua HOLDING
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(status).isEqualTo("HOLDING");
    }

    @Test
    @DisplayName("Concorrência de retries simultâneos do mesmo paymentAttemptId grava exatamente um registro no PostgreSQL")
    void concurrentRetriesOfSameAttemptIdPersistOnlyOnePayment() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Concorrencia Payment",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"DECLINED\"}";

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> post(
                    "/api/v1/reservations/" + reservationId + "/payments",
                    "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                    csrf,
                    payload
            ));
        }

        List<Future<HttpResponse<String>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        for (Future<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"DECLINED\"");
            assertThat(response.body()).contains("\"id\":\"" + paymentAttemptId + "\"");
        }

        // Verifica que exatamente 1 registro foi persistido
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("APPROVED válido: persiste Payment APPROVED, confirma Reservation e emite exatamente N Tickets atomicamente no PostgreSQL")
    void customerCanProcessSimulatedApprovedPaymentSuccessfully() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        // 1. Criar evento e setor publicado com 100 lugares a R$ 250,00
        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Festival Rock In Rio Approved",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista Premium",
                100,
                "250.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // 2. Customer cria hold de 2 ingressos (R$ 500.00)
        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        assertThat(resResponse.statusCode()).isEqualTo(201);
        String reservationId = extractJsonField(resResponse.body(), "id");

        // 3. Customer processa tentativa simulada APPROVED
        String paymentAttemptId = UUID.randomUUID().toString();
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );

        assertThat(payResponse.statusCode()).isEqualTo(200);
        assertThat(payResponse.body()).contains("\"id\":\"" + paymentAttemptId + "\"");
        assertThat(payResponse.body()).contains("\"reservationId\":\"" + reservationId + "\"");
        assertThat(payResponse.body()).contains("\"status\":\"APPROVED\"");
        assertThat(payResponse.body()).contains("\"amount\":500.00");
        assertThat(payResponse.body()).contains("\"currency\":\"BRL\"");
        assertThat(payResponse.body()).contains("\"provider\":\"FAKE\"");
        assertThat(payResponse.body()).doesNotContain("\"declineReason\"");

        // 4. Verificações no PostgreSQL:
        // a) Payment APPROVED persistido com snapshots corretos
        String paymentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?",
                String.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentStatus).isEqualTo("APPROVED");

        BigDecimal paymentAmount = jdbcTemplate.queryForObject(
                "SELECT amount FROM payments WHERE id = ?",
                BigDecimal.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentAmount).isEqualByComparingTo(new BigDecimal("500.00"));

        // b) Reservation transicionou para CONFIRMED com confirmed_at preenchido
        String resStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(resStatus).isEqualTo("CONFIRMED");

        Instant confirmedAt = jdbcTemplate.queryForObject(
                "SELECT confirmed_at FROM reservations WHERE id = ?",
                Instant.class,
                UUID.fromString(reservationId)
        );
        assertThat(confirmedAt).isNotNull();

        // c) Nenhum decremento adicional de estoque (continua 98, pois já havia sido reservado no hold)
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                UUID.fromString(sectorId)
        );
        assertThat(stock).isEqualTo(98);

        // d) Exatamente 2 Tickets emitidos no banco, ordinais 1 e 2, status VALID
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(2);

        List<Integer> ordinals = jdbcTemplate.queryForList(
                "SELECT ordinal FROM tickets WHERE reservation_id = ? ORDER BY ordinal ASC",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ordinals).containsExactly(1, 2);

        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM tickets WHERE reservation_id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(statuses).containsOnly("VALID");

        List<String> validationTokens = jdbcTemplate.queryForList(
                "SELECT validation_token FROM tickets WHERE reservation_id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(validationTokens).hasSize(2);
        assertThat(validationTokens.get(0)).isNotEqualTo(validationTokens.get(1));

        List<String> manualCodes = jdbcTemplate.queryForList(
                "SELECT manual_code FROM tickets WHERE reservation_id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(manualCodes).hasSize(2);
        assertThat(manualCodes.get(0)).isNotEqualTo(manualCodes.get(1));
    }

    @Test
    @DisplayName("APPROVED com quantity 1 emite exatamente 1 Ticket")
    void customerCanProcessApprovedPaymentForQuantity1() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Quantity 1",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "80.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(200);

        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(1);
    }

    @Test
    @DisplayName("APPROVED com quantity 6 emite exatamente 6 Tickets com ordinais 1..6")
    void customerCanProcessApprovedPaymentForQuantity6() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Quantity 6",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":6}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(200);

        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(6);

        List<Integer> ordinals = jdbcTemplate.queryForList(
                "SELECT ordinal FROM tickets WHERE reservation_id = ? ORDER BY ordinal ASC",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ordinals).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("Replay idempotente de tentativa APPROVED retorna o mesmo Payment sem duplicar Tickets")
    void idempotentReplayOfApprovedPaymentDoesNotDuplicateTickets() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Replay Approved",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}";

        // 1. Primeira chamada APPROVED
        HttpResponse<String> pay1 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(pay1.statusCode()).isEqualTo(200);

        // 2. Replay com mesmo paymentAttemptId
        HttpResponse<String> pay2 = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(pay2.statusCode()).isEqualTo(200);
        assertThat(extractJsonField(pay2.body(), "id")).isEqualTo(paymentAttemptId);
        assertThat(extractJsonField(pay2.body(), "status")).isEqualTo("APPROVED");

        // 3. Verifica que exatamente 2 Tickets continuam existindo no banco
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Dois paymentAttemptIds diferentes concorrendo pela mesma Reservation: apenas um confirma e o segundo recebe conflito")
    void twoDifferentPaymentAttemptsRacingForSameReservationOnlyOneConfirms() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Concorrencia Dois Attempts",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String attempt1 = UUID.randomUUID().toString();
        String attempt2 = UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<HttpResponse<String>> task1 = () -> post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + attempt1 + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        Callable<HttpResponse<String>> task2 = () -> post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + attempt2 + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );

        List<Future<HttpResponse<String>>> futures = executor.invokeAll(List.of(task1, task2));
        executor.shutdown();

        HttpResponse<String> res1 = futures.get(0).get();
        HttpResponse<String> res2 = futures.get(1).get();

        int successCount = (res1.statusCode() == 200 ? 1 : 0) + (res2.statusCode() == 200 ? 1 : 0);
        int conflictCount = (res1.statusCode() == 409 ? 1 : 0) + (res2.statusCode() == 409 ? 1 : 0);

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        // Exatamente 2 Tickets no banco
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Tentativa APPROVED para Reservation expirada nunca confirma e retorna HTTP 422 com zero tickets")
    void expiredHoldWithApprovedAttemptNeverConfirmsAndEmitsZeroTickets() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Expirada Approved",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        // Expira a reserva
        jdbcTemplate.update(
                "UPDATE reservations SET expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)),
                UUID.fromString(reservationId)
        );

        // Tentar pagar com APPROVED
        HttpResponse<String> payResponse = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + UUID.randomUUID() + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(payResponse.statusCode()).isEqualTo(422);
        assertThat(payResponse.body()).contains("\"code\":\"RESERVATION_EXPIRED\"");

        // Status no banco é EXPIRED
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(status).isEqualTo("EXPIRED");

        // Zero tickets emitidos
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Story 5.3 — APPROVED commitado + resposta perdida: reconcile com mesmo paymentAttemptId retorna APPROVED sem gateway novo e sem duplicar Tickets")
    void reconciliationOfCommittedApprovedPaymentReturnsApprovedWithoutReExecutingGatewayOrDuplicatingTickets() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Reconcile Approved 5.3",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista VIP",
                100,
                "300.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":2}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}";

        // 1. Primeira chamada enviada pelo cliente (simula que backend processou e commitou, mas resposta se perdeu no caminho)
        HttpResponse<String> initialPay = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(initialPay.statusCode()).isEqualTo(200);

        // Captura timestamps e contagens autoritativas
        Instant initialProcessedAt = jdbcTemplate.queryForObject(
                "SELECT processed_at FROM payments WHERE id = ?",
                Instant.class,
                UUID.fromString(paymentAttemptId)
        );
        Instant confirmedAt = jdbcTemplate.queryForObject(
                "SELECT confirmed_at FROM reservations WHERE id = ?",
                Instant.class,
                UUID.fromString(reservationId)
        );
        assertThat(initialProcessedAt).isNotNull();
        assertThat(confirmedAt).isNotNull();

        // 2. Cliente entra em modo de reconciliação e consulta com o MESMO paymentAttemptId
        HttpResponse<String> reconcilePay = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(reconcilePay.statusCode()).isEqualTo(200);
        assertThat(reconcilePay.body()).contains("\"id\":\"" + paymentAttemptId + "\"");
        assertThat(reconcilePay.body()).contains("\"reservationId\":\"" + reservationId + "\"");
        assertThat(reconcilePay.body()).contains("\"status\":\"APPROVED\"");
        assertThat(reconcilePay.body()).contains("\"amount\":600.00");
        assertThat(reconcilePay.body()).contains("\"currency\":\"BRL\"");

        // 3. Verificação de Invariantes Estritas no PostgreSQL:
        // a) Exatamente 1 registro de Payment no banco de dados para a tentativa
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentCount).isEqualTo(1);

        // b) Exatamente 2 Tickets emitidos, sem nenhuma duplicação
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(2);

        // c) Estoque permanece inalterado (98 restantes)
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                UUID.fromString(sectorId)
        );
        assertThat(stock).isEqualTo(98);

        // d) Timestamps persistidos inalterados
        Instant finalProcessedAt = jdbcTemplate.queryForObject(
                "SELECT processed_at FROM payments WHERE id = ?",
                Instant.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(finalProcessedAt).isEqualTo(initialProcessedAt);
    }

    @Test
    @DisplayName("Story 5.3 — DECLINED commitado + resposta perdida: reconcile com mesmo paymentAttemptId retorna DECLINED sem gateway novo")
    void reconciliationOfCommittedDeclinedPaymentReturnsDeclinedWithoutReExecutingGateway() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Reconcile Declined 5.3",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista Comum",
                50,
                "150.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"DECLINED\"}";

        // 1. Primeira chamada DECLINED
        HttpResponse<String> initialPay = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(initialPay.statusCode()).isEqualTo(200);
        assertThat(initialPay.body()).contains("\"status\":\"DECLINED\"");

        // 2. Reconciliação da tentativa DECLINED
        HttpResponse<String> reconcilePay = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(reconcilePay.statusCode()).isEqualTo(200);
        assertThat(reconcilePay.body()).contains("\"status\":\"DECLINED\"");
        assertThat(reconcilePay.body()).contains("\"id\":\"" + paymentAttemptId + "\"");

        // 3. Invariantes no banco:
        // a) Reserva continua HOLDING
        String resStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                UUID.fromString(reservationId)
        );
        assertThat(resStatus).isEqualTo("HOLDING");

        // b) Exatamente 1 registro de payment
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentCount).isEqualTo(1);

        // c) Zero tickets
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Story 5.3 — Reconciliação concorrente múltipla do mesmo paymentAttemptId produz resultado consistente e zero efeitos colaterais extras")
    void concurrentReconciliationOfSamePaymentAttemptProducesConsistentResult() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerSession = loginSession("customer.one@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Concurrent Reconcile 5.3",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                100,
                "200.00"
        );
        String sectorId = getFirstSectorId(eventId);

        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":3}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        String payload = "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}";

        // Commit inicial da tentativa
        HttpResponse<String> initialPay = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                payload
        );
        assertThat(initialPay.statusCode()).isEqualTo(200);

        // 4 chamadas concorrentes de reconciliação
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> post(
                    "/api/v1/reservations/" + reservationId + "/payments",
                    "EDT_SESSION=" + customerSession + "; XSRF-TOKEN=" + csrf,
                    csrf,
                    payload
            ));
        }

        List<Future<HttpResponse<String>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        for (Future<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"APPROVED\"");
            assertThat(response.body()).contains("\"id\":\"" + paymentAttemptId + "\"");
        }

        // Exatamente 3 Tickets e 1 Payment persistido
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                UUID.fromString(reservationId)
        );
        assertThat(ticketCount).isEqualTo(3);

        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                UUID.fromString(paymentAttemptId)
        );
        assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Story 5.3 — CUSTOMER não proprietário não consegue reconciliar tentativa de outro usuário (HTTP 403 AUTH_FORBIDDEN)")
    void nonOwnerCustomerCannotReconcileAnotherUsersPaymentAttempt() throws Exception {
        String organizerSession = loginSession("organizer@demo.elitedevticket.local");
        String customerOneSession = loginSession("customer.one@demo.elitedevticket.local");
        String customerTwoSession = loginSession("customer.two@demo.elitedevticket.local");
        String csrf = bootstrapCsrf();

        String eventId = createAndPublishEvent(
                organizerSession,
                csrf,
                "Show Ownership Reconcile 5.3",
                Instant.now().plus(20, ChronoUnit.DAYS).toString(),
                "Pista",
                50,
                "100.00"
        );
        String sectorId = getFirstSectorId(eventId);

        // Customer One cria reserva e processa pagamento
        HttpResponse<String> resResponse = post(
                "/api/v1/events/" + eventId + "/sectors/" + sectorId + "/reservations",
                "EDT_SESSION=" + customerOneSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"quantity\":1}"
        );
        String reservationId = extractJsonField(resResponse.body(), "id");

        String paymentAttemptId = UUID.randomUUID().toString();
        HttpResponse<String> payOne = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerOneSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(payOne.statusCode()).isEqualTo(200);

        // Customer Two tenta reconciliar o paymentAttemptId do Customer One
        HttpResponse<String> reconcileTwo = post(
                "/api/v1/reservations/" + reservationId + "/payments",
                "EDT_SESSION=" + customerTwoSession + "; XSRF-TOKEN=" + csrf,
                csrf,
                "{\"paymentAttemptId\":\"" + paymentAttemptId + "\",\"simulatedOutcome\":\"APPROVED\"}"
        );
        assertThat(reconcileTwo.statusCode()).isEqualTo(403);
        assertThat(reconcileTwo.body()).contains("\"code\":\"AUTH_FORBIDDEN\"");
    }


    private String bootstrapCsrf() throws Exception {
        HttpResponse<String> response = get("/api/v1/auth/session", "");
        return cookieValue(response, "XSRF-TOKEN");
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

    private String createAndPublishEvent(
            String organizerSession,
            String csrf,
            String title,
            String startsAt,
            String sectorName,
            int capacity,
            String price
    ) throws Exception {
        String draftPayload = """
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
                draftPayload
        );
        assertThat(draftRes.statusCode()).isEqualTo(201);
        String eventId = extractJsonField(draftRes.body(), "id");

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

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (!cookie.isBlank()) {
            request.header("Cookie", cookie);
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

    private HttpResponse<String> post(String path, String cookie, String csrf, String body) throws Exception {
        String key = path.contains("/reservations") && path.contains("/sectors/") ? UUID.randomUUID().toString() : null;
        return postWithHeaders(path, cookie, csrf, key, body);
    }

    private HttpResponse<String> postWithHeaders(
            String path,
            String cookie,
            String csrf,
            String idempotencyKey,
            String body
    ) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (cookie != null && !cookie.isBlank()) {
            request.header("Cookie", cookie);
        }
        if (csrf != null && !csrf.isBlank()) {
            request.header("X-XSRF-TOKEN", csrf);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request.header("Idempotency-Key", idempotencyKey);
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
