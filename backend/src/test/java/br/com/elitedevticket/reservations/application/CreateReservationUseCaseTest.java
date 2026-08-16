package br.com.elitedevticket.reservations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import br.com.elitedevticket.reservations.domain.EventNotPublishedException;
import br.com.elitedevticket.reservations.domain.IdempotencyConflictException;
import br.com.elitedevticket.reservations.domain.InsufficientAvailabilityException;
import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationIdempotencyRecord;
import br.com.elitedevticket.reservations.domain.ReservationStatus;
import br.com.elitedevticket.reservations.domain.SalesClosedException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CreateReservationUseCaseTest {

    private CustomerLockPort customerLockPort;
    private EventStockPort eventStockPort;
    private ReservationRepository reservationRepository;
    private ReservationIdempotencyRepository reservationIdempotencyRepository;
    private Clock clock;
    private CreateReservationUseCase useCase;

    private final Instant now = Instant.parse("2026-08-16T15:00:00Z");

    @BeforeEach
    void setUp() {
        customerLockPort = mock(CustomerLockPort.class);
        eventStockPort = mock(EventStockPort.class);
        reservationRepository = mock(ReservationRepository.class);
        reservationIdempotencyRepository = mock(ReservationIdempotencyRepository.class);
        clock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new CreateReservationUseCase(
                customerLockPort,
                eventStockPort,
                reservationRepository,
                reservationIdempotencyRepository,
                clock
        );
    }

    @Test
    @DisplayName("Cria reservation HOLDING respeitando ordem de locks Customer -> Sector e decrementando estoque")
    void shouldCreateReservationHoldingAndDecrementStockWithProperLockOrder() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                organizerId,
                null,
                null,
                "Show do Artista",
                "Descrição do show",
                null,
                "Música",
                EventStatus.PUBLISHED,
                "Allianz Parque",
                "Av. Francisco Matarazzo, 1705",
                now.plus(5, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS)
        );

        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista Premium",
                "Em frente ao palco",
                100,
                20,
                new BigDecimal("250.00"),
                now.minus(1, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS)
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 3);
        Reservation result = useCase.execute(command);

        // Verify lock order Customer -> Sector
        InOrder inOrder = inOrder(customerLockPort, eventStockPort, reservationRepository);
        inOrder.verify(customerLockPort).lockCustomer(customerId);
        inOrder.verify(eventStockPort).findSectorByIdWithLock(sectorId);
        inOrder.verify(eventStockPort).updateSectorAvailability(sectorId, 17);
        inOrder.verify(reservationRepository).save(any(Reservation.class));

        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.sectorId()).isEqualTo(sectorId);
        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.unitPrice()).isEqualTo(new BigDecimal("250.00"));
        assertThat(result.totalAmount()).isEqualTo(new BigDecimal("750.00"));
        assertThat(result.status()).isEqualTo(ReservationStatus.HOLDING);
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.expiresAt()).isEqualTo(now.plus(10, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("Rejeita criação de reservation para evento em status DRAFT")
    void shouldRejectDraftEvent() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                UUID.randomUUID(),
                null,
                null,
                "Rascunho",
                "Descrição",
                null,
                "Teatro",
                EventStatus.DRAFT,
                "Teatro Municipal",
                "Praça Ramos de Azevedo",
                now.plus(5, ChronoUnit.DAYS),
                now,
                now
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 2);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(EventNotPublishedException.class)
                .hasMessageContaining("não está publicado");

        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita criação de reservation quando as vendas estão encerradas (startsAt <= serverNow)")
    void shouldRejectWhenSalesClosed() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                UUID.randomUUID(),
                null,
                null,
                "Evento Passado",
                "Descrição",
                null,
                "Música",
                EventStatus.PUBLISHED,
                "Local",
                "Endereço",
                now.minus(1, ChronoUnit.HOURS),
                now.minus(2, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS)
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 2);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(SalesClosedException.class)
                .hasMessageContaining("vendas para este evento foram encerradas");
    }

    @Test
    @DisplayName("Rejeita criação de reservation quando o estoque é insuficiente")
    void shouldRejectWhenInsufficientAvailability() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                UUID.randomUUID(),
                null,
                null,
                "Evento",
                "Descrição",
                null,
                "Música",
                EventStatus.PUBLISHED,
                "Local",
                "Endereço",
                now.plus(5, ChronoUnit.DAYS),
                now,
                now
        );

        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Camarote",
                "VIP",
                50,
                2, // only 2 available
                new BigDecimal("300.00"),
                now,
                now
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 3);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InsufficientAvailabilityException.class)
                .hasMessageContaining("Disponibilidade insuficiente");

        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita criação de reservation quando o setor pertence a outro evento")
    void shouldRejectWhenSectorBelongsToDifferentEvent() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID otherEventId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                UUID.randomUUID(),
                null,
                null,
                "Evento A",
                "Descrição",
                null,
                "Música",
                EventStatus.PUBLISHED,
                "Local",
                "Endereço",
                now.plus(5, ChronoUnit.DAYS),
                now,
                now
        );

        TicketSector sector = new TicketSector(
                sectorId,
                otherEventId, // different event!
                "Setor B",
                "Descrição",
                50,
                10,
                new BigDecimal("100.00"),
                now,
                now
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 2);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(TicketSectorNotFoundException.class);
    }

    @Test
    @DisplayName("Rejeita quantidade inválida (menor que 1 ou maior que 6)")
    void shouldRejectInvalidQuantityInCommand() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(new CreateReservationCommand(customerId, eventId, sectorId, 0)))
                .isInstanceOf(InvalidReservationQuantityException.class);

        assertThatThrownBy(() -> useCase.execute(new CreateReservationCommand(customerId, eventId, sectorId, 7)))
                .isInstanceOf(InvalidReservationQuantityException.class);
    }

    @Test
    @DisplayName("Calcula snapshot exato em BRL para unitPrice e totalAmount = unitPrice * quantity")
    void shouldCalculateExactBrlSnapshotPriceForVariousQuantities() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();

        Event event = new Event(
                eventId,
                UUID.randomUUID(),
                null,
                null,
                "Festival de Jazz",
                "Descrição",
                null,
                "Música",
                EventStatus.PUBLISHED,
                "Local",
                "Endereço",
                now.plus(10, ChronoUnit.DAYS),
                now,
                now
        );

        TicketSector sector = new TicketSector(
                sectorId,
                eventId,
                "Pista",
                "Setor Pista",
                100,
                50,
                new BigDecimal("123.45"),
                now,
                now
        );

        when(eventStockPort.findEventById(eventId)).thenReturn(Optional.of(event));
        when(eventStockPort.findSectorByIdWithLock(sectorId)).thenReturn(Optional.of(sector));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = useCase.execute(new CreateReservationCommand(customerId, eventId, sectorId, 5));

        assertThat(reservation.unitPrice()).isEqualTo(new BigDecimal("123.45"));
        assertThat(reservation.totalAmount()).isEqualTo(new BigDecimal("617.25"));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.HOLDING);
        assertThat(reservation.expiresAt()).isEqualTo(now.plus(10, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("Mesma Idempotency-Key com mesmo payload retorna a Reservation existente sem nova baixa")
    void shouldReturnSameReservationForSameIdempotencyKeyAndSamePayload() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID existingReservationId = UUID.randomUUID();
        String idempotencyKey = "key-12345";
        String payloadHash = hashOf("v1:" + eventId + ":" + sectorId + ":2");

        Reservation existingReservation = Reservation.createHolding(
                existingReservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("100.00"),
                now
        );

        ReservationIdempotencyRecord idempotencyRecord = new ReservationIdempotencyRecord(
                UUID.randomUUID(),
                customerId,
                idempotencyKey,
                payloadHash,
                existingReservationId,
                now
        );

        when(reservationIdempotencyRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey))
                .thenReturn(Optional.of(idempotencyRecord));
        when(reservationRepository.findById(existingReservationId))
                .thenReturn(Optional.of(existingReservation));

        CreateReservationCommand command = new CreateReservationCommand(
                customerId,
                eventId,
                sectorId,
                2,
                idempotencyKey
        );
        Reservation result = useCase.execute(command);

        assertThat(result).isSameAs(existingReservation);
        verify(eventStockPort, never()).findSectorByIdWithLock(any());
        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mesma Idempotency-Key com payload diferente lança IdempotencyConflictException")
    void shouldThrowIdempotencyConflictWhenPayloadDiffers() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID existingReservationId = UUID.randomUUID();
        String idempotencyKey = "key-12345";
        String differentPayloadHash = hashOf("v1:" + eventId + ":" + sectorId + ":4");

        ReservationIdempotencyRecord idempotencyRecord = new ReservationIdempotencyRecord(
                UUID.randomUUID(),
                customerId,
                idempotencyKey,
                differentPayloadHash,
                existingReservationId,
                now
        );

        when(reservationIdempotencyRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey))
                .thenReturn(Optional.of(idempotencyRecord));

        // Attempt with quantity = 2 (payload differs from 4)
        CreateReservationCommand command = new CreateReservationCommand(
                customerId,
                eventId,
                sectorId,
                2,
                idempotencyKey
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Chave de idempotência reutilizada");

        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Customer com HOLDING vigente no mesmo evento recupera o hold existente sem nova baixa")
    void shouldReturnExistingActiveHoldForSameCustomerAndEvent() {
        UUID customerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID existingReservationId = UUID.randomUUID();

        Reservation existingActiveHold = Reservation.createHolding(
                existingReservationId,
                customerId,
                eventId,
                sectorId,
                2,
                new BigDecimal("150.00"),
                now
        );

        when(reservationRepository.findHoldingByCustomerAndEvent(customerId, eventId))
                .thenReturn(Optional.of(existingActiveHold));

        CreateReservationCommand command = new CreateReservationCommand(customerId, eventId, sectorId, 2);
        Reservation result = useCase.execute(command);

        assertThat(result).isSameAs(existingActiveHold);
        verify(eventStockPort, never()).findSectorByIdWithLock(any());
        verify(eventStockPort, never()).updateSectorAvailability(any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
    }

    private static String hashOf(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
