package br.com.elitedevticket.reservations.application;

import br.com.elitedevticket.auth.application.CustomerLockPort;
import br.com.elitedevticket.events.application.EventStockPort;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.EventNotFoundException;
import br.com.elitedevticket.events.domain.EventStatus;
import br.com.elitedevticket.events.domain.TicketSector;
import br.com.elitedevticket.events.domain.TicketSectorNotFoundException;
import br.com.elitedevticket.reservations.domain.EventNotPublishedException;
import br.com.elitedevticket.reservations.domain.InsufficientAvailabilityException;
import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;
import br.com.elitedevticket.reservations.domain.Reservation;
import br.com.elitedevticket.reservations.domain.SalesClosedException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateReservationUseCase {

    private final CustomerLockPort customerLockPort;
    private final EventStockPort eventStockPort;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public CreateReservationUseCase(
            CustomerLockPort customerLockPort,
            EventStockPort eventStockPort,
            ReservationRepository reservationRepository,
            Clock clock
    ) {
        this.customerLockPort = customerLockPort;
        this.eventStockPort = eventStockPort;
        this.reservationRepository = reservationRepository;
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

        // Lock Customer first (AD-4, AD-5)
        customerLockPort.lockCustomer(command.customerId());

        Instant serverNow = clock.instant();

        // Validate Event published & sales open
        Event event = eventStockPort.findEventById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado: " + command.eventId()));

        if (event.status() != EventStatus.PUBLISHED) {
            throw new EventNotPublishedException("Evento não está publicado: " + command.eventId());
        }

        if (event.startsAt() != null && !serverNow.isBefore(event.startsAt())) {
            throw new SalesClosedException("As vendas para este evento foram encerradas.");
        }

        // Lock TicketSector with PESSIMISTIC_WRITE (AD-3, AD-5)
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

        // Decrement availableQuantity atomically
        int newAvailableQuantity = sector.availableQuantity() - command.quantity();
        eventStockPort.updateSectorAvailability(sector.id(), newAvailableQuantity);

        // Create and persist Reservation
        Reservation reservation = Reservation.createHolding(
                UUID.randomUUID(),
                command.customerId(),
                command.eventId(),
                command.sectorId(),
                command.quantity(),
                sector.price(),
                serverNow
        );

        return reservationRepository.save(reservation);
    }
}
