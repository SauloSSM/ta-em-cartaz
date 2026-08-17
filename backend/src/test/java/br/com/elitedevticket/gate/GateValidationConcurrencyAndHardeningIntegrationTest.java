package br.com.elitedevticket.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.gate.application.ValidateTicketCommand;
import br.com.elitedevticket.gate.application.ValidateTicketUseCase;
import br.com.elitedevticket.gate.domain.GateAttemptConflictException;
import br.com.elitedevticket.gate.domain.GateOwnershipException;
import br.com.elitedevticket.gate.domain.GateValidationResult;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import br.com.elitedevticket.gate.domain.ValidationMethod;
import br.com.elitedevticket.tickets.application.TicketRepository;
import br.com.elitedevticket.tickets.domain.Ticket;
import br.com.elitedevticket.tickets.domain.TicketCredentialGenerator;
import br.com.elitedevticket.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class GateValidationConcurrencyAndHardeningIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ValidateTicketUseCase validateTicketUseCase;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final TicketCredentialGenerator credentialGenerator = new TicketCredentialGenerator();

    private UUID gateUserId1;
    private UUID gateUserId2;
    private UUID customerId;
    private UUID organizerId;
    private UUID eventId1;
    private UUID eventId2;
    private UUID sectorId1;
    private UUID sectorId2;
    private UUID reservation1Id;
    private UUID reservation2Id;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM validation_attempts");
        jdbcTemplate.update("DELETE FROM tickets");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM ticket_sectors");
        jdbcTemplate.update("DELETE FROM events");

        gateUserId1 = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'gate@demo.elitedevticket.local'",
                UUID.class
        );
        
        List<UUID> existingGate2 = jdbcTemplate.query(
                "SELECT id FROM users WHERE email = 'gate.two@demo.elitedevticket.local'",
                (rs, rowNum) -> (UUID) rs.getObject("id")
        );
        if (existingGate2.isEmpty()) {
            gateUserId2 = UUID.randomUUID();
            jdbcTemplate.update(
                    """
                    INSERT INTO users (id, email, role, password_hash)
                    VALUES (?, 'gate.two@demo.elitedevticket.local', 'GATE', '$2a$10$4rG3lfYmu7XUHp4JcDprUOkDK/w9RQ00Za703kW/oGaBu1oNEjm1q')
                    """,
                    gateUserId2
            );
        } else {
            gateUserId2 = existingGate2.get(0);
        }
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
                VALUES (?, ?, 'SEED', ?, 'Evento Validação A', 'SHOW', 'Desc', 'Arena Principal', 'Av A', NOW() + INTERVAL '5 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId1, organizerId, "ext-" + UUID.randomUUID()
        );

        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'SEED', ?, 'Evento Validação B', 'SHOW', 'Desc', 'Arena Secundária', 'Av B', NOW() + INTERVAL '6 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId2, organizerId, "ext-" + UUID.randomUUID()
        );

        sectorId1 = UUID.randomUUID();
        sectorId2 = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Pista Premium', 'Desc', 500, 500, 150.00, NOW(), NOW())
                """,
                sectorId1, eventId1
        );

        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Camarote', 'Desc', 200, 200, 300.00, NOW(), NOW())
                """,
                sectorId2, eventId2
        );

        reservation1Id = UUID.randomUUID();
        reservation2Id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 150.00, 150.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation1Id, customerId, eventId1, sectorId1
        );

        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 300.00, 300.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservation2Id, customerId, eventId2, sectorId2
        );
    }

    private Ticket createValidTicket(UUID eventId, UUID sectorId) {
        UUID reservationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO reservations (id, customer_id, event_id, sector_id, quantity, unit_price, total_amount, status, expires_at, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 1, 150.00, 150.00, 'CONFIRMED', NOW() + INTERVAL '10 minutes', NOW(), NOW())
                """,
                reservationId, customerId, eventId, sectorId
        );

        Ticket ticket = Ticket.createValid(
                UUID.randomUUID(),
                reservationId,
                eventId,
                sectorId,
                customerId,
                1,
                credentialGenerator.generateValidationToken(),
                credentialGenerator.generateManualCode(),
                credentialGenerator.generateShareToken(),
                Instant.now().minusSeconds(600)
        );
        return ticketRepository.save(ticket);
    }

    @RepeatedTest(10)
    @DisplayName("Concorrência real: 2 validações simultâneas do mesmo ticket VALID resultam em exatamente 1 VALID e 1 ALREADY_USED")
    void concurrentValidationOnSameTicketYieldsExactlyOneValidAndOneAlreadyUsed() throws Exception {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        String manualCode = ticket.manualCode();

        UUID attemptId1 = UUID.randomUUID();
        UUID attemptId2 = UUID.randomUUID();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<ValidationAttempt> task1 = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptId1,
                    gateUserId1,
                    eventId1,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        Callable<ValidationAttempt> task2 = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptId2,
                    gateUserId2,
                    eventId1,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        List<Future<ValidationAttempt>> futures = executor.invokeAll(List.of(task1, task2));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        ValidationAttempt result1 = futures.get(0).get();
        ValidationAttempt result2 = futures.get(1).get();

        List<GateValidationResult> outcomes = List.of(result1.result(), result2.result());
        assertThat(outcomes).containsExactlyInAnyOrder(GateValidationResult.VALID, GateValidationResult.ALREADY_USED);

        // Ticket termina USED uma única vez
        Ticket ticketInDb = ticketRepository.findById(ticket.id()).orElseThrow();
        assertThat(ticketInDb.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticketInDb.usedAt()).isNotNull();

        // usedAt e usedByGateUserId pertencem ao vencedor
        ValidationAttempt winnerAttempt = result1.result() == GateValidationResult.VALID ? result1 : result2;
        ValidationAttempt loserAttempt = result1.result() == GateValidationResult.VALID ? result2 : result1;

        assertThat(ticketInDb.usedByGateUserId()).isEqualTo(winnerAttempt.gateUserId());

        // Duas auditorias coerentes persistidas no banco
        Integer validAuditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ? AND result = 'VALID' AND ticket_id = ? AND gate_user_id = ?",
                Integer.class,
                winnerAttempt.id(),
                ticket.id(),
                winnerAttempt.gateUserId()
        );
        assertThat(validAuditCount).isEqualTo(1);

        Integer alreadyUsedAuditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ? AND result = 'ALREADY_USED' AND ticket_id = ? AND gate_user_id = ?",
                Integer.class,
                loserAttempt.id(),
                ticket.id(),
                loserAttempt.gateUserId()
        );
        assertThat(alreadyUsedAuditCount).isEqualTo(1);

        Integer totalAttempts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE ticket_id = ?",
                Integer.class,
                ticket.id()
        );
        assertThat(totalAttempts).isEqualTo(2);
    }

    @Test
    @DisplayName("Alta concorrência: 5 validações concorrentes do mesmo Ticket resultam em exatamente 1 VALID e 4 ALREADY_USED")
    void highConcurrencyValidationsOnSameTicketYieldExactlyOneValidAndFourAlreadyUsed() throws Exception {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        String manualCode = ticket.manualCode();

        int threadCount = 5;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<ValidationAttempt>> tasks = new ArrayList<>();
        List<UUID> attemptIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            UUID attemptId = UUID.randomUUID();
            attemptIds.add(attemptId);
            UUID gateUser = (i % 2 == 0) ? gateUserId1 : gateUserId2;
            tasks.add(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return validateTicketUseCase.execute(new ValidateTicketCommand(
                        attemptId,
                        gateUser,
                        eventId1,
                        ValidationMethod.MANUAL,
                        manualCode
                ));
            });
        }

        List<Future<ValidationAttempt>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<GateValidationResult> outcomes = new ArrayList<>();
        for (Future<ValidationAttempt> future : futures) {
            outcomes.add(future.get().result());
        }

        long validCount = outcomes.stream().filter(r -> r == GateValidationResult.VALID).count();
        long alreadyUsedCount = outcomes.stream().filter(r -> r == GateValidationResult.ALREADY_USED).count();

        assertThat(validCount).isEqualTo(1);
        assertThat(alreadyUsedCount).isEqualTo(threadCount - 1);

        Ticket ticketInDb = ticketRepository.findById(ticket.id()).orElseThrow();
        assertThat(ticketInDb.status()).isEqualTo(TicketStatus.USED);

        Integer totalAudits = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE ticket_id = ?",
                Integer.class,
                ticket.id()
        );
        assertThat(totalAudits).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("WRONG_EVENT concorrente: tentativas simultâneas de outro evento nunca consomem o Ticket")
    void concurrentWrongEventValidationsNeverConsumeTicket() throws Exception {
        // Ticket pertence ao eventId2
        Ticket ticketEvent2 = createValidTicket(eventId2, sectorId2);
        String manualCode = ticketEvent2.manualCode();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        UUID attemptId1 = UUID.randomUUID();
        UUID attemptId2 = UUID.randomUUID();

        // Ambas validam contra eventId1 (evento errado)
        Callable<ValidationAttempt> task1 = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptId1,
                    gateUserId1,
                    eventId1,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        Callable<ValidationAttempt> task2 = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptId2,
                    gateUserId2,
                    eventId1,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        List<Future<ValidationAttempt>> futures = executor.invokeAll(List.of(task1, task2));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(futures.get(0).get().result()).isEqualTo(GateValidationResult.WRONG_EVENT);
        assertThat(futures.get(1).get().result()).isEqualTo(GateValidationResult.WRONG_EVENT);

        // Ticket permanece estritamente VALID e sem dados de consumo
        Ticket ticketAfter = ticketRepository.findById(ticketEvent2.id()).orElseThrow();
        assertThat(ticketAfter.status()).isEqualTo(TicketStatus.VALID);
        assertThat(ticketAfter.usedAt()).isNull();
        assertThat(ticketAfter.usedByGateUserId()).isNull();

        // 2 auditorias WRONG_EVENT gravadas com precisão
        Integer wrongEventAuditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE ticket_id = ? AND result = 'WRONG_EVENT'",
                Integer.class,
                ticketEvent2.id()
        );
        assertThat(wrongEventAuditCount).isEqualTo(2);
    }

    @RepeatedTest(10)
    @DisplayName("Concorrência entre eventos: 1 tentativa no Evento A (correto) e 1 tentativa no Evento B (errado) resultam em exatamente 1 VALID e 1 WRONG_EVENT")
    void concurrentValidationCrossEventsSameTicketYieldsValidForCorrectEventAndWrongEventForOther() throws Exception {
        // Ticket VALID pertencente ao Event A (eventId1)
        Ticket ticketEventA = createValidTicket(eventId1, sectorId1);
        String manualCode = ticketEventA.manualCode();

        UUID attemptIdEventA = UUID.randomUUID();
        UUID attemptIdEventB = UUID.randomUUID();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1 valida com selectedEventId = eventId1 (correto)
        Callable<ValidationAttempt> taskCorrectEvent = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptIdEventA,
                    gateUserId1,
                    eventId1,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        // Thread 2 valida com selectedEventId = eventId2 (errado)
        Callable<ValidationAttempt> taskWrongEvent = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptIdEventB,
                    gateUserId2,
                    eventId2,
                    ValidationMethod.MANUAL,
                    manualCode
            ));
        };

        List<Future<ValidationAttempt>> futures = executor.invokeAll(List.of(taskCorrectEvent, taskWrongEvent));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        ValidationAttempt attemptA = futures.get(0).get();
        ValidationAttempt attemptB = futures.get(1).get();

        // 1. Tentativa do Event correto → exatamente VALID
        assertThat(attemptA.result()).isEqualTo(GateValidationResult.VALID);
        assertThat(attemptA.selectedEventId()).isEqualTo(eventId1);
        assertThat(attemptA.ticketId()).isEqualTo(ticketEventA.id());

        // 2. Tentativa do Event errado → exatamente WRONG_EVENT
        assertThat(attemptB.result()).isEqualTo(GateValidationResult.WRONG_EVENT);
        assertThat(attemptB.selectedEventId()).isEqualTo(eventId2);
        assertThat(attemptB.ticketId()).isEqualTo(ticketEventA.id());

        // 3. Ticket termina USED
        Ticket ticketAfter = ticketRepository.findById(ticketEventA.id()).orElseThrow();
        assertThat(ticketAfter.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticketAfter.usedAt()).isNotNull();

        // 4. usedAt e usedByGateUserId pertencem somente à validação correta (gateUserId1)
        assertThat(ticketAfter.usedByGateUserId()).isEqualTo(gateUserId1);

        // 5. Duas auditorias coerentes persistidas
        Integer auditEventACount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ? AND result = 'VALID' AND selected_event_id = ? AND ticket_id = ? AND gate_user_id = ?",
                Integer.class,
                attemptIdEventA,
                eventId1,
                ticketEventA.id(),
                gateUserId1
        );
        assertThat(auditEventACount).isEqualTo(1);

        Integer auditEventBCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ? AND result = 'WRONG_EVENT' AND selected_event_id = ? AND ticket_id = ? AND gate_user_id = ?",
                Integer.class,
                attemptIdEventB,
                eventId2,
                ticketEventA.id(),
                gateUserId2
        );
        assertThat(auditEventBCount).isEqualTo(1);

        Integer totalAudits = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE ticket_id = ?",
                Integer.class,
                ticketEventA.id()
        );
        assertThat(totalAudits).isEqualTo(2);
    }

    @Test
    @DisplayName("Replay idempotente completo: mesmo attemptId e mesmo fingerprint retorna resultado persistido original sem nova mutação")
    void idempotentReplayPreservesOriginalResultWithoutNewMutationOrAudit() {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        UUID attemptId = UUID.randomUUID();

        ValidateTicketCommand command = new ValidateTicketCommand(
                attemptId,
                gateUserId1,
                eventId1,
                ValidationMethod.MANUAL,
                ticket.manualCode()
        );

        ValidationAttempt initialAttempt = validateTicketUseCase.execute(command);
        assertThat(initialAttempt.result()).isEqualTo(GateValidationResult.VALID);
        assertThat(initialAttempt.ticketId()).isEqualTo(ticket.id());

        Instant firstProcessedAt = initialAttempt.processedAt();

        // Replay imediato
        ValidationAttempt replayedAttempt = validateTicketUseCase.execute(command);
        assertThat(replayedAttempt.result()).isEqualTo(GateValidationResult.VALID);
        assertThat(replayedAttempt.id()).isEqualTo(attemptId);
        assertThat(replayedAttempt.processedAt()).isEqualTo(firstProcessedAt);

        // Apenas 1 registro na tabela validation_attempts
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ?",
                Integer.class,
                attemptId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Replay com attemptId idêntico mas fingerprint diferente lança 409 GateAttemptConflictException sem alteração de estado")
    void replayWithConflictingFingerprintThrowsConflictWithoutStateChange() {
        Ticket ticket1 = createValidTicket(eventId1, sectorId1);
        Ticket ticket2 = createValidTicket(eventId1, sectorId1);

        UUID attemptId = UUID.randomUUID();

        // Primeira validação com ticket1
        ValidateTicketCommand cmd1 = new ValidateTicketCommand(
                attemptId,
                gateUserId1,
                eventId1,
                ValidationMethod.MANUAL,
                ticket1.manualCode()
        );
        ValidationAttempt initial = validateTicketUseCase.execute(cmd1);
        assertThat(initial.result()).isEqualTo(GateValidationResult.VALID);

        // Segunda tentativa com mesmo attemptId mas código do ticket2
        ValidateTicketCommand cmdConflicting = new ValidateTicketCommand(
                attemptId,
                gateUserId1,
                eventId1,
                ValidationMethod.MANUAL,
                ticket2.manualCode()
        );

        assertThatThrownBy(() -> validateTicketUseCase.execute(cmdConflicting))
                .isInstanceOf(GateAttemptConflictException.class);

        // ticket2 permanece intacto (VALID, não consumido)
        Ticket ticket2After = ticketRepository.findById(ticket2.id()).orElseThrow();
        assertThat(ticket2After.status()).isEqualTo(TicketStatus.VALID);
        assertThat(ticket2After.usedAt()).isNull();

        // validation_attempts permanece com 1 único registro apontando para ticket1
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ?",
                Integer.class,
                attemptId
        );
        assertThat(auditCount).isEqualTo(1);

        UUID associatedTicketId = jdbcTemplate.queryForObject(
                "SELECT ticket_id FROM validation_attempts WHERE id = ?",
                UUID.class,
                attemptId
        );
        assertThat(associatedTicketId).isEqualTo(ticket1.id());
    }

    @Test
    @DisplayName("Replay com attemptId pertencente a outro operador lança GateOwnershipException sem alterar estado")
    void replayWithDifferentGateUserThrowsOwnershipExceptionWithoutStateChange() {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        UUID attemptId = UUID.randomUUID();

        ValidateTicketCommand cmd1 = new ValidateTicketCommand(
                attemptId,
                gateUserId1,
                eventId1,
                ValidationMethod.MANUAL,
                ticket.manualCode()
        );
        validateTicketUseCase.execute(cmd1);

        ValidateTicketCommand cmdDifferentUser = new ValidateTicketCommand(
                attemptId,
                gateUserId2,
                eventId1,
                ValidationMethod.MANUAL,
                ticket.manualCode()
        );

        assertThatThrownBy(() -> validateTicketUseCase.execute(cmdDifferentUser))
                .isInstanceOf(GateOwnershipException.class);
    }

    @Test
    @DisplayName("Redaction estrita de credenciais: manualCode e tokens brutos nunca persistem na tabela validation_attempts")
    void rawCredentialNeverPersistedInDatabase() {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        String rawManualCode = ticket.manualCode();
        UUID attemptId = UUID.randomUUID();

        validateTicketUseCase.execute(new ValidateTicketCommand(
                attemptId,
                gateUserId1,
                eventId1,
                ValidationMethod.MANUAL,
                rawManualCode
        ));

        Map<String, Object> attemptRow = jdbcTemplate.queryForMap(
                "SELECT * FROM validation_attempts WHERE id = ?",
                attemptId
        );

        // Campos auditados
        assertThat(attemptRow.get("id")).isEqualTo(attemptId);
        assertThat(attemptRow.get("gate_user_id")).isEqualTo(gateUserId1);
        assertThat(attemptRow.get("selected_event_id")).isEqualTo(eventId1);
        assertThat(attemptRow.get("ticket_id")).isEqualTo(ticket.id());
        assertThat(attemptRow.get("validation_method")).isEqualTo("MANUAL");
        assertThat(attemptRow.get("result")).isEqualTo("VALID");
        assertThat(attemptRow.get("processed_at")).isNotNull();
        assertThat(attemptRow.get("created_at")).isNotNull();

        // Fingerprint é SHA-256 (64 hex characters)
        String fingerprint = (String) attemptRow.get("fingerprint");
        assertThat(fingerprint).hasSize(64).matches("^[a-f0-9]{64}$");

        // Nenhuma coluna da tabela contém o código manual original ou normalizado
        String normalizedCode = TicketCredentialGenerator.normalizeManualCode(rawManualCode);
        for (Map.Entry<String, Object> entry : attemptRow.entrySet()) {
            if (entry.getValue() instanceof String val) {
                assertThat(val).doesNotContain(rawManualCode);
                if (normalizedCode != null && !normalizedCode.isBlank()) {
                    assertThat(val).doesNotContain(normalizedCode);
                }
            }
        }
    }

    @Test
    @DisplayName("Rollback atômico: rollback de transação não deixa Ticket e auditoria divergentes")
    void transactionRollbackLeavesTicketAndAuditConsistent() {
        Ticket ticket = createValidTicket(eventId1, sectorId1);
        UUID attemptId = UUID.randomUUID();

        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> template.execute(status -> {
            validateTicketUseCase.execute(new ValidateTicketCommand(
                    attemptId,
                    gateUserId1,
                    eventId1,
                    ValidationMethod.MANUAL,
                    ticket.manualCode()
            ));
            // Provoca rollback forçado da transação
            throw new RuntimeException("Simulated failure to trigger transactional rollback");
        })).isInstanceOf(RuntimeException.class).hasMessageContaining("Simulated failure");

        // Verificações pós-rollback no PostgreSQL:
        // 1. Ticket não sofreu mutação (continua VALID e não usado)
        Ticket ticketInDb = ticketRepository.findById(ticket.id()).orElseThrow();
        assertThat(ticketInDb.status()).isEqualTo(TicketStatus.VALID);
        assertThat(ticketInDb.usedAt()).isNull();
        assertThat(ticketInDb.usedByGateUserId()).isNull();

        // 2. Nenhuma tentativa de validação ficou gravada
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM validation_attempts WHERE id = ?",
                Integer.class,
                attemptId
        );
        assertThat(auditCount).isEqualTo(0);
    }
}
