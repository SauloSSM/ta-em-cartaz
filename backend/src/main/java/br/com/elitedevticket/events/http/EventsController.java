package br.com.elitedevticket.events.http;

import br.com.elitedevticket.auth.domain.SessionUser;
import br.com.elitedevticket.events.application.CreateDraftEventUseCase;
import br.com.elitedevticket.events.application.CreateTicketSectorUseCase;
import br.com.elitedevticket.events.application.DeleteDraftEventUseCase;
import br.com.elitedevticket.events.application.DeleteTicketSectorUseCase;
import br.com.elitedevticket.events.application.GetEventUseCase;
import br.com.elitedevticket.events.application.ListMyEventsUseCase;
import br.com.elitedevticket.events.application.ListTicketSectorsUseCase;
import br.com.elitedevticket.events.application.UpdateDraftEventUseCase;
import br.com.elitedevticket.events.application.UpdateTicketSectorUseCase;
import br.com.elitedevticket.events.domain.Event;
import br.com.elitedevticket.events.domain.TicketSector;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final CreateDraftEventUseCase createDraftEventUseCase;
    private final GetEventUseCase getEventUseCase;
    private final ListMyEventsUseCase listMyEventsUseCase;
    private final UpdateDraftEventUseCase updateDraftEventUseCase;
    private final DeleteDraftEventUseCase deleteDraftEventUseCase;
    private final CreateTicketSectorUseCase createTicketSectorUseCase;
    private final UpdateTicketSectorUseCase updateTicketSectorUseCase;
    private final DeleteTicketSectorUseCase deleteTicketSectorUseCase;
    private final ListTicketSectorsUseCase listTicketSectorsUseCase;

    public EventsController(
            CreateDraftEventUseCase createDraftEventUseCase,
            GetEventUseCase getEventUseCase,
            ListMyEventsUseCase listMyEventsUseCase,
            UpdateDraftEventUseCase updateDraftEventUseCase,
            DeleteDraftEventUseCase deleteDraftEventUseCase,
            CreateTicketSectorUseCase createTicketSectorUseCase,
            UpdateTicketSectorUseCase updateTicketSectorUseCase,
            DeleteTicketSectorUseCase deleteTicketSectorUseCase,
            ListTicketSectorsUseCase listTicketSectorsUseCase
    ) {
        this.createDraftEventUseCase = createDraftEventUseCase;
        this.getEventUseCase = getEventUseCase;
        this.listMyEventsUseCase = listMyEventsUseCase;
        this.updateDraftEventUseCase = updateDraftEventUseCase;
        this.deleteDraftEventUseCase = deleteDraftEventUseCase;
        this.createTicketSectorUseCase = createTicketSectorUseCase;
        this.updateTicketSectorUseCase = updateTicketSectorUseCase;
        this.deleteTicketSectorUseCase = deleteTicketSectorUseCase;
        this.listTicketSectorsUseCase = listTicketSectorsUseCase;
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

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventListResponse> listMyEvents(
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        List<Event> events = listMyEventsUseCase.execute(sessionUser.id());
        List<EventResponse> responseList = events.stream()
                .map(EventResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(new EventListResponse(responseList));
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> updateDraftEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody UpdateDraftEventRequest request
    ) {
        request.validate();
        Event updated = updateDraftEventUseCase.execute(
                id,
                sessionUser.id(),
                request.title(),
                request.description(),
                request.imageUrl(),
                request.category(),
                request.venue(),
                request.startsAt()
        );
        return ResponseEntity.ok(EventResponse.fromDomain(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteDraftEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        deleteDraftEventUseCase.execute(id, sessionUser.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/sectors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketSectorListResponse> listTicketSectors(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        UUID userId = sessionUser != null ? sessionUser.id() : null;
        boolean isOrganizer = sessionUser != null && sessionUser.role().name().equals("ORGANIZER");

        List<TicketSector> sectors = listTicketSectorsUseCase.execute(eventId, userId, isOrganizer);
        List<TicketSectorResponse> responseList = sectors.stream()
                .map(TicketSectorResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(new TicketSectorListResponse(responseList));
    }

    @PostMapping("/{eventId}/sectors")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketSectorResponse> createTicketSector(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody CreateTicketSectorRequest request
    ) {
        request.validate();
        TicketSector sector = createTicketSectorUseCase.execute(
                eventId,
                sessionUser.id(),
                request.name(),
                request.description(),
                request.capacity(),
                request.price()
        );

        TicketSectorResponse response = TicketSectorResponse.fromDomain(sector);
        URI location = URI.create("/api/v1/events/" + eventId + "/sectors/" + sector.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{eventId}/sectors/{sectorId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketSectorResponse> updateTicketSector(
            @PathVariable UUID eventId,
            @PathVariable UUID sectorId,
            @AuthenticationPrincipal SessionUser sessionUser,
            @RequestBody UpdateTicketSectorRequest request
    ) {
        request.validate();
        TicketSector updated = updateTicketSectorUseCase.execute(
                eventId,
                sectorId,
                sessionUser.id(),
                request.name(),
                request.description(),
                request.capacity(),
                request.price()
        );
        return ResponseEntity.ok(TicketSectorResponse.fromDomain(updated));
    }

    @DeleteMapping("/{eventId}/sectors/{sectorId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteTicketSector(
            @PathVariable UUID eventId,
            @PathVariable UUID sectorId,
            @AuthenticationPrincipal SessionUser sessionUser
    ) {
        deleteTicketSectorUseCase.execute(eventId, sectorId, sessionUser.id());
        return ResponseEntity.noContent().build();
    }
}
