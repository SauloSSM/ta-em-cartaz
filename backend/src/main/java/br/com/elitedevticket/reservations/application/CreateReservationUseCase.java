package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import br.com.elitedevticket.reservations.domain.EventNotPublishedException;
import br.com.elitedevticket.reservations.domain.IdempotencyConflictException;
import br.com.elitedevticket.reservations.domain.InsufficientAvailabilityException;
import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.ReservationIdempotencyRecord;
import br.com.elitedevticket.reservations.domain.SalesClosedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateReservationUseCase {

    private final CustomerLockPort customerLockPort;
    private final EventStockPort eventStockPort;
    private final ReservationRepository reservationRepository;
    private final ReservationIdempotencyRepository reservationIdempotencyRepository;
    private final ExpireReservationUseCase expireReservationUseCase;
    private final Clock clock;

    public CreateReservationUseCase(
            CustomerLockPort customerLockPort,
            EventStockPort eventStockPort,
            ReservationRepository reservationRepository,
            ReservationIdempotencyRepository reservationIdempotencyRepository,
            ExpireReservationUseCase expireReservationUseCase,
            Clock clock
    ) {
        this.customerLockPort = customerLockPort;
        this.eventStockPort = eventStockPort;
        this.reservationRepository = reservationRepository;
        this.reservationIdempotencyRepository = reservationIdempotencyRepository;
        this.expireReservationUseCase = expireReservationUseCase;
        this.clock = clock;
    }

    @Transactional
    public Reservation execute(CreateReservationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.customerId(), "customerId must not be null");
        Objects.requireNonNull(command.eventId(), "eventId must not be null");
        Objects.requireNonNull(command.sectorId(), "sectorId must not be null");

        if (command.quantity() < Reservation.MIN_QUANTITY || command.quantity() > Reservation.MAX_QUANTITY) {
            throw new InvalidReservationQuantityException(
                    "A quantidade de ingressos deve ser entre " + Reservation.MIN_QUANTITY + " e " + Reservation.MAX_QUANTITY
            );
        }

        String normalizedIdempotencyKey = normalizeKey(command.idempotencyKey());
        String payloadHash = calculatePayloadHash(command.eventId(), command.sectorId(), command.quantity());

        // 1. Lock Customer first (AD-4, AD-5)
        customerLockPort.lockCustomer(command.customerId());

        Instant serverNow = clock.instant();

        // 2. Check Idempotency-Key if provided (AD-7)
        if (normalizedIdempotencyKey != null) {
            Optional<ReservationIdempotencyRecord> existingKeyRecord =
                    reservationIdempotencyRepository.findByCustomerIdAndIdempotencyKey(
                            command.customerId(),
                            normalizedIdempotencyKey
                    );

            if (existingKeyRecord.isPresent()) {
                ReservationIdempotencyRecord record = existingKeyRecord.get();
                if (!record.payloadHash().equals(payloadHash)) {
                    throw new IdempotencyConflictException("Chave de idempotência reutilizada com parâmetros incompatíveis.");
                }
                return reservationRepository.findById(record.reservationId())
                        .orElseThrow(() -> new IllegalStateException("Reserva associada à chave de idempotência não encontrada"));
            }
        }

        // 3. Check if Customer already has a hold for the same event (AD-4, AD-6)
        Optional<Reservation> existingHold =
                reservationRepository.findHoldingByCustomerAndEvent(command.customerId(), command.eventId());

        if (existingHold.isPresent()) {
            Reservation hold = existingHold.get();
            if (!hold.isExpired(serverNow)) {
                // Active valid hold exists for this customer/event
                if (normalizedIdempotencyKey != null) {
                    reservationIdempotencyRepository.save(new ReservationIdempotencyRecord(
                            UUID.randomUUID(),
                            command.customerId(),
                            normalizedIdempotencyKey,
                            payloadHash,
                            hold.id(),
                            serverNow
                    ));
                }
                return hold;
            } else {
                // Hold is expired: reconcile/expire it immediately to free stock (AD-6)
                expireReservationUseCase.execute(hold.id());
            }
        }

        // 4. Validate Event published & sales open
        Event event = eventStockPort.findEventById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado: " + command.eventId()));

        if (event.status() != EventStatus.PUBLISHED) {
            throw new EventNotPublishedException("Evento não está publicado: " + command.eventId());
        }

        if (event.startsAt() != null && !serverNow.isBefore(event.startsAt())) {
            throw new SalesClosedException("As vendas para este evento foram encerradas.");
        }

        // 5. Lazy expiry on sector to prevent false scarcity (AD-6)
        List<UUID> expiredOnSector = reservationRepository.findExpiredHoldingIdsBySector(command.sectorId(), serverNow);
        for (UUID expiredId : expiredOnSector) {
            expireReservationUseCase.execute(expiredId);
        }

        // 6. Lock TicketSector with PESSIMISTIC_WRITE (AD-3, AD-5)
        TicketSector sector = eventStockPort.findSectorByIdWithLock(command.sectorId())
                .orElseThrow(() -> new TicketSectorNotFoundException("Setor de ingressos não encontrado: " + command.sectorId()));

        if (!sector.eventId().equals(command.eventId())) {
            throw new TicketSectorNotFoundException("Setor de ingressos não pertence ao evento especificado.");
        }

        if (sector.availableQuantity() < command.quantity()) {
            throw new InsufficientAvailabilityException(
                    "Disponibilidade insuficiente no setor selecionado (" + sector.name() + ")."
            );
        }

        // 7. Decrement availableQuantity atomically
        int newAvailableQuantity = sector.availableQuantity() - command.quantity();
        eventStockPort.updateSectorAvailability(sector.id(), newAvailableQuantity);

        // 8. Create and persist Reservation
        Reservation reservation = Reservation.createHolding(
                UUID.randomUUID(),
                command.customerId(),
                command.eventId(),
                command.sectorId(),
                command.quantity(),
                sector.price(),
                serverNow
        );
        Reservation savedReservation = reservationRepository.save(reservation);

        // 9. Persist Idempotency Record if key was provided (AD-7)
        if (normalizedIdempotencyKey != null) {
            reservationIdempotencyRepository.save(new ReservationIdempotencyRecord(
                    UUID.randomUUID(),
                    command.customerId(),
                    normalizedIdempotencyKey,
                    payloadHash,
                    savedReservation.id(),
                    serverNow
            ));
        }

        return savedReservation;
    }

    private static String normalizeKey(String key) {
        if (key == null) return null;
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String calculatePayloadHash(UUID eventId, UUID sectorId, int quantity) {
        String canonical = "v1:" + eventId + ":" + sectorId + ":" + quantity;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível no ambiente", e);
        }
    }
}
