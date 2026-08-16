package br.com.elitedevticket.reservations.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.reservations.application.CreateReservationCommand;
import br.com.elitedevticket.reservations.application.CreateReservationUseCase;
import br.com.elitedevticket.reservations.domain.InvalidReservationQuantityException;
import br.com.elitedevticket.reservations.domain.Reservation;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}/sectors/{sectorId}/reservations")
public class ReservationsController {

    private final CreateReservationUseCase createReservationUseCase;
    private final Clock clock;

    public ReservationsController(CreateReservationUseCase createReservationUseCase, Clock clock) {
        this.createReservationUseCase = createReservationUseCase;
        this.clock = clock;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> createReservation(
            @PathVariable UUID eventId,
            @PathVariable UUID sectorId,
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) CreateReservationRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("O cabeçalho Idempotency-Key é obrigatório.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Corpo da requisição é obrigatório.");
        }
        request.validate();
        CreateReservationCommand command = new CreateReservationCommand(
                sessionUser.id(),
                eventId,
                sectorId,
                request.quantity(),
                idempotencyKey
        );
        Reservation reservation = createReservationUseCase.execute(command);
        ReservationResponse response = ReservationResponse.fromDomain(reservation, clock.instant());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
