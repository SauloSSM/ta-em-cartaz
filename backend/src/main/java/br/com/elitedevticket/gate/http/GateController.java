package br.com.elitedevticket.gate.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.gate.application.ValidateTicketCommand;
import br.com.elitedevticket.gate.application.ValidateTicketUseCase;
import br.com.elitedevticket.gate.domain.ValidationAttempt;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gate")
public class GateController {

    private final ValidateTicketUseCase validateTicketUseCase;

    public GateController(ValidateTicketUseCase validateTicketUseCase) {
        this.validateTicketUseCase = Objects.requireNonNull(validateTicketUseCase, "validateTicketUseCase must not be null");
    }

    @PostMapping("/validations")
    @PreAuthorize("hasRole('GATE')")
    public ResponseEntity<ValidateTicketResponse> validateTicket(
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody(required = false) ValidateTicketRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Corpo da requisicao de validacao e obrigatorio.");
        }
        request.validate();

        ValidateTicketCommand command = new ValidateTicketCommand(
                request.validationAttemptId(),
                sessionUser.id(),
                request.selectedEventId(),
                request.method(),
                request.manualCode()
        );

        ValidationAttempt attempt = validateTicketUseCase.execute(command);
        return ResponseEntity.ok(ValidateTicketResponse.fromDomain(attempt));
    }
}
