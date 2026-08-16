package br.com.elitedevticket.reservations;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.reservations.application.CreateReservationCommand;
import br.com.elitedevticket.reservations.application.CreateReservationUseCase;
import br.com.elitedevticket.reservations.application.ExpireReservationUseCase;
import br.com.elitedevticket.reservations.application.ReservationExpiryScheduler;
import br.com.elitedevticket.reservations.application.ReservationRepository;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class ReservationExpirationIntegrationTest {

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
    private ExpireReservationUseCase expireReservationUseCase;

    @Autowired
    private ReservationExpiryScheduler reservationExpiryScheduler;

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EventStockPort eventStockPort;

    @Autowired
    private Clock clock;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("Hold antes de expiresAt permanece HOLDING e não devolve estoque")
    void holdBeforeExpiresAtRemainsHoldingAndDoesNotRestoreStock() {
        UUID eventId = createEventInDb("Festival de Inverno", Instant.now().plus(10, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Pista", 50, 48, new BigDecimal("100.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        // Hold vigente (expires in 8 minutes)
        Instant expiresAt = clock.instant().plus(8, ChronoUnit.MINUTES);
        UUID reservationId = createReservationInDb(customerId, eventId, sectorId, 2, ReservationStatus.HOLDING, expiresAt);

        boolean expired = expireReservationUseCase.execute(reservationId);

        assertThat(expired).isFalse();

        // Status remains HOLDING
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.HOLDING);

        // Stock remains 48
        int available = getAvailableStock(sectorId);
        assertThat(available).isEqualTo(48);
    }

    @Test
    @DisplayName("Hold vencido (expiresAt <= serverNow) torna-se EXPIRED e devolve exatamente a quantidade ao setor")
    void expiredHoldTransitionsToEXPIREDAndRestoresExactStock() {
        UUID eventId = createEventInDb("Show de Rock", Instant.now().plus(5, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Camarote", 50, 45, new BigDecimal("200.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        // Hold expired 2 minutes ago
        Instant expiresAt = clock.instant().minus(2, ChronoUnit.MINUTES);
        UUID reservationId = createReservationInDb(customerId, eventId, sectorId, 5, ReservationStatus.HOLDING, expiresAt);

        boolean expired = expireReservationUseCase.execute(reservationId);

        assertThat(expired).isTrue();

        // Status changed to EXPIRED
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);

        // Stock restored from 45 -> 50 (45 + 5)
        int available = getAvailableStock(sectorId);
        assertThat(available).isEqualTo(50);
    }

    @Test
    @DisplayName("Reconciliar a mesma Reservation duas vezes não devolve estoque duas vezes (idempotência)")
    void reconcilingSameReservationTwiceDoesNotRestoreStockTwice() {
        UUID eventId = createEventInDb("Teatro Musical", Instant.now().plus(7, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Plateia", 50, 40, new BigDecimal("120.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        // Expired hold
        Instant expiresAt = clock.instant().minus(1, ChronoUnit.MINUTES);
        UUID reservationId = createReservationInDb(customerId, eventId, sectorId, 4, ReservationStatus.HOLDING, expiresAt);

        // 1ª reconciliação: deve expirar e devolver 4 unidades (40 -> 44)
        boolean firstRun = expireReservationUseCase.execute(reservationId);
        assertThat(firstRun).isTrue();
        assertThat(getAvailableStock(sectorId)).isEqualTo(44);

        // 2ª reconciliação: não deve fazer nada (idempotente)
        boolean secondRun = expireReservationUseCase.execute(reservationId);
        assertThat(secondRun).isFalse();
        assertThat(getAvailableStock(sectorId)).isEqualTo(44);
    }

    @Test
    @DisplayName("Concorrência de múltiplas threads tentando expirar a mesma Reservation tem efeito único")
    void concurrentExpiryAttemptsHaveSingleEffect() throws Exception {
        UUID eventId = createEventInDb("Stand-up Comedy", Instant.now().plus(4, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Geral", 100, 80, new BigDecimal("80.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        Instant expiresAt = clock.instant().minus(1, ChronoUnit.MINUTES);
        UUID reservationId = createReservationInDb(customerId, eventId, sectorId, 6, ReservationStatus.HOLDING, expiresAt);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                startLatch.await();
                return expireReservationUseCase.execute(reservationId);
            });
        }

        startLatch.countDown();
        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        // Exatamente uma thread executou a devolução
        assertThat(successCount).isEqualTo(1);

        // Estoque restaurado exatamente uma vez: 80 + 6 = 86
        assertThat(getAvailableStock(sectorId)).isEqualTo(86);

        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("Inconsistência de devolução de estoque que excederia capacity aborta transação sem side effects")
    void stockRestorationExceedingCapacityThrowsExceptionWithoutSideEffects() {
        UUID eventId = createEventInDb("Feira Gastronômica", Instant.now().plus(6, ChronoUnit.DAYS));
        // Capacity 50, currently 49 available
        UUID sectorId = createSectorInDb(eventId, "Ingresso Único", 50, 49, new BigDecimal("50.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        // Hold of 4 tickets expired (49 + 4 = 53 > capacity 50 -> inconsistent)
        Instant expiresAt = clock.instant().minus(1, ChronoUnit.MINUTES);
        UUID reservationId = createReservationInDb(customerId, eventId, sectorId, 4, ReservationStatus.HOLDING, expiresAt);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> expireReservationUseCase.execute(reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inconsistência de estoque");

        // Rollback atômico:
        // 1. Estoque permanece exatamente 49
        assertThat(getAvailableStock(sectorId)).isEqualTo(49);

        // 2. Reserva permanece em HOLDING
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.HOLDING);
    }

    @Test
    @DisplayName("Lazy expiry libera estoque de hold vencido no setor antes de nova tentativa, eliminando falsa escassez")
    void lazyExpiryReconcilesExpiredHoldBeforeNewReservationEliminatingFalseScarcity() {
        UUID eventId = createEventInDb("Cinema Open Air", Instant.now().plus(3, ChronoUnit.DAYS));
        // Capacity 2, currently 0 available because oldCustomer holds 2 tickets
        UUID sectorId = createSectorInDb(eventId, "Pista", 2, 0, new BigDecimal("30.00"));
        UUID oldCustomerId = getCustomerId("customer.one@demo.elitedevticket.local");
        UUID newCustomerId = getCustomerId("customer.two@demo.elitedevticket.local");

        // Old hold expired 3 minutes ago
        Instant expiresAt = clock.instant().minus(3, ChronoUnit.MINUTES);
        UUID oldReservationId = createReservationInDb(oldCustomerId, eventId, sectorId, 2, ReservationStatus.HOLDING, expiresAt);

        // New customer attempts to reserve 2 tickets (without waiting for the 30s scheduler)
        CreateReservationCommand command = new CreateReservationCommand(
                newCustomerId,
                eventId,
                sectorId,
                2,
                "idemp-lazy-scarcity-" + UUID.randomUUID()
        );

        Reservation newReservation = createReservationUseCase.execute(command);

        // The old reservation was expired by lazy reconciliation
        Reservation oldReservation = reservationRepository.findById(oldReservationId).orElseThrow();
        assertThat(oldReservation.status()).isEqualTo(ReservationStatus.EXPIRED);

        // The new reservation was successfully created without false scarcity!
        assertThat(newReservation.customerId()).isEqualTo(newCustomerId);
        assertThat(newReservation.quantity()).isEqualTo(2);
        assertThat(newReservation.status()).isEqualTo(ReservationStatus.HOLDING);

        // Available stock is now 0 (0 -> restored 2 -> consumed 2 = 0)
        assertThat(getAvailableStock(sectorId)).isEqualTo(0);
    }

    @Test
    @DisplayName("Scheduler e Lazy Expiry concorrentes não duplicam devolução de estoque")
    void concurrentSchedulerAndLazyExpiryDoNotDuplicateStockRestoration() throws Exception {
        UUID eventId = createEventInDb("Conferência de Tecnologia", Instant.now().plus(12, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Auditório", 100, 50, new BigDecimal("250.00"));
        UUID oldCustomerId = getCustomerId("customer.one@demo.elitedevticket.local");
        UUID newCustomerId = getCustomerId("customer.two@demo.elitedevticket.local");

        // Expired hold of 4 tickets
        Instant expiresAt = clock.instant().minus(2, ChronoUnit.MINUTES);
        createReservationInDb(oldCustomerId, eventId, sectorId, 4, ReservationStatus.HOLDING, expiresAt);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Integer> schedulerFuture = executor.submit(() -> {
            startLatch.await();
            return reservationExpiryScheduler.runCleanup();
        });

        Future<Reservation> lazyFuture = executor.submit(() -> {
            startLatch.await();
            return createReservationUseCase.execute(new CreateReservationCommand(
                    newCustomerId,
                    eventId,
                    sectorId,
                    2,
                    "idemp-concurrent-sched-" + UUID.randomUUID()
            ));
        });

        startLatch.countDown();
        schedulerFuture.get();
        Reservation newReservation = lazyFuture.get();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(newReservation.status()).isEqualTo(ReservationStatus.HOLDING);

        // Initial stock: 50
        // Restored from expired hold: +4
        // Consumed by new reservation: -2
        // Final stock MUST BE exactly 52 (never 56 by double restoration)!
        assertThat(getAvailableStock(sectorId)).isEqualTo(52);
    }

    @Test
    @DisplayName("Reservation CONFIRMED nunca é expirada pelo scheduler nem por lazy expiry")
    void confirmedReservationIsNeverExpired() {
        UUID eventId = createEventInDb("Workshop de Arquitetura", Instant.now().plus(8, ChronoUnit.DAYS));
        UUID sectorId = createSectorInDb(eventId, "Sala 1", 30, 25, new BigDecimal("500.00"));
        UUID customerId = getCustomerId("customer.one@demo.elitedevticket.local");

        // Confirmed reservation whose expiresAt is in the past
        Instant expiresAt = clock.instant().minus(5, ChronoUnit.MINUTES);
        UUID confirmedReservationId = createReservationInDb(customerId, eventId, sectorId, 3, ReservationStatus.CONFIRMED, expiresAt);

        boolean expired = expireReservationUseCase.execute(confirmedReservationId);

        assertThat(expired).isFalse();

        // Remains CONFIRMED
        Reservation reservation = reservationRepository.findById(confirmedReservationId).orElseThrow();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);

        // Stock unaffected (remains 25)
        assertThat(getAvailableStock(sectorId)).isEqualTo(25);
    }

    // --- Helpers ---

    private UUID createEventInDb(String title, Instant startsAt) {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = getCustomerId("organizer@demo.elitedevticket.local");
        jdbcTemplate.update(
                "INSERT INTO events (id, organizer_id, title, description, category, status, venue_name, venue_address, starts_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, 'PUBLISHED', 'Local', 'Endereco', ?, NOW(), NOW())",
                eventId, organizerId, title, "Descricao do " + title, "Show", java.sql.Timestamp.from(startsAt)
        );
        return eventId;
    }

    private UUID createSectorInDb(UUID eventId, String name, int capacity, int availableQuantity, BigDecimal price) {
        UUID sectorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                sectorId, eventId, name, "Descricao " + name, capacity, availableQuantity, price
        );
        return sectorId;
    }

    private UUID createReservationInDb(UUID customerId, UUID eventId, UUID sectorId, int quantity, ReservationStatus status, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Instant createdAt = expiresAt.minus(10, ChronoUnit.MINUTES);

        jdbcTemplate.update(
                "INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, customerId, eventId, sectorId, quantity, unitPrice, totalAmount, status.name(), java.sql.Timestamp.from(expiresAt), java.sql.Timestamp.from(createdAt)
        );
        return reservationId;
    }

    private int getAvailableStock(UUID sectorId) {
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                sectorId
        );
        return stock == null ? 0 : stock;
    }

    private UUID getCustomerId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                UUID.class,
                email
        );
    }
}
