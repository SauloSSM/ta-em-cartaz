package br.com.elitedevticket.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.elitedevticket.payments.application.ProcessPaymentAttemptCommand;
import br.com.elitedevticket.payments.application.ProcessPaymentAttemptUseCase;
import br.com.elitedevticket.payments.domain.PaymentSimulatedOutcome;
import br.com.elitedevticket.reservations.application.CreateReservationCommand;
import br.com.elitedevticket.reservations.application.CreateReservationUseCase;
import br.com.elitedevticket.reservations.application.ExpireReservationUseCase;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationExpiredException;
import java.time.Instant;
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
class PaymentApprovalConcurrencyAndRollbackIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ProcessPaymentAttemptUseCase processPaymentAttemptUseCase;

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @Autowired
    private ExpireReservationUseCase expireReservationUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UUID createTestEventAndSector(int capacity) {
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO events (id, organizer_id, external_source, external_id, title, category, description, venue_name, venue_address, starts_at, status, created_at, updated_at)
                VALUES (?, ?, 'TICKETMASTER', ?, 'Show Concorrencia Real', 'Musica', 'Desc', 'Allianz', 'Av', NOW() + INTERVAL '10 days', 'PUBLISHED', NOW(), NOW())
                """,
                eventId,
                ORGANIZER_ID,
                "tm-" + UUID.randomUUID().toString().substring(0, 8)
        );

        jdbcTemplate.update(
                """
                INSERT INTO ticket_sectors (id, event_id, name, description, capacity, available_quantity, price, created_at, updated_at)
                VALUES (?, ?, 'Pista', 'Desc', ?, ?, 100.00, NOW(), NOW())
                """,
                sectorId,
                eventId,
                capacity,
                capacity
        );

        return sectorId;
    }

    @Test
    @DisplayName("Corrida payment approval x expiry: resultado é estritamente consistente (ou CONFIRMED com ingressos ou EXPIRED sem ingressos)")
    void raceBetweenPaymentApprovalAndExpiryIsStrictlyConsistent() throws Exception {
        UUID sectorId = createTestEventAndSector(50);
        UUID eventId = jdbcTemplate.queryForObject("SELECT event_id FROM ticket_sectors WHERE id = ?", UUID.class, sectorId);

        Reservation res = createReservationUseCase.execute(new CreateReservationCommand(
                CUSTOMER_ID,
                eventId,
                sectorId,
                2,
                "idemp-" + UUID.randomUUID()
        ));

        UUID paymentAttemptId = UUID.randomUUID();

        // Disparar concorrentemente aprovação e expiração
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<String> paymentTask = () -> {
            try {
                processPaymentAttemptUseCase.execute(new ProcessPaymentAttemptCommand(
                        CUSTOMER_ID,
                        res.id(),
                        paymentAttemptId,
                        PaymentSimulatedOutcome.APPROVED
                ));
                return "PAYMENT_OK";
            } catch (ReservationExpiredException e) {
                return "PAYMENT_EXPIRED";
            } catch (Exception e) {
                return "PAYMENT_ERR:" + e.getClass().getSimpleName();
            }
        };

        Callable<String> expiryTask = () -> {
            try {
                expireReservationUseCase.execute(res.id());
                return "EXPIRY_OK";
            } catch (Exception e) {
                return "EXPIRY_ERR:" + e.getClass().getSimpleName();
            }
        };

        List<Future<String>> futures = executor.invokeAll(List.of(paymentTask, expiryTask));
        executor.shutdown();

        String paymentRes = futures.get(0).get();
        String expiryRes = futures.get(1).get();

        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                res.id()
        );
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ?",
                Integer.class,
                res.id()
        );

        if ("CONFIRMED".equals(finalStatus)) {
            assertThat(ticketCount).isEqualTo(2);
            Integer paymentCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM payments WHERE id = ? AND status = 'APPROVED'",
                    Integer.class,
                    paymentAttemptId
            );
            assertThat(paymentCount).isEqualTo(1);
        } else {
            assertThat(finalStatus).isEqualTo("EXPIRED");
            assertThat(ticketCount).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Rollback atômico: exceção durante persistência dos Tickets reverte transação inteira no PostgreSQL (zero tickets parciais, reservation HOLDING, confirmed_at nulo, zero payments)")
    void transactionRollbackWhenExceptionOccursDuringTicketIssuanceRevertsAllChangesAtomically() {
        UUID sectorId = createTestEventAndSector(50);
        UUID eventId = jdbcTemplate.queryForObject("SELECT event_id FROM ticket_sectors WHERE id = ?", UUID.class, sectorId);

        // 1. Criar hold de 2 ingressos (estoque diminui de 50 para 48)
        Reservation res = createReservationUseCase.execute(new CreateReservationCommand(
                CUSTOMER_ID,
                eventId,
                sectorId,
                2,
                "idemp-" + UUID.randomUUID()
        ));

        Integer stockAfterHold = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                sectorId
        );
        assertThat(stockAfterHold).isEqualTo(48);

        // 2. Inserir previamente um ticket com ordinal 2 para provocar colisão de constraint no PostgreSQL durante a emissão do 2º ticket
        UUID preExistingTicketId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tickets (id, reservation_id, event_id, sector_id, customer_id, ordinal, status, validation_token, manual_code, share_token, created_at)
                VALUES (?, ?, ?, ?, ?, 2, 'VALID', 'pre-val-tok', 'PREMANCD22', 'pre-shr-tok', NOW())
                """,
                preExistingTicketId,
                res.id(),
                eventId,
                sectorId,
                CUSTOMER_ID
        );

        UUID paymentAttemptId = UUID.randomUUID();

        // 3. Executar tentativa APPROVED: o gateway aprova, confirma a reserva na sessão, tenta emitir ordinal 1 e 2,
        // mas falha no ordinal 2 por colisão de constraint no PostgreSQL
        assertThatThrownBy(() -> processPaymentAttemptUseCase.execute(new ProcessPaymentAttemptCommand(
                CUSTOMER_ID,
                res.id(),
                paymentAttemptId,
                PaymentSimulatedOutcome.APPROVED
        ))).isInstanceOf(DataIntegrityViolationException.class);

        // 4. Verificações rigorosas pós-rollback no PostgreSQL:

        // a) Nenhum Payment foi persistido como APPROVED
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE id = ?",
                Integer.class,
                paymentAttemptId
        );
        assertThat(paymentCount).isEqualTo(0);

        // b) A Reservation permanece HOLDING
        String reservationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                res.id()
        );
        assertThat(reservationStatus).isEqualTo("HOLDING");

        // c) confirmed_at permanece nulo
        Instant confirmedAt = jdbcTemplate.queryForObject(
                "SELECT confirmed_at FROM reservations WHERE id = ?",
                Instant.class,
                res.id()
        );
        assertThat(confirmedAt).isNull();

        // d) Nenhum ticket parcial (ordinal 1) permaneceu persistido pela tentativa que falhou
        Integer ticket1Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tickets WHERE reservation_id = ? AND ordinal = 1",
                Integer.class,
                res.id()
        );
        assertThat(ticket1Count).isEqualTo(0);

        // e) O estoque do setor não sofreu qualquer alteração adicional (continua 48)
        Integer stockAfterRollback = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM ticket_sectors WHERE id = ?",
                Integer.class,
                sectorId
        );
        assertThat(stockAfterRollback).isEqualTo(48);
    }
}
