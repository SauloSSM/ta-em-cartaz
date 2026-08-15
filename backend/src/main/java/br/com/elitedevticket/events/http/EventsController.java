package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.events.application.CreateDraftEventUseCase;
import br.com.elitedevticket.events.application.GetEventUseCase;
import br.com.elitedevticket.events.domain.Event;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final CreateDraftEventUseCase createDraftEventUseCase;
    private final GetEventUseCase getEventUseCase;

    public EventsController(CreateDraftEventUseCase createDraftEventUseCase, GetEventUseCase getEventUseCase) {
        this.createDraftEventUseCase = createDraftEventUseCase;
        this.getEventUseCase = getEventUseCase;
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> createDraftEvent(
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody CreateDraftEventRequest request
    ) {
        request.validate();
        Event event = createDraftEventUseCase.createDraft(
                sessionUser.id(),
                request.title(),
                request.externalId(),
                request.description(),
                request.imageUrl(),
                request.category()
        );

        EventResponse response = EventResponse.fromDomain(event);
        URI location = URI.create("/api/v1/events/" + event.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        UUID userId = sessionUser != null ? sessionUser.id() : null;
        boolean isOrganizer = sessionUser != null && sessionUser.role().name().equals("ORGANIZER");

        Event event = getEventUseCase.getEvent(id, userId, isOrganizer);
        return ResponseEntity.ok(EventResponse.fromDomain(event));
    }
}
